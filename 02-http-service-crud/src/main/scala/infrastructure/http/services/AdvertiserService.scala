package infrastructure
package http
package services

import logstage.IzLogger
import domain.types.*
import types.*
import io.github.arainko.ducktape.*

import _root_.infrastructure.internal.*

import ErrorsBuilder.*

extension (self: String)
  def idToLong: Long = 
    import scala.util.control.Exception.allCatch
    allCatch.opt(self.trim.toLong)
    .getOrElse(throw badRequestError(s"Invalid id: ${self}"))

    
class AdvertiserServiceImpl(
                           repo: AdvertiserRepository[IO],
                           logger: Option[IzLogger] = None) extends AdvertiserService[IO] {

  def getPersonById(id: String): IO[Person] = {

    logger.foreach(_.info(s"Service, getting data from name: '$id'"))

    for
      repositoryResult <- repo.getPersonById(id.idToLong)
    yield {
      repositoryResult match
        case Right(Some(person)) => person.to[Person]
        case Right(None)         =>
          throw notFoundError(f"Data with id: ${id} missing")

        case Left(ex) =>
          throw serviceUnavailableError("Error accessing data")
    }
  }

  def updatePerson(body: PersonInfo, id: Long): IO[Person] = {
    logger.foreach(_.info(s"Service, updating person: '$body'"))
    
    domain.data.PersonInfo.from(body) match
      case Left(ex) => throw badRequestError(ex)
      case Right(person) => 
        for
          repositoryResult <- repo.updatePerson(person, id)
        yield {
          repositoryResult match
            case Right(None)    => throw notFoundError(f"Data with id: ${id} missing")
            case Right(Some(p)) => p.to[Person]
            case Left(ex)       =>
              if ex.getMessage contains "duplicate key value" then
                  throw conflictError(s"Advertiser with name: ${body.name.get} already exists")
              else if ex.getMessage contains "Person not found" then
                  throw notFoundError(f"Data with id: ${id} missing")
              else
                  logger.foreach(_.error(s"${ex.getMessage()}"))
                  throw serviceUnavailableError("Error accessing data")
        }
  }

  def createPerson(body: Person): IO[Long] = {
    logger.foreach(_.info(s"Service, creating advertiser: '$body'"))
    
    domain.data.Person.from(body) match
      case Left(ex) => throw badRequestError(ex)
      case Right(person) => 
        for
          repositoryResult <- repo.savePerson(person)
        yield {
          repositoryResult match
            case Right(id) => id
            case Left(ex)  =>
              if ex.getMessage contains "duplicate key value" then
                  throw conflictError(s"Advertiser with name: ${body.name} already exists")
              else
                  logger.foreach(_.error(s"${ex.getMessage()}"))
                  throw serviceUnavailableError("Error accessing data")
        }
  }

}
