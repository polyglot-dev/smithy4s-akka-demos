package infrastructure
package resources

import main.Configs.*

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import cats.*
import cats.effect.*
import fs2.grpc.syntax.all.*

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry

class GrpcClientToWritesideResource(opentelemetry: OpenTelemetry)(using config: GrpcClientConfig):

    val resource: Resource[IO, ManagedChannel] = {
      val grpcTelemetry: GrpcTelemetry = GrpcTelemetry.create(opentelemetry)
      NettyChannelBuilder
      .forAddress(config.hostname, config.port)
      .intercept(grpcTelemetry.newClientInterceptor())
      .usePlaintext()
      .resource[IO]
    }
