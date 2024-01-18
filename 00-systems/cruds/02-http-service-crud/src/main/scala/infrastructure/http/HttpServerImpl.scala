package infrastructure
package http

import logstage.IzLogger

import scala.language.implicitConversions
// import TypesConversion.given
import _root_.infrastructure.internal.*
import types.*

class HttpServerImpl(
                    service: AdvertiserService[Result],
                    logger: Option[IzLogger])
    extends ApiService[Result], server.AdvertiserHttpController(service, logger) {}
