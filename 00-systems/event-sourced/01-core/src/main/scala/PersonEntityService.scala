package infrastructure
package services
package person

import util.ResultError

import scala.concurrent.Future
import akka.Done

import entities.person.DataModel.*

trait PersonService:
    def createPerson(id: String, data: Person): Future[Done | ResultError]
    def updatePerson(id: String, data: PersonUpdate): Future[Person | ResultError]
    def getPerson(id: String): Future[Person | ResultError]
    def stop(id: String): Future[Done | ResultError]
    def start(id: String): Future[Done | ResultError]
