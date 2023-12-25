package infrastructure
package entities
package person

import akka.serialization.jackson.CborSerializable

object Events:
    import DataModel.*

    sealed trait Event                                                                           extends CborSerializable
    case class PersonCreated(name: String, town: Option[String], address: Option[Address])       extends Event
    case class PersonUpdated(town: Option[String], address: Option[Address])                     extends Event
    case class PersonFixed(name: Option[String], town: Option[String], address: Option[Address]) extends Event
