package infrastructure
package entities
package person
package projections

import collection.mutable

import akka.projection.eventsourced.EventEnvelope
import akka.projection.r2dbc.scaladsl.R2dbcHandler
import akka.projection.r2dbc.scaladsl.R2dbcSession
import io.r2dbc.spi.Row
import scala.collection.immutable

import akka.Done
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import scala.util.{ Failure, Success, Try }
import java.util.UUID
import entities.PersonEntity

import person.DataModel.*
import infrastructure.entities.person.Events.PersonCreated
import infrastructure.entities.person.Events.PersonUpdated

class PersonProjectionHandler()(using ec: ExecutionContext)
    extends R2dbcHandler[immutable.Seq[EventEnvelope[entities.person.Events.Event]]]:
    private val logger = LoggerFactory.getLogger(getClass)

    override def process(
                      session: R2dbcSession,
                      envelopes: immutable.Seq[EventEnvelope[entities.person.Events.Event]]): Future[Done] =
        given R2dbcSession = session

        val groups = envelopes.groupBy(_.persistenceId)
        val groupsIds =
          groups.keys.map(
            e => UUID.fromString(e.substring(PersonEntity.typeKey.name.length() + 1))
          ).toList
        val groupsIdsSegment = groupsIds.indices.map(_ + 1).map(
          "$" + _
        ).mkString("(", ", ", ")")

        val stmtRange = session
          .createStatement("SELECT id, name, town FROM person_projection WHERE id IN " + groupsIdsSegment)
        val stmtForAllExistingPersons =
          groupsIds
            .zipWithIndex
            .foldLeft(stmtRange):
                case (stmt, (id, i)) => stmt.bind(i, id)

        val existingPersonsTuples: Future[IndexedSeq[(UUID, String, Option[String])]] =
          session
            .select(stmtForAllExistingPersons)(
              (row: Row) =>
                (row.get("id", classOf[UUID]),
                 row.get("name", classOf[String]),
                 Option(row.get("town", classOf[String])),
                )
            )

        val existingPersonsAsMap = existingPersonsTuples.map(_.map {
          case (id: UUID, name: String, town: Option[String]) => (id, PersonEntity.State(name, town))
        }
          .groupBy(_._1)).map:
            case hmap =>
              mutable.Map(hmap.map {
                case (k, v) => (k, v(0)._2)
              }.toSeq: _*)

        val newEntities = mutable.Map.empty[UUID, PersonEntity.State]
        val batch = existingPersonsAsMap.map:
            case existingEntities =>
              def getEntity(id: UUID): PersonEntity.State = newEntities.getOrElse(
                id,
                existingEntities(id)
              )

              def updateEntity(id: UUID, state: PersonEntity.State) =
                if newEntities.contains(id) then
                    newEntities.update(id, state)
                else
                    existingEntities.update(id, state)

              val finalState = groups.map:
                  case (key, value) =>
                    val uuidKey = UUID.fromString(key.substring(PersonEntity.typeKey.name.length() + 1))
                    val (head :: _, rest) = value.toList.splitAt(1): @unchecked
                    head.event match
                      case PersonCreated(name, town) =>
                        val p = PersonEntity.State(name, town)
                        newEntities += (uuidKey -> p)

                      case ev @ PersonUpdated(name, town) => updateEntity(uuidKey, getEntity(uuidKey).applyEvent(ev))

                    val restEvents = rest.map(_.event)

                    (uuidKey,
                     restEvents.foldLeft(getEntity(uuidKey))(
                       (acc, ev) => acc.applyEvent(ev)
                     )
                    )
              finalState.map:
                  case (uuidKey, state) =>
                    logger.info(s"Updating projection for $uuidKey")

                    if newEntities.contains(uuidKey) then
                        session
                          .createStatement("""
                                        INSERT INTO person_projection(id, name, town) 
                                        VALUES                       ($1, $2,   $3  )
                                    """)
                          .bind(0, uuidKey)
                          .bind(1, state.name)
                          .bind(2, state.town.orNull)
                    else
                        session
                          .createStatement("""
                                          UPDATE person_projection
                                          SET name = $1, town = $2 
                                          WHERE id = $3
                                      """)
                          .bind(0, state.name)
                          .bind(1, state.town.orNull)
                          .bind(2, uuidKey)

        batch.map {
          case stmts => session.update(stmts.toVector)
        }.map(
          _ => Done
        )

    end process
