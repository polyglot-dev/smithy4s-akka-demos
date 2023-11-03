package infrastructure
package http
package server

import logstage.IzLogger
import scala.language.implicitConversions
// import TypesConversion.given
import _root_.infrastructure.internal.*

import cats.data.EitherT

import types.*

trait AdvertiserHttpController(
                            service: AdvertiserService[Result],
                            logger: Option[IzLogger]):

    def updateAdvertiserBasicInformation(id: Int, body: Option[PersonInfo])
        : Result[UpdateAdvertiserBasicInformation200] =
        logger.foreach(_.error(s"updating person of id ${id} with $body"))
        service.updatePerson(body.get, id).map(
          p =>
            UpdateAdvertiserBasicInformation200(p)
        )

    def createAdvertiser(body: Option[Person]): Result[CreateAdvertiser201] =
        logger.foreach(_.error(s"createAdvertiser: $body"))
        service.createPerson(body.get).map(
          id =>
            CreateAdvertiser201(AdvertiserBodyResponse(Some(id.toString())))
        )

    def getAdvertiserById(id: String): Result[GetAdvertiserById200] = service.getPersonById(id).map(
      p => GetAdvertiserById200(p)
    )

    def updateAdvertiserCategory(id: String, body: Option[Category]): Result[UpdateAdvertiserCategory200] =
        logger.foreach(_.error(s"updateAdvertiserCategory: $body"))
        EitherT(IO.pure(Right(UpdateAdvertiserCategory200(Advertiser(Some("cosa"))))))

    def setStatus(body: Option[Status]): Result[SetStatus200] = 
      EitherT(IO.pure(Right(SetStatus200(Status.created))))