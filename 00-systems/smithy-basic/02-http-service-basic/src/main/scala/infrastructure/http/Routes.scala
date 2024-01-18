package infrastructure
package http

import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dsl.io.*

import logstage.IzLogger

import smithy4s.hello.*
import types.*


import Converter.*


import cats.data._
import org.http4s.HttpRoutes
import cats.syntax.all._
import org.http4s.headers.{`Content-Type`, `User-Agent`}
import org.typelevel.ci.CIString

object Middleware {

  def withRequestInfo(
      routes: HttpRoutes[IO],
      local: IOLocal[Option[RequestInfo]]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request =>
      // val requestInfo = for {
      //   contentType <- request.headers.get[`Content-Type`].map(ct => s"${ct.mediaType.mainType}/${ct.mediaType.subType}")
      //   userAgent <- request.headers.get[`User-Agent`].map(_.product.toString)
      //   userId <- if (request.headers.headers.exists( key => key.name == CIString("userId"))) Some(Some("yo")) else Some(None) // (Header. CIString("UserId"))) None else Some("hola")  //.map( el => if (el.head.name == CIString("UserId")) Some(el.head.value) else None)
      //   } yield 
          
      val requestInfo = Some(RequestInfo(
        // "contentType",
        // "userAgent",
         request.headers.headers.find( key => key.name == CIString("userId")).map( el => el.value )
      ))
        
      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

}


class ServerRoutes(
               logger: Option[IzLogger]
               ):
    // private val mainRoutes: Resource[IO, HttpRoutes[IO]] = {
      
  def getAll(local: IOLocal[Option[RequestInfo]]): Resource[IO, HttpRoutes[IO]] = {
    val getRequestInfo: IO[RequestInfo] = local.get.flatMap {
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
    }
      SimpleRestJsonBuilder.routes(

      // HttpServerImpl(logger, getRequestInfo)
      HttpServerImpl2(logger, getRequestInfo).transform(Converter.toIO)

    )
      .mapErrors(
        ex => ServiceUnavailableError("", "", ex.getMessage())
      )
    .resource
      .map { routes =>
        Middleware.withRequestInfo(routes, local)
      }
    } 
    // }

    private val healthCheck: HttpRoutes[IO] = HttpRoutes.of[IO]:
        // TODO: Check DB connection
        case GET -> Root / "alive" => Ok()
        case GET -> Root / "ready" => Ok()

    // val all: Resource[IO, HttpRoutes[IO]] = mainRoutes.map(_ <+> healthCheck)
