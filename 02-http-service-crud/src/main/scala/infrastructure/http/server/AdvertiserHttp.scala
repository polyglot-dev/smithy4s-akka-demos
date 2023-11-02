package infrastructure
package http
package server

import logstage.IzLogger
import scala.language.implicitConversions
// import TypesConversion.given
import _root_.infrastructure.internal.*

import types.*
import server.common.CommonHTTP

trait AdvertiserHttp(
                  service: AdvertiserService[IO],
                  logger: Option[IzLogger]):
    this: CommonHTTP =>

    // def createAdvertiser(body: Option[AdvertiserCreateRequest]): IO[CreateAdvertiser201] = IO {
    //   CreateAdvertiser201(AdvertiserBodyResponse(Some("1234")))
    // }

    def updateAdvertiserBasicInformation(id: Int, body: Option[PersonInfo]): IO[UpdateAdvertiserBasicInformation200] =
        logger.foreach(_.error(s"updating person of id ${id} with $body"))

        val res = service.updatePerson(body.get, id).map(
          p =>
            UpdateAdvertiserBasicInformation200(p)
        )

        res.handleErrorWith(errorHandler)

    def createAdvertiser(body: Option[Person]): IO[CreateAdvertiser201] = {
      logger.foreach(_.error(s"createAdvertiser: $body"))

      // IO{CreateAdvertiser201(AdvertiserBodyResponse(Some("1234")))}

      val res = service.createPerson(body.get).map(
        id =>
          CreateAdvertiser201(AdvertiserBodyResponse(Some(id.toString())))
      )

      res.handleErrorWith(errorHandler)
    }

    def getAdvertiserById(id: String): IO[GetAdvertiserById200] =
        // val result: GetAdvertiserById200 = GetAdvertiserById200(Person(Some("jo"), Some("Madrid")))
        // return IO { result }

        // throw new RuntimeException("Not implemented")

        val res = service.getPersonById(id).map(
          p => GetAdvertiserById200(p)
        )

        res.handleErrorWith(errorHandler)

        // ???

    def updateAdvertiserCategory(id: String, body: Option[Category]): IO[UpdateAdvertiserCategory200] =
        logger.foreach(_.error(s"updateAdvertiserCategory: $body"))
        IO { UpdateAdvertiserCategory200(Advertiser(Some("cosa"))) }
