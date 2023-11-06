package infrastructure
package entities

import akka.persistence.typed.scaladsl.Effect
import akka.Done

import person.DataModel.*
import person.Commands.*
import person.Events.*
import util.*

trait PersonCommandHandler:
    this: PersonEntity.State =>

    def applyCommand(cmd: Command): PersonEntity.ReplyEffect =
      cmd match
        case UpdatePersonCommand(p: PersonUpdate, replyTo) =>
          Effect.persist(PersonUpdated(p.name, p.town, p.address)).thenReply(replyTo)(
            _ => Done
          )

        case GetPersonCommand(replyTo) =>
          Effect.none.thenReply(replyTo)(
            _ => Person(name, town, address)
          )

        case CreatePersonCommand(p: Person, replyTo) =>
          Effect.reply(replyTo)(
            ResultError(
              TransportError.BadRequest,
              "Person already exists"
            )
          )
