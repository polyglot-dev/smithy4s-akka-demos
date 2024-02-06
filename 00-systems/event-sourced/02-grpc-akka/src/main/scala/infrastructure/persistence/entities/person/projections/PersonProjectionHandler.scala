package infrastructure
package entities
package person
package projections

import collection.mutable
import scala.collection.immutable

import akka.projection.eventsourced.EventEnvelope
import akka.projection.r2dbc.scaladsl.R2dbcHandler
import akka.projection.r2dbc.scaladsl.R2dbcSession

import io.r2dbc.spi.Row
import io.r2dbc.postgresql.codec.Json

import akka.Done
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.slf4j.{ Logger, LoggerFactory }
import io.circe.*, io.circe.syntax.*, io.circe.parser.*, io.circe.generic.auto.*

import person.DataModel.*
import infrastructure.entities.person.Events.{ Event as _, * }

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

        val eventsIds =
          envelopes.map(
            e => e.persistenceId.substring(PersonEntity.typeKey.name.length() + 1)
          ).distinct.toList

        val eventsIdsSegment = eventsIds.indices.map(_ + 1).map(
          "$" + _
        ).mkString("(", ", ", ")")

        val finalEventsIdsSegment =
          eventsIdsSegment match
            case "()" => "('')"
            case other => other

        val stmtRange = session
          .createStatement("SELECT id, name, town, address FROM person_projection WHERE id IN " + finalEventsIdsSegment)

        val stmtForAllExistingPersons =
          eventsIds
            .zipWithIndex
            .foldLeft(stmtRange):
              case (stmt, (id, i)) => stmt.bind(i, id)

        val existingPersonsTuplesF: Future[IndexedSeq[(String, String, Option[String], Option[Json])]] =
          session
            .select(stmtForAllExistingPersons)(
              (row: Row) =>
                (row.get("id", classOf[String]),
                 row.get("name", classOf[String]),
                 Option(row.get("town", classOf[String])),
                 Option(row.get("address", classOf[Json])),
                )
            )

        val batch = existingPersonsTuplesF.map:
          case existingPersonsTuples =>
            val existingPersonsSeq = existingPersonsTuples.map {
              case (id: String, name: String, town: Option[String], address: Option[Json]) =>
                val addressValue = address.map(
                  v =>
                    decode[Address](v.asString).toOption
                )
                (id, PersonEntity.State(name, town, addressValue.flatten))
            }

            val events = mutable.ArrayBuffer.empty[(String, DataModel.Event)]
            events ++= envelopes.map(
              env => {
                val uuidKey = env.persistenceId.substring(PersonEntity.typeKey.name.length() + 1)
                (uuidKey, env.event)
              }
            )

            val existingPersons : mutable.Map[String, PersonEntity.State] = mutable.Map(existingPersonsSeq: _*)
            val newItems = mutable.Map.empty[String, PersonEntity.State]
            val personsToUpdate = mutable.Set.empty[String]

            while events.nonEmpty do
              val (uuidKey, event) = events.remove(0)
              val vv = adapter.fromJournal(event, "").events.toList.head
              vv match
                case ev: PersonCreated => 
                  newItems += (uuidKey -> PersonEntity.onFirstEvent(ev))
                case ev =>
                  if newItems.contains(uuidKey) then
                    newItems.update(uuidKey, newItems(uuidKey).applyEvent(ev))
                  else
                    existingPersons.update(uuidKey, existingPersons(uuidKey).applyEvent(ev))
                    personsToUpdate += uuidKey


            val inserts = newItems.map:
              case (uuidKey, state) =>
                logger.info(s"Creating person projection for $uuidKey")
                val addressJson = state.address.map(address => Json.of(address.asJson.noSpaces))
                val optionalFields = List[(String, Option[Any])](
                                                          ("id", Some(uuidKey)),
                                                          ("name", Some(state.name)),
                                                          ("town", state.town),
                                                          ("address", addressJson)
                )
                val fields = optionalFields.filter(_._2.isDefined)
                val expr = fields
                  .map(_._1)
                  .mkString(", ")
                val stmtRange = session
                  .createStatement("INSERT INTO person_projection (" + expr + ") VALUES (" + fields.indices.map(_ + 1).map(
                    "$" + _
                  ).mkString(", ") + ")")
                
                fields.map(_._2)
                  .zipWithIndex
                  .foldLeft(stmtRange):
                      case (stmt, (data, i)) => stmt.bind(i, data.get)

            
            val updates = personsToUpdate.map:
                case uuidKey =>
                  logger.info(s"Updating person projection for $uuidKey")
                  val state = existingPersons(uuidKey)

                  val addressJson = state.address.map(address => Json.of(address.asJson.noSpaces))
                  val optionalFields = List[(String, Option[Any])](
                                                            ("town", state.town),
                                                            ("address", addressJson)
                  )
                  val fields = optionalFields.filter(_._2.isDefined)
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
                
            inserts ++ updates
        
        batch.map {
          case stmts => session.update(stmts.toVector)
        }.map(
          _ => Done
        )
      
      
    end process
