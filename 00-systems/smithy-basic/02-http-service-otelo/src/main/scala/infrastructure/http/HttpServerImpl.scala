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

import cats.data.EitherT

type Result[A] = EitherT[IO, SmithyModelErrors, A]

type ResultI[A] = Either[SmithyModelErrors, IO[A]]

type Result2[A] = EitherT[IO, SmithyModelErrors2, IO[A]]

// class HttpServerImpl3(
//                        logger: Option[IzLogger],
//                        )
//     extends HelloWorldService[Result[IO]]{

//         def hello(name: String, town: Option[String]): Result[IO, Greeting]= ???
             
// }

case class RequestInfo(
  // contentType: String, userAgent: String, 
userId: Option[String])
    
class HttpServerImpl2(

                      logger: org.slf4j.Logger,
                      //  logger: Option[IzLogger],
                       requestInfo: IO[RequestInfo],
                       )
    extends HelloWorldService[Result]{

        def hello(name: String, town: Option[String]): Result[Greeting]= 
        
          logger.info(s"Hello $name from $town!    \"SSN\":\"34242343\"")
        
          EitherT(IO{Right(Greeting(s"Hello $name from $town! (Result)"))})

          val response = requestInfo.flatMap { (reqInfo: RequestInfo) =>
                IO.pure(Right(Greeting(s"Hello $name from $town! (IO)")))
          }
          
          EitherT(response)
             
}

object Converter:
  val toIO: PolyFunction[Result, IO] = new PolyFunction[Result, IO]{
    def apply[A](result: Result[A]): IO[A] = {
      
      result.foldF(
        error => IO.raiseError(error),
        value => IO{value}
      )

      // result match {
      //   case Left(error) => IO.raiseError(error)
      //   case Right(value) => value //.handleErrorWith(server.common.errorHandler)
      // }
    }
  }

class HttpServerImpl(
                       logger: Option[IzLogger],
                       requestInfo: IO[RequestInfo],
                       )
    extends HelloWorldService[IO], CommonHTTP(logger){

        def hello(name: String, town: Option[String]): IO[Greeting] = 

          logger.foreach(_.info(s"Hello $name from $town! (IO)"))

          // val response = IO.pure{Greeting(s"Hello $name from $town! (IO)")}

          val response = requestInfo.flatMap { (reqInfo: RequestInfo) =>
            IO.println("REQUEST_INFO: " + reqInfo)
              .as(Greeting(s"Hello $name from $town! (IO)"))
          }

          response.handleErrorWith(
            errorHandler
          )
}
