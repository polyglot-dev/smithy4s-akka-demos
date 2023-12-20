package infrastructure
package resources

import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import main.Configs.*
import http.types.*

import com.comcast.ip4s.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server
import http.Result

import fs2.kafka.*
import fs2.kafka.KafkaProducer.*

class KafkaProducerResource:

    val producerSettings = ProducerSettings[IO, String, String]
      .withBootstrapServers("localhost:19092")

    def resource: Resource[IO, PartitionsFor[IO, String, String]] = KafkaProducer.resource(
      producerSettings
    ) // .compile.drain
