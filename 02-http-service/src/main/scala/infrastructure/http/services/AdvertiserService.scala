package infrastructure
package http
package services

import logstage.IzLogger
import domain.types.*
import types.*
import io.github.arainko.ducktape.*

class AdvertiserServiceImpl(
   repo: AdvertiserRepository[IO], logger: Option[IzLogger]=None
) extends AdvertiserService[IO]{
  
}
