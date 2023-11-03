package domain
package data

import infrastructure.http.ErrorsBuilder.*

import io.github.arainko.ducktape.*
import _root_.infrastructure.internal.Person as ExternalPerson
import _root_.infrastructure.internal.PersonInfo as ExternalPersonInfo

import infrastructure.http.ErrorsBuilder.*

case class Address(name: Option[String], n: Option[Int])

case class Person(name: String, town: String, address: Option[Address] = None)

case class PersonInfo(name: Option[String] = None, town: Option[String] = None)

object Person:

    def from(p: ExternalPerson): Either[infrastructure.http.ServiceError, Person] =
      p match
        case ExternalPerson("", town, address) => Left(badRequestError("name is mandatory"))
        case ExternalPerson(name, "", address) => Left(badRequestError("town is mandatory"))
        case person: ExternalPerson            => Right(person.to[Person])

object PersonInfo:

    def from(p: ExternalPersonInfo): Either[infrastructure.http.ServiceError, PersonInfo] =
      p match
        case ExternalPersonInfo(None, None)  => Left(badRequestError("You need to send at least one field"))
        case ExternalPersonInfo(Some(""), _) => Left(badRequestError("name can not be empty"))
        case ExternalPersonInfo(_, Some("")) => Left(badRequestError("town can not be empty"))
        case person: ExternalPersonInfo      => Right(person.to[PersonInfo])
