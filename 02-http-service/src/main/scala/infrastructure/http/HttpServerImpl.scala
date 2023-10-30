package infrastructure
package http

import smithy4s.hello._

import logstage.IzLogger

import scala.language.implicitConversions

import io.github.arainko.ducktape.*

import io.grpc.ManagedChannel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

import types.*
import server.common.CommonHTTP

import example.myapp.helloworld.grpc.service.*

trait PartHTTP(
                channel: ManagedChannel, logger: Option[IzLogger], tracer: Tracer):
    this: CommonHTTP =>
        def hello(name: String, town: Option[String]): IO[Greeting] = 

          logger.foreach(_.info(s"Hello $name from $town!"))

          val span: Span  = tracer.spanBuilder("Start my wonderful use case").startSpan()
          span.addEvent("Event 0")
          val response = IO:
                          GreeterServiceGrpc.GreeterServiceBlockingStub(channel).sayHello(HelloRequest(name))
                                                                                .to[Greeting]
          span.addEvent("Event 1")
          span.end()
          response.handleErrorWith(
            errorHandler
          )

class HttpServerImpl(
                       service: AdvertiserService[IO], logger: Option[IzLogger],
                       channel: ManagedChannel, tracer: Tracer
                       )
    extends HelloWorldService[IO], CommonHTTP(logger), PartHTTP(channel, logger, tracer) {

  override def updateCategory(body: Option[Category]): IO[Response] = IO.pure(Response())

}
