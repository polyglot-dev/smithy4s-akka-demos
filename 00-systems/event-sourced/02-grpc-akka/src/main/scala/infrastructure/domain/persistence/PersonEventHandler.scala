package infrastructure
package entities

import person.DataModel.*
import person.Commands.*
import person.Events.*
import util.*

trait PersonEventHandler:
    this: PersonEntity.State =>

    def applyEvent(event: Event): PersonEntity.State =
      event match
        case PersonUpdated(Some(n), t, a) => copy(n, t, a)
        case PersonUpdated(None, t, a)    => copy(town = t, address = a)
        case PersonCreated(_, _, _)       => throw new IllegalStateException(s"unexpected event [$event] in active state")
