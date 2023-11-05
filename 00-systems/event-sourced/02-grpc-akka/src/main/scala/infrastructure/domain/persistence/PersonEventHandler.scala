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
        case PersonUpdated(Some(n), t) => copy(name = n, town = t)
        case PersonUpdated(None, t)    => copy(town = t)
        case PersonCreated(_, _)       => throw new IllegalStateException(s"unexpected event [$event] in active state")
