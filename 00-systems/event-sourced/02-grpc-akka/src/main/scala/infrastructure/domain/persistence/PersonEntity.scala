package infrastructure
package entities

import services.Configs.*

import akka.serialization.jackson.CborSerializable
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId

import akka.persistence.typed.scaladsl.RetentionCriteria
import akka.actor.typed.SupervisorStrategy
import akka.persistence.typed.scaladsl.Effect
import akka.Done

import util.person.EventsTags

object PersonEntity extends PersonEntityCommon:

    export person.DataModel.*
    export person.Commands.*
    export person.Events.*
    import util.*

    val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("person")

    type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, Option[State]]

// format: off
    case class State(
      name: String, 
      town: Option[String], 
      address: Option[Address],
    ) extends CborSerializable, 
              PersonEntityCommon,
              PersonCommandHandler,
              PersonEventHandler
// format: on

    def onFirstCommand(cmd: Command): ReplyEffect =
      cmd match
        case CreatePersonCommand(person: Person, replyTo) =>
          Effect.persist(
            PersonCreated(
              person.name,
              person.town,
              person.address,
            )
          ).thenReply(replyTo)(
            _ => Done
          )
        case default                                      =>
          Effect
            .none
            .thenReply(default.replyTo)(
              _ =>
                ResultError(
                  TransportError.NotFound,
                  "Person do not exists"
                )
            )

    def onFirstEvent(event: Event): State =
      event match
        case e @ PersonCreated(name, town, address) =>
          logger.info(s"PersonCreated: $e")
          State(name, town, address)
        case _                                      => throw new IllegalStateException(s"unexpected event [$event] in empthy state")

    def apply
      (persistenceId: PersistenceId)(using config: PersonEntityConfig)
      : Behavior[Command] = Behaviors.setup[Command]:
        context =>
            EventSourcedBehavior.withEnforcedReplies[Command, Event, Option[State]](
              persistenceId,
              None,
              (state, cmd) =>
                state match {
                  case None         => onFirstCommand(cmd)
                  case Some(person) => person.applyCommand(cmd)
                },
              (state, event) =>
                state match {
                  case None         => Some(onFirstEvent(event))
                  case Some(person) => Some(person.applyEvent(event))
                }
            )
              .withTagger:
                  case _: PersonCreated => Set(EventsTags.PersonCreated.value, EventsTags.PersonCreateUpdated.value)
                  case _: PersonUpdated => Set(EventsTags.PersonUpdated.value, EventsTags.PersonCreateUpdated.value)
                  case _: PersonFixed   => Set(EventsTags.PersonUpdated.value, EventsTags.PersonCreateUpdated.value)
              .snapshotWhen {
                 case (state, _, sequenceNumber) => true
                }
              .withRetention(
                RetentionCriteria
                  .snapshotEvery(
                    numberOfEvents = config.snapshotNumberOfEvents,
                    keepNSnapshots = config.snapshotKeepNsnapshots
                  )
              ).onPersistFailure(
                SupervisorStrategy.restartWithBackoff(
                  minBackoff = config.restartMinBackoff,
                  maxBackoff = config.restartMaxBackoff,
                  randomFactor = config.restartRandomFactor
                )
              )
            // .eventAdapter()

object PersonEntity2:

    export person.DataModel.*
    export person.Commands.*
    export person.Events.*
    import util.*

    val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("person")

    type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, Option[State]]

// format: off
    case class State(
      name: String, 
      town: Option[String], 
      address: Option[Address],
    ) extends CborSerializable, 
              PersonCommandHandler2,
              PersonEventHandler2
// format: on

    def onFirstCommand(cmd: Command): ReplyEffect =
      cmd match
        case CreatePersonCommand(person: Person, replyTo) =>
          Effect.persist(
            PersonCreated(
              person.name,
              person.town,
              person.address,
            )
          ).thenReply(replyTo)(
            _ => Done
          )
        case default                                      =>
          Effect
            .none
            .thenReply(default.replyTo)(
              _ =>
                ResultError(
                  TransportError.NotFound,
                  "Person do not exists"
                )
            )

    def onFirstEvent(event: Event): State =
      event match
        case PersonCreated(name, town, address) => State(name, town, address)
        case _                                  => throw new IllegalStateException(s"unexpected event [$event] in empthy state")

    def apply
      (persistenceId: PersistenceId)(using config: PersonEntityConfig)
      : Behavior[Command] = Behaviors.setup[Command]:
        context =>
            EventSourcedBehavior.withEnforcedReplies[Command, Event, Option[State]](
              persistenceId,
              None,
              (state, cmd) =>
                state match {
                  case None         => onFirstCommand(cmd)
                  case Some(person) => person.applyCommand(cmd)
                },
              (state, event) =>
                state match {
                  case None         => Some(onFirstEvent(event))
                  case Some(person) => Some(person.applyEvent(event))
                }
            )
              .withTagger:
                  case _: PersonCreated => Set(EventsTags.PersonCreated.value, EventsTags.PersonCreateUpdated.value)
                  case _: PersonUpdated => Set(EventsTags.PersonUpdated.value, EventsTags.PersonCreateUpdated.value)
                  case _: PersonFixed   => Set(EventsTags.PersonUpdated.value, EventsTags.PersonCreateUpdated.value)
              .withRetention(
                RetentionCriteria
                  .snapshotEvery(
                    numberOfEvents = config.snapshotNumberOfEvents,
                    keepNSnapshots = config.snapshotKeepNsnapshots
                  )
              ).onPersistFailure(
                SupervisorStrategy.restartWithBackoff(
                  minBackoff = config.restartMinBackoff,
                  maxBackoff = config.restartMaxBackoff,
                  randomFactor = config.restartRandomFactor
                )
              )
            // .eventAdapter()
