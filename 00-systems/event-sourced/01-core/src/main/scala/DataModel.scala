package infrastructure
package entities
package person

import akka.serialization.jackson.CborSerializable

object DataModel:

    enum Status:
        case active, inactive

    sealed trait Model extends CborSerializable

    case class Address(street: String, no: Int) extends Model

    case class Person(name: String, town: Option[String], address: Option[Address], status: Status = Status.active) extends Model

    case class PersonUpdate(name: Option[String], town: Option[String], address: Option[Address]) extends Model
