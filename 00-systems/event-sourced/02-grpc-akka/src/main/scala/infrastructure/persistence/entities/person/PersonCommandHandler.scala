package infrastructure
package entities

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.typed.scaladsl.Effect
import akka.Done

import person.DataModel.*
import person.Commands.*
import person.Events.*
import util.*

trait PersonCommandHandler:
    this: PersonEntity.State =>

    def applyCommand(cmd: Command)(using shard: ActorRef[ClusterSharding.ShardCommand], ctx: ActorContext[Command]): PersonEntity.ReplyEffect =
      cmd match

        case UpdatePersonCommand(PersonUpdate(None, None), replyTo) =>
          Effect.reply(replyTo)(
            ResultError(
              TransportError.BadRequest,
              "To update a Person you must provide at least one field"
            )
          )

        case UpdatePersonCommand(p: PersonUpdate, replyTo) =>
          Effect.persist(PersonUpdated(p.town, p.address)).thenReply(replyTo)(
            _ => Done
          )

        case GetPersonCommand(replyTo) =>
          Effect.reply(replyTo)(
            Person(name, town, address)
          )

        case StopPersonCommand(replyTo) =>
          shard ! ClusterSharding.Passivate(ctx.self)
          Effect.persist(Fixing(true)).thenReply(replyTo)(
            _ => Done
          )

        case StartPersonCommand(replyTo) =>
          Effect.persist(Fixing(false)).thenReply(replyTo)(
            _ => Done
          )

        case CreatePersonCommand(p: Person, replyTo) =>
          Effect.reply(replyTo)(
            ResultError(
              TransportError.BadRequest,
              "Person already exists"
            )
          )

//trait PersonInRecoveryStateCommandHandler:
//    this: PersonEntity.State =>
//
//    def applyCommandInRecovery(cmd: Command): PersonEntity.ReplyEffect =
//      cmd match
//        case UpdatePersonCommand(p: PersonUpdate, replyTo) =>
//          Effect.persist(PersonUpdated(p.town, p.address)).thenReply(replyTo)(
//            _ => Done
//          )
//
//        case GetPersonCommand(replyTo) =>
//          Effect.reply(replyTo)(
//            Person(name, town, address)
//          )
//
//        case StopPersonCommand(replyTo) =>
//          Effect.persist(Fixing(true)).thenReply(replyTo)(
//            _ => Done
//          )
//
//        case StartPersonCommand(replyTo) =>
//          Effect.persist(Fixing(false)).thenReply(replyTo)(
//            _ => Done
//          )
//
//        case CreatePersonCommand(p: Person, replyTo) =>
//          Effect.reply(replyTo)(
//            ResultError(
//              TransportError.BadRequest,
//              "Person already exists"
//            )
//          )
