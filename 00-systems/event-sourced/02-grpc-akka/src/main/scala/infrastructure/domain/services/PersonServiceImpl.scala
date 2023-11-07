package infrastructure
package services

import Configs.*

import scala.concurrent.Future
import akka.Done

import akka.actor.typed.ActorSystem
import scala.concurrent.ExecutionContextExecutor
import akka.util.Timeout
// import org.slf4j.LoggerFactory
// import java.util.Date

import infrastructure.entities.person.DataModel.*
import infrastructure.entities.PersonEntity

import util.ResultError
// import util.TransportError

import person.*

// import akka.stream.scaladsl.Flow
// import akka.stream.scaladsl.Source

// import services.TypesConversion.given

class PersonServiceImpl(personSharding: PersonSharding)
                       (
                         using sys: ActorSystem[Nothing],
                         config: ServiceConfig) extends PersonService:
    given ec: ExecutionContextExecutor = sys.executionContext
    given timeout: Timeout = config.requestToActorsTimeout
    // private val logger = LoggerFactory.getLogger(getClass)

    def createPerson(id: String, data: Person): Future[Done | ResultError] = personSharding
      .entityRefFor(PersonEntity.typeKey, id)
      .ask(PersonEntity.CreatePersonCommand(data, _))
      .mapTo[Done | ResultError]

    def updatePerson(id: String, data: PersonUpdate): Future[Done | ResultError] = personSharding
      .entityRefFor(
        PersonEntity.typeKey,
        id
      ).ask(PersonEntity.UpdatePersonCommand(data, _)).mapTo[Done | ResultError]

    def getPerson(id: String): Future[Person | ResultError] = personSharding
      .entityRefFor(PersonEntity.typeKey, id)
      .ask(PersonEntity.GetPersonCommand(_))
      .mapTo[Person | ResultError]
