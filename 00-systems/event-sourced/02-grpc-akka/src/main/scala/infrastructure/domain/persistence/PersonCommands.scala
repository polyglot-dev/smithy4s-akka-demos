package infrastructure
package entities
package person

import akka.actor.typed.ActorRef
import akka.Done
import java.util.Date
import akka.pattern.StatusReply

object Commands:

    import DataModel.*
    import util.ResultError

    sealed trait Command extends CborSerializable

    final case class CreatePersonCommand(person: Person, replyTo: ActorRef[Done | ResultError])       extends Command
    final case class UpdatePersonCommand(person: PersonUpdate, replyTo: ActorRef[Done | ResultError]) extends Command
    final case class GetPersonCommand(replyTo: ActorRef[Person | ResultError])                        extends Command
