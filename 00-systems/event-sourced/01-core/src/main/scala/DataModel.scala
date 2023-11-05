package infrastructure
package entities
package person

import java.util.Date

object DataModel:

    sealed trait Model extends CborSerializable

    case class Person(name: String, town: Option[String]) extends Model

    case class PersonUpdate(name: Option[String], town: Option[String]) extends Model
