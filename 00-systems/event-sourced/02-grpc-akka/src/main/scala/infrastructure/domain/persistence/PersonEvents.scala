package infrastructure
package entities
package person

import java.util.Date

object Events:
    import DataModel.*

    sealed trait Event                                                   extends CborSerializable
    case class PersonCreated(name: String, town: Option[String])         extends Event
    case class PersonUpdated(name: Option[String], town: Option[String]) extends Event
