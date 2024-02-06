package infrastructure
package services

import Configs.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import akka.Done
import akka.util.Timeout

import org.slf4j.LoggerFactory

import infrastructure.entities.person.DataModel.*
import infrastructure.entities.PersonEntity

import util.ResultError

import person.*

class PersonServiceImpl(personSharding: PersonSharding)
                       (
                         using sys: ActorSystem[Nothing],
                         config: ServiceConfig) extends PersonService:
    given ec: ExecutionContextExecutor = sys.executionContext
    given timeout: Timeout = config.requestToActorsTimeout
    private val logger = LoggerFactory.getLogger(getClass)

    def createPerson(id: String, data: Person): Future[Done | ResultError] =
      personSharding
        .entityRefFor(PersonEntity.typeKey, id)
        .ask(PersonEntity.CreatePersonCommand(data, _))
        .mapTo[Done | ResultError]

    def updatePerson(id: String, data: PersonUpdate): Future[Person | ResultError] =
        val ref = personSharding
          .entityRefFor(
            PersonEntity.typeKey,
            id
          )
        ref
          .ask(PersonEntity.UpdatePersonCommand(data, _)).mapTo[Done | ResultError]
          .flatMap {
            case Done           => ref.ask(PersonEntity.GetPersonCommand(_)).mapTo[Person | ResultError]
            case e: ResultError => Future { e }
          }

    def getPerson(id: String): Future[Person | ResultError] = personSharding
      .entityRefFor(PersonEntity.typeKey, id)
      .ask(PersonEntity.GetPersonCommand(_))
      .mapTo[Person | ResultError]

    def stop(id: String): Future[Done | ResultError] = personSharding
      .entityRefFor(PersonEntity.typeKey, id)
      .ask(PersonEntity.StopPersonCommand(_))
      .mapTo[Done | ResultError]
    def start(id: String): Future[Done | ResultError] = personSharding
      .entityRefFor(PersonEntity.typeKey, id)
      .ask(PersonEntity.StartPersonCommand(_))
      .mapTo[Done | ResultError]
