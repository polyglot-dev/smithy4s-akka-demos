package infrastructure
package entities
package person

import akka.serialization.jackson.CborSerializable

object DataModel:

    enum Status extends Model:
        case active, inactive

    sealed trait Model extends CborSerializable

    case class Address(street: String, no: Long) extends Model

    case class Person(name: String, town: Option[String], address: Option[Address]) extends Model
    // , status: Status = Status.active

    case class PersonUpdate(town: Option[String], address: Option[Address]) extends Model
