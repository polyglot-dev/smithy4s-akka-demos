package infrastructure
package entities
package person

import java.util.Date

object DataModel:

    sealed trait Model extends CborSerializable

    case class Address(street: String, no: Int) extends Model

    case class Person(name: String, town: Option[String], address: Option[Address]) extends Model

    case class PersonUpdate(name: Option[String], town: Option[String], address: Option[Address]) extends Model
