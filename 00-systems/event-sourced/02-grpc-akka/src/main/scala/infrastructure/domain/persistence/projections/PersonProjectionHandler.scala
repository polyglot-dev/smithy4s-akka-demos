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
import org.slf4j.{ Logger, LoggerFactory }

import person.DataModel.*
import infrastructure.entities.person.Events.{ Event as _, * }
import io.r2dbc.postgresql.codec.Json

import io.circe.*, io.circe.syntax.*, io.circe.parser.*, io.circe.generic.auto.*

import akka.persistence.typed.EventAdapter
import _root_.journal.infrastructure.entities.person.events as DataModel
import infrastructure.entities.person.Events as DomainEvents

class PersonProjectionHandler(
                         )(using adapter: EventAdapter[DomainEvents.Event, DataModel.Event], ec: ExecutionContext)
    extends R2dbcHandler[immutable.Seq[EventEnvelope[DataModel.Event]]]:
    given logger: Logger = LoggerFactory.getLogger(getClass)

    override def process(
                        session: R2dbcSession,
                        envelopes: immutable.Seq[EventEnvelope[DataModel.Event]]): Future[Done] =
        // given R2dbcSession = session

        val groups = envelopes.groupBy(_.persistenceId)
        val groupsIds =
          groups.keys.map(
            e => e.substring(PersonEntity.typeKey.name.length() + 1)
          ).toList
        val groupsIdsSegment = groupsIds.indices.map(_ + 1).map(
          "$" + _
        ).mkString("(", ", ", ")")

        val stmtRange = session
          .createStatement("SELECT id, name, town, address FROM person_projection WHERE id IN " + groupsIdsSegment)
        val stmtForAllExistingPersons =
          groupsIds
            .zipWithIndex
            .foldLeft(stmtRange):
                case (stmt, (id, i)) => stmt.bind(i, id)

        val existingPersonsTuples: Future[IndexedSeq[(String, String, Option[String], Option[Json])]] =
          session
            .select(stmtForAllExistingPersons)(
              (row: Row) =>
                (row.get("id", classOf[String]),
                 row.get("name", classOf[String]),
                 Option(row.get("town", classOf[String])),
                 Option(row.get("address", classOf[Json])),
                )
            )

        val existingPersonsAsMap = existingPersonsTuples.map(_.map {
          case (id: String, name: String, town: Option[String], address: Option[Json]) =>
            val addressValue = address.map(
              v =>
                decode[Address](v.asString).toOption
            )
            (id, PersonEntity.State(name, town, addressValue.flatten))
        }
          .groupBy(_._1)).map:
            case hmap =>
              mutable.Map(hmap.map {
                case (k, v) => (k, v(0)._2)
              }.toSeq: _*)

        val newEntities = mutable.Map.empty[String, PersonEntity.State]
        val batch = existingPersonsAsMap.map:
            case existingEntities =>
              def getEntity(id: String): PersonEntity.State = newEntities.getOrElse(
                id,
                existingEntities(id)
              )

              def updateEntity(id: String, state: PersonEntity.State) =
                if newEntities.contains(id) then
                    newEntities.update(id, state)
                else
                    existingEntities.update(id, state)

              val finalState = groups.map:
                  case (key, value) =>
                    val uuidKey = key.substring(PersonEntity.typeKey.name.length() + 1)
                    val vvalue =
                      value.map(
                        e => adapter.fromJournal(e.event, "").events
                      ).flatten
                    val (head :: _, rest) = vvalue.toList.splitAt(1): @unchecked
                    head match
                      case PersonCreated(name, town, address) =>
                        val p = PersonEntity.State(name, town, address)
                        newEntities += (uuidKey -> p)

                      case ev @ PersonUpdated(town, address) => updateEntity(uuidKey, getEntity(uuidKey).applyEvent(ev))

                      case ev @ PersonFixed(name, town, address) =>
                        updateEntity(uuidKey, getEntity(uuidKey).applyEvent(ev))

                    (uuidKey,
                     rest.foldLeft(getEntity(uuidKey))(
                       (acc, ev) => acc.applyEvent(ev)
                     )
                    )
              finalState.map:
                  case (uuidKey, state) =>
                    logger.info(s"Updating projection for $uuidKey")

                    val addr = state.address.map(
                      v => Json.of(v.asJson.noSpaces)
                    )

                    if newEntities.contains(uuidKey) then
                        val optionalFields = List[(String, Option[Any])](("town", state.town), ("address", addr))
                        val fields = optionalFields.filter(_._2.isDefined)
                        val fieldsNames = fields.map(_._1).mkString(start = if fields.isEmpty then "" else ", ",
                                                                    sep = ", ",
                                                                    end = ""
                                                                   )
                        val fieldsIndexForBindings = fields.indices.map(_ + 3).map(
                          "$" + _
                        ).mkString(start = if fields.isEmpty then "" else ", ", sep = ", ", end = "")

                        val stmtRange = session
                          .createStatement(
                            "INSERT INTO person_projection(id, name " + fieldsNames + ")" + " VALUES ($1, $2 " + fieldsIndexForBindings + ")"
                          )

                        (List[Option[Any]](Some(uuidKey), Some(state.name)) ++ fields.map(_._2))
                          .zipWithIndex
                          .foldLeft(stmtRange):
                              case (stmt, (data, i)) => stmt.bind(i, data.get)
                    else

                        val optionalFields = List[(String, Option[Any])](("name", Some(state.name)),
                                                                         ("town", state.town),
                                                                         ("address", addr)
                                                                        )
                        val fields = optionalFields.filter(_._2.isDefined)
                        // val fieldsNames = fields.map(_._1)
                        val expr = fields
                          .map(_._1)
                          .zipWithIndex
                          .map:
                              case (name, index) => s"$name = $$${index + 1}"
                          .mkString(", ")

                        val stmtRange = session
                          .createStatement("UPDATE person_projection SET " + expr + " WHERE id = $" + (fields.size + 1))

                        (fields.map(_._2) ++ List[Option[Any]](Some(uuidKey)))
                          .zipWithIndex
                          .foldLeft(stmtRange):
                              case (stmt, (data, i)) => stmt.bind(i, data.get)

        batch.map {
          case stmts => session.update(stmts.toVector)
        }.map(
          _ => Done
        )

    end process
