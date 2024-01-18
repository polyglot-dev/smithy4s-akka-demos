package infrastructure
package http
package services

import logstage.IzLogger
import domain.types.*
import types.*
// import io.github.arainko.ducktape.*

import _root_.io.scalaland.chimney.dsl.*
import _root_.io.scalaland.chimney.Transformer
import io.scalaland.chimney.partial

import _root_.infrastructure.internal.*

import ErrorsBuilder.*

import cats.data.EitherT
import scala.util.{ Failure, Success, Try }
import TypesConversion.*
import infrastructure.resources.Handle
import infrastructure.*

import org.integration.avro.ad
import org.integration.avro.people as pe

import infrastructure.http.transformers.AdvertisersTransformers.given
import integration.serializers.*

// import cats.effect.unsafe.implicits.global
import _root_.main.Producer
import _root_.main.ProducerParams

import fs2.concurrent.Channel

import org.apache.avro.specific.SpecificRecord

class AdvertiserServiceImpl(
                           repo: AdvertiserRepository[IO],
                           handler: Option[Producer[ProducerParams]] = None,
                           logger: Option[IzLogger] = None) extends AdvertiserService[Result] {

  def getPersonById(id: String): Result[Person] = {

    logger.foreach(_.info(s"Service, getting data from name: '$id'"))

    def validatePreconditions: Either[ServiceError, Tuple1[Long]] = {
      for
        okId <- convertId(id)
      yield Tuple1(okId)
    }

    validatePreconditions.fold(
      error => error.toResult,
      {
        case Tuple1[Long](okId) =>
          for
            repositoryResult <- repo.getPersonById(okId)
          yield {
            repositoryResult match
              case Right(Some(person)) =>
                // Right(person.transformInto[Person])

                person.transformIntoPartial[Person].asEither match
                  case Left(value) =>
                    val msg = value.errors.map(
                      (er: partial.Error) => er._1.asString
                    ).mkString(",")
                    Left(badRequestError(msg))
                  // .toResult

                  case Right(value) => Right(value)

              case Right(None) => Left(notFoundError(f"Data with id: ${id} missing"))

              case Left(ex) => Left(serviceUnavailableError("Error accessing data"))
          }
      }
    ).toResult
  }

  def updatePerson(body: PersonInfo, id: Long): Result[Person] = {
    logger.foreach(_.info(s"Service, updating person: '$body'"))

    def validatePreconditions: Either[ServiceError, Tuple1[domain.data.PersonInfo]] = {
      for
        person <- domain.data.PersonInfo.from(body)
      yield Tuple1(person)
    }

    validatePreconditions.fold(
      error => error.toResult,
      {
        case Tuple1[domain.data.PersonInfo](person) =>
          for
            repositoryResult <- repo.updatePerson(person, id)
          yield {
            repositoryResult match
              case Right(None)    => Left(notFoundError(f"Data with id: ${id} missing"))
              case Right(Some(p)) =>
                // Right(p.transformInto[Person])

                p.transformIntoPartial[Person].asEither match
                  case Left(value) =>
                    val msg = value.errors.map(
                      (er: partial.Error) => er._1.asString
                    ).mkString(",")
                    Left(badRequestError(msg))

                  case Right(value) => Right(value)

              case Left(ex) =>
                if ex.getMessage contains "duplicate key value" then
                    Left(conflictError(s"Advertiser with name: ${body.name.get} already exists"))
                else if ex.getMessage contains "Person not found" then
                    Left(notFoundError(f"Data with id: ${id} missing"))
                else
                    logger.foreach(_.error(s"${ex.getMessage()}"))
                    Left(serviceUnavailableError("Error accessing data"))
          }
      }
    ).toResult
  }

  def createPerson(body: Person): Result[Long] = {
    logger.foreach(_.info(s"Service, creating advertiser: '$body'"))

    def validatePreconditions: Either[ServiceError, Tuple1[domain.data.Person]] = {
      for
        person <- domain.data.Person.from(body)
      yield Tuple1(person)
    }

    def send2(repositoryResult: Either[Throwable, Option[domain.data.Person]]): IO[Either[Channel.Closed, Unit]] = {
      repositoryResult match
        case Right(Some(p)) =>
          handler match
            case Some(h) =>
              val x = ProducerParams("campaigns.advertiser-update.v1", "key", ad.Advertiser(p.name.length.toLong, ad.Status.ACTIVE))
              // val x = ProducerParams("country.person-create.v1", "key", pe.Person(33L, p.name))
              h.produce(x)
            case None    => IO { Left(Channel.Closed) }
        case Right(None)    => IO { Left(Channel.Closed) }
        case Left(value)    => IO.raiseError(value)
    }

    def send(repositoryResult: Either[Throwable, Long]): IO[Either[Channel.Closed, Unit]] = {
      repositoryResult match
        case Right(id)   =>
          for
              i <- repo.getPersonById(id)
              r <- send2(i)
          yield r
        case Left(value) => IO.raiseError(value)
    }

    validatePreconditions.fold(
      error => error.toResult,
      {
        case Tuple1[domain.data.Person](person) =>
          for
              repositoryResult <- repo.savePerson(person)
              // IO[Either[Throwable, Long]]
              // repositoryResult <- IO[Either[Throwable, Long]]{Right(2L)}
              _ <- send(repositoryResult)
          yield {
            repositoryResult match
              case Right(id) =>
                // TODO: Report the ID of the created resource via Fafka
                Right(id)

              case Left(ex) =>
                if ex.getMessage contains "duplicate key value" then
                    Left(conflictError(s"Advertiser with name: ${body.name} already exists"))
                else
                    logger.foreach(_.error(s"${ex.getMessage()}"))
                    Left(serviceUnavailableError("Error accessing data"))
          }
      }
    ).toResult
  }

}
