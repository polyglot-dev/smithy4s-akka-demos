package infrastructure
package http

import smithy4s.hello._

import logstage.IzLogger

import scala.language.implicitConversions

import io.github.arainko.ducktape.*

import types.*
import server.common.CommonHTTP

type SmithyModelErrors = NotFoundError | BadRequestError | ServiceUnavailableError
type SmithyModelErrors2 = NotFoundError | BadRequestError

import smithy4s.Transformation
import smithy4s.kinds.PolyFunction

type Result[A] = Either[SmithyModelErrors, IO[A]]
type Result2[A] = Either[SmithyModelErrors2, IO[A]]

trait PartHello(
                       logger: Option[IzLogger],
                       ):
        def hello(name: String, town: Option[String]): Result2[Greeting]= 
          logger.foreach(_.info(s"Hello $name from $town!"))
        
          Left(NotFoundError("404", "Not Found", "Not Found"))
          // Right(IO.pure{Greeting(s"Hello $name from $town!")})

class HttpServerImpl2(
                       logger: Option[IzLogger],
                       )
    extends HelloWorldService[Result], CommonHTTP(logger){

        def hello(name: String, town: Option[String]): Result2[Greeting]= 
          logger.foreach(_.info(s"Hello $name from $town!"))
        
          Left(NotFoundError("404", "Not Found", "Not Found"))
          // Right(IO.pure{Greeting(s"Hello $name from $town!")})
             
}

object Converter:
  val toIO: PolyFunction[Result, IO] = new PolyFunction[Result, IO]{
    def apply[A](result: Result[A]): IO[A] = {
      result match {
        case Left(error) => IO.raiseError(error)
        case Right(value) => value
      }
    }
  }

class HttpServerImpl(
                       logger: Option[IzLogger],
                       )
    extends HelloWorldService[IO], CommonHTTP(logger){

        def hello(name: String, town: Option[String]): IO[Greeting] = 

          logger.foreach(_.info(s"Hello $name from $town!"))

          val response = IO.pure{Greeting(s"Hello $name from $town!")}
             
          response.handleErrorWith(
            errorHandler
          )
}
