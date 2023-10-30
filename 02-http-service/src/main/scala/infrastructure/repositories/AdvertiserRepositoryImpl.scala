package infrastructure
package repositories

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import logstage.IzLogger

import domain.types.*

import domain.data.Person

class AdvertiserRepositoryImpl(xa: Transactor[IO], logger: Option[IzLogger]=None) extends AdvertiserRepository[IO]{

  // override def getPersonById(name: String): IO[Option[Person]] ={
  //       logger.foreach(_.info(s"Getting a person with name: '$name'"))

  //       val query =
  //         sql"""
  //           SELECT name, town
  //           FROM person
  //           WHERE name = ${name}
  //           """
  //       query
  //         .query[Person]
  //         .option
  //         .transact(xa)

  // }


}
