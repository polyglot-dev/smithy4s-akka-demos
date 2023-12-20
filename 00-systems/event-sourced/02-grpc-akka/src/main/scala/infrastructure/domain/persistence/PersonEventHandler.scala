package infrastructure
package entities

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

    def applyEvent(event: Event): PersonEntity.State =
      event match
        case PersonUpdated(n, t, a) =>
          copy(
            name = n.getOrElse(name),
            town = getIfNotNone(t, town),
            address = getIfNotNone(a, address)
          )
        case PersonCreated(_, _, _) => throw new IllegalStateException(s"unexpected event [$event] in active state")
