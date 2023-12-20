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


import infrastructure.http.transformers.AdvertisersTransformers.given
import integration.serializers.*

class AdvertiserServiceImpl(
                           repo: AdvertiserRepository[IO],
                           handler: Option[Handler[Dtos.Advertiser]] = None,
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

                  case Right(value) =>
                    Right(value)
                
              case Right(None)         => Left(notFoundError(f"Data with id: ${id} missing"))

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

                  case Right(value) =>
                    Right(value)
                
              case Left(ex)       =>
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

  import cats.effect.unsafe.implicits.global

  def createPerson(body: Person): Result[Long] = {
    logger.foreach(_.info(s"Service, creating advertiser: '$body'"))

    def validatePreconditions: Either[ServiceError, Tuple1[domain.data.Person]] = {
      for
        person <- domain.data.Person.from(body)
      yield Tuple1(person)
    }

    validatePreconditions.fold(
      error => error.toResult,
      {
        case Tuple1[domain.data.Person](person) =>
          for
              repositoryResult <- repo.savePerson(person)
              // IO[Either[Throwable, Long]]
              // repositoryResult <- IO[Either[Throwable, Long]]{Right(2L)}
          yield {
            repositoryResult match
              case Right(id) =>
                // TODO: Report the ID of the created resource via Fafka
                // logger.foreach(_.info(s"About to offer: '$id'"))
                logger.foreach(_.info(s"person saved: '$person'"))
                // handler.queue.map(_.send(Dtos.Advertiser(id, Dtos.Status.ACTIVE)).unsafeRunAndForget())
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
