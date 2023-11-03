package infrastructure
package http
package services

import logstage.IzLogger
import domain.types.*
import types.*
import io.github.arainko.ducktape.*

import _root_.infrastructure.internal.*

import ErrorsBuilder.*

import cats.data.EitherT
import scala.util.{ Failure, Success, Try }
import TypesConversion.*

class AdvertiserServiceImpl(
                           repo: AdvertiserRepository[IO],
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
              case Right(Some(person)) => Right(person.to[Person])
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
              case Right(Some(p)) => Right(p.to[Person])
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
          yield {
            repositoryResult match
              case Right(id) => Right(id)
              case Left(ex)  =>
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
