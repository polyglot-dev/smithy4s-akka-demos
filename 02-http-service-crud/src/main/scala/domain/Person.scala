package domain
package data

import infrastructure.http.ErrorsBuilder.*

import io.github.arainko.ducktape.*
import _root_.infrastructure.internal.Person as ExternalPerson
import _root_.infrastructure.internal.PersonInfo as ExternalPersonInfo

case class Address(name: Option[String], n: Option[Int])

case class Person(name: String, town: String, address: Option[Address] = None)

case class PersonInfo(name: Option[String] = None, town: Option[String] = None)

object Person:
    def from(p: ExternalPerson): Either[String, Person] =
        p match
            case ExternalPerson("", town, address) =>
                Left("name is mandatory")
            case ExternalPerson(name, "", address) =>
                Left("town is mandatory")
            case person: ExternalPerson =>
                Right(person.to[Person])

object PersonInfo:
    def from(p: ExternalPersonInfo): Either[String, PersonInfo] =
        p match
            case ExternalPersonInfo(None, None) =>
                Left("You need to send at least one field")
            case ExternalPersonInfo(Some(""), _) =>
                Left("name can not be empty")
            case ExternalPersonInfo(_, Some("")) =>
                Left("town can not be empty")
            case person: ExternalPersonInfo =>
                Right(person.to[PersonInfo])