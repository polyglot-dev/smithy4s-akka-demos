package infrastructure
package entities

import org.slf4j.{ Logger, LoggerFactory }

// import person.DataModel.*
// import person.Commands.*
// import util.*
import person.Events.*

def getIfNotNone[A](a: Option[A], b: Option[A]): Option[A] =
  a match
    case None => b
    case _    => a

trait PersonEventHandler:
    this: PersonEntity.State =>

    def applyEvent(event: Event)(using logger: Logger): PersonEntity.State =
      event match
        case PersonUpdated(t, a)    =>
          logger.info(s"PersonUpdated: $event")
          copy(
            town = getIfNotNone(t, town),
            address = getIfNotNone(a, address)
          )
        case PersonFixed(n, t, a)   =>
          logger.info(s"PersonFixed: $event")
          copy(
            name = n.getOrElse(name),
            town = getIfNotNone(t, town),
            address = getIfNotNone(a, address)
          )

        case Fixing(value)   =>
          logger.info(s"Fixing: $event")
          copy(
            isBeingFixed = value
          )
          
        case PersonCreated(_, _, _) => throw new IllegalStateException(s"unexpected event [$event] in active state")

//trait PersonInRecoveryStateEventHandler:
//    this: PersonEntity.State =>
//
//    def applyEventInRecovery(event: Event): PersonEntity.State =
//      // TODO: report all events to reporter actor
//      event match
//        case PersonUpdated(t, a)                    =>
//          copy(
//            town = getIfNotNone(t, town),
//            address = getIfNotNone(a, address)
//          )
//        case PersonFixed(n, t, a)                   =>
//          copy(
//            name = n.getOrElse(name),
//            town = getIfNotNone(t, town),
//            address = getIfNotNone(a, address)
//          )
//        case Fixing(value)   =>
//          copy(
//            isBeingFixed = value
//          )
//          
//        case e @ PersonCreated(name, town, address) => PersonEntity.State(name, town, address)
