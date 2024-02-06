package infrastructure
package entities
package person

import akka.serialization.jackson.CborSerializable

import akka.actor.typed.ActorRef
import akka.Done

object Commands:

    import DataModel.*
    import util.ResultError

    sealed trait Command extends CborSerializable:
        def replyTo: ActorRef[ResultError]

    final case class CreatePersonCommand(person: Person, replyTo: ActorRef[Done | ResultError])       extends Command
    final case class UpdatePersonCommand(person: PersonUpdate, replyTo: ActorRef[Done | ResultError]) extends Command
    final case class GetPersonCommand(replyTo: ActorRef[Person | ResultError])                        extends Command
    final case class StopPersonCommand(replyTo: ActorRef[Done | ResultError])                        extends Command
    final case class StartPersonCommand(replyTo: ActorRef[Done | ResultError])                        extends Command
//    case object GoodByeCommand extends Command