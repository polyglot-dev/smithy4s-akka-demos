package infrastructure
package services

import scala.concurrent.duration.FiniteDuration

object Configs:

    case class LocalConfig(
                          first: Boolean,
                          usingKubernetes: Boolean,
                          actorSystemName: String)

    case class ServiceConfig(
                            requestToActorsTimeout: FiniteDuration)

    case class HostConfig(
                         hostname: String,
                         port: Int)

    case class GrpcConfig(
                       host: HostConfig,
                       hardTerminationDeadline: FiniteDuration):
        export host.*

    case class EntityConfig(
                           snapshotNumberOfEvents: Int,
                           snapshotKeepNsnapshots: Int,
                           restartMinBackoff: FiniteDuration,
                           restartMaxBackoff: FiniteDuration,
                           restartRandomFactor: Double)

    case class PersonEntityConfig(conf: EntityConfig):
        export conf.*

    case class ReadSideGrpcServiceConfig(
                                      host: HostConfig):
        export host.*
