package infrastructure
package http

import logstage.IzLogger

import scala.language.implicitConversions
// import TypesConversion.given
import _root_.infrastructure.internal.*
import types.*
import server.common.CommonHTTP

class HttpServerImpl(
                    service: AdvertiserService[IO],
                    logger: Option[IzLogger])
    extends ApiService[IO], CommonHTTP(), server.AdvertiserHttp(service, logger) {}
