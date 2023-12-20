package domain
package data

import infrastructure.http.ErrorsBuilder.*

import _root_.io.scalaland.chimney.dsl.*
import _root_.io.scalaland.chimney.Transformer
import io.scalaland.chimney.partial

// import io.github.arainko.ducktape.*
import _root_.infrastructure.internal.Person as ExternalPerson
import _root_.infrastructure.internal.PersonInfo as ExternalPersonInfo

import infrastructure.http.ErrorsBuilder.*

enum AddressTypes:
  case Address(name: Option[String], n: Option[Int])
  case FullAddress(name: Option[String], n: Option[Int], country: Option[String])

case class Person(name: String, town: String, address: Option[AddressTypes] = None)

case class PersonInfo(name: Option[String] = None, town: Option[String] = None)


import infrastructure.http.transformers.AdvertisersTransformers.given

object Person:

    def from(p: ExternalPerson): Either[infrastructure.http.ServiceError, Person] =
      p match
        case ExternalPerson("", town, address) => Left(badRequestError("name is mandatory"))
        case ExternalPerson(name, "", address) => Left(badRequestError("town is mandatory"))
        case person: ExternalPerson            => 
          
                person.transformIntoPartial[Person].asEither match
                  case Left(value) =>
                    val msg = value.errors.map(
                      (er: partial.Error) => er._1.asString
                    ).mkString(",")
                    Left(badRequestError(msg))
                      // .toResult

                  case Right(value) =>
                    Right(value)
          // Right(person.transformInto[Person])

object PersonInfo:

    def from(p: ExternalPersonInfo): Either[infrastructure.http.ServiceError, PersonInfo] =
      p match
        case ExternalPersonInfo(None, None)  => Left(badRequestError("You need to send at least one field"))
        case ExternalPersonInfo(Some(""), _) => Left(badRequestError("name can not be empty"))
        case ExternalPersonInfo(_, Some("")) => Left(badRequestError("town can not be empty"))
        case person: ExternalPersonInfo      => Right(person.transformInto[PersonInfo])
