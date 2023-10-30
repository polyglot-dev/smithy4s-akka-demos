package infrastructure
package http
package services

import logstage.IzLogger
import domain.types.*
import types.*
import io.github.arainko.ducktape.*

import _root_.infrastructure.internal.*
import infrastructure.internal.common.*

class AdvertiserServiceImpl(
                           repo: AdvertiserRepository[IO],
                           logger: Option[IzLogger] = None) extends AdvertiserService[IO] {

  def getPersonById(id: String): IO[Person] = {

    logger.foreach(_.info(s"Service, getting data from name: '$id'"))

    for
      repositoryResult <- repo.getPersonById(id.toLong)
    yield {
      repositoryResult match
        case Right(Some(person)) => person.to[Person]
        case Right(None)         => throw NotFoundError(404, f"Data with id: ${id} missing")

        case Left(ex) =>
          logger.foreach(_.error(s"${ex.getMessage()}"))
          throw ServiceUnavailableError(503, "Error accessing data")
    }
  }

  def updatePerson(body: PersonInfo, id: Long): IO[Person] = {
    logger.foreach(_.info(s"Service, updating person: '$body'"))

    body match
      case PersonInfo(None, None) => throw BadRequestError(404, "You need to send at least one field")
      case _                      =>
        for
          repositoryResult <- repo.updatePerson(body.to[domain.data.PersonInfo], id)
        yield {
          repositoryResult match
            case Right(None)    => throw NotFoundError(404, f"Data with id: ${id} missing")
            case Right(Some(p)) => p.to[Person]
            case Left(ex)       =>
              if ex.getMessage contains "duplicate key value" then
                  throw ConflictError(409, s"Advertiser with name: ${body.name.get} already exists")
              else if ex.getMessage contains "Person not found" then
                  throw NotFoundError(404, f"Data with id: ${id} missing")
              else
                  logger.foreach(_.error(s"${ex.getMessage()}"))
                  throw ServiceUnavailableError(503, "Error accessing data")
        }
  }

  def createPerson(body: Person): IO[Long] = {
    logger.foreach(_.info(s"Service, creating advertiser: '$body'"))

    for
      repositoryResult <- repo.savePerson(body.to[domain.data.Person])
    yield {
      repositoryResult match
        case Right(id) => id
        case Left(ex)  =>
          if ex.getMessage contains "duplicate key value" then
              throw ConflictError(409, s"Advertiser with name: ${body.name.get} already exists")
          else
              logger.foreach(_.error(s"${ex.getMessage()}"))
              throw ServiceUnavailableError(503, "Error accessing data")
    }
  }

}
