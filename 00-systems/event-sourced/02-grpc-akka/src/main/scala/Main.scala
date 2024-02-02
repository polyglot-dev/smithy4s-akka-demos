package main

import infrastructure.services.*
import Configs.*
import infrastructure.entities.person

// import akka.actor.typed.{ ActorSystem as TypedActorSystem }

// import akka.actor.CoordinatedShutdown
// import akka.Done
// import scala.concurrent.Future
// import akka.kafka.scaladsl.Consumer.DrainingControl
// import akka.grpc.GrpcClientSettings

import akka.actor.typed.ActorSystem
import akka.actor.ActorSystem as UntypedActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.language.unsafeNulls

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.ClusterEvent.*
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.actor.typed.scaladsl.*

import akka.cluster.typed.*

import scala.concurrent.ExecutionContextExecutor

import infrastructure.entities.PersonEntity

import infrastructure.entities.person.projections.PersonProjection

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory

import infrastructure.services.person.PersonService

import infrastructure.services.PersonServiceImpl

import infrastructure.grpc.GrpcApi

import akka.persistence.typed.PersistenceId
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.actor.typed.ActorRef
import infrastructure.grpc.GrpcManagmentApi

// format: off
object DIConfigs:

    val configModule =
      new ConfigModuleDef:
          makeConfig[GrpcConfig]          ("ports.grpc")
          makeConfig[GrpcManagmentConfig] ("ports.grpc-management")

          makeConfig[ServiceConfig]       ("service")
          makeConfig[LocalConfig]         ("local.config")

          makeConfig[PersonEntityConfig]("person")

          make[AppConfig].from(AppConfig.provided(
            ConfigFactory
              .defaultApplication().getConfig("app").resolve()
          ))

@annotation.nowarn("msg=unused explicit parameter")
class DI(config: LocalConfig):

    val clusterModule =
      new ModuleDef:
          make[PersonSharding]

    val servicesModule =
      new ModuleDef:

          make[PersonService]         .from[PersonServiceImpl]

          make[GrpcApi]
          make[GrpcManagmentApi]

          make[PersonProjection]

// format: on
object RootBehaviorMainNode:

    def apply(): Behavior[Coordination.Command] = Behaviors.setup[Coordination.Command]:
        (ctx: ActorContext[Coordination.Command]) =>

            ctx.log.info("Starting Main Node")

            val ctxModule =
              new ModuleDef:
                  make[ActorSystem[?]].from(ctx.system)
                  make[ActorSystem[Nothing]].from(ctx.system)
                  make[ActorSystem[Coordination.Command]].from(
                    ctx.system.asInstanceOf[ActorSystem[Coordination.Command]]
                  )
                  make[ExecutionContextExecutor].from(ctx.system.executionContext)

            Injector().produceRun(DIConfigs.configModule):
                (config: LocalConfig) =>
                    val di = new DI(config)
                    import di.*

                    Injector().produceRun(ctxModule ++ servicesModule ++ clusterModule ++ DIConfigs.configModule):
                        (
                          typedActorSystem: ActorSystem[Coordination.Command],
                          executionContext: ExecutionContextExecutor,
                          personSharding: PersonSharding,
                          grpcApi: GrpcApi,
                          grpcManagmentApi: GrpcManagmentApi,
                          personProjection: PersonProjection,
                          personEntityConfig: PersonEntityConfig,
                          grpcConfig: GrpcConfig,
                          grpcManagmentConfig: GrpcManagmentConfig) =>
                            given UntypedActorSystem = typedActorSystem.toClassic
                            given ExecutionContextExecutor = executionContext

                            given PersonEntityConfig = personEntityConfig

                            infrastructure.Serializers.register(typedActorSystem)

                            val cluster = Cluster(typedActorSystem)
                            ctx.log.info(
                              "Started [" + ctx.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")"
                            )

                            if config.usingKubernetes then
                                AkkaManagement(typedActorSystem).start()
                                ClusterBootstrap(typedActorSystem).start()
                            else if config.first then
                                cluster.manager ! Join(cluster.selfMember.address)

                            val subscriber = ctx.spawnAnonymous(ClusterStateChanges(ctx.self))
                            cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

                            personSharding.init(
                              Entity(PersonEntity.typeKey)(createBehavior =
                                entityContext =>
                                  PersonEntity(
                                    PersistenceId(
                                      PersonEntity.typeKey.name,
                                      entityContext.entityId,
                                    ),
                                    entityContext.shard
                                  )
                              )
//                                .withStopMessage(person.Commands.GoodByeCommand)
                            )

                            if config.first then
                                grpcApi.init().map(_.addToCoordinatedShutdown(hardTerminationDeadline =
                                  grpcConfig.hardTerminationDeadline
                                ))
                                grpcManagmentApi.init().map(_.addToCoordinatedShutdown(hardTerminationDeadline =
                                  grpcManagmentConfig.hardTerminationDeadline
                                ))

                                personProjection.init()

            Behaviors.same

object Coordination:
    trait Command
    object Start extends Command

object ClusterStateChanges:

    def apply(owner: ActorRef[Coordination.Command]): Behavior[MemberEvent] = Behaviors.setup:
        ctx =>
            Behaviors.receiveMessage:
                case MemberJoined(member: Member) =>
                  ctx.log.info("MemberJoined: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberWeaklyUp(member: Member) =>
                  ctx.log.info("MemberWeaklyUp: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberUp(member: Member) =>
                  ctx.log.info("MemberUp: {}", member.uniqueAddress)
                  owner ! Coordination.Start
                  Behaviors.same

                case MemberLeft(member: Member) =>
                  ctx.log.info("MemberLeft: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberPreparingForShutdown(member: Member) =>
                  ctx.log.info("MemberPreparingForShutdown: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberReadyForShutdown(member: Member) =>
                  ctx.log.info("MemberReadyForShutdown: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberExited(member: Member) =>
                  ctx.log.info("MemberExited: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberDowned(member: Member) =>
                  ctx.log.info("MemberDowned: {}", member.uniqueAddress)
                  Behaviors.same

                case MemberRemoved(member: Member, previousStatus: MemberStatus) =>
                  ctx.log.info2(
                    "MemberRemoved: {}, previousStatus: {}",
                    member.uniqueAddress,
                    previousStatus
                  )
                  Behaviors.same

object App:

    def main(args: Array[String]): Unit =
        import DIConfigs.*

        Injector().produceRun(configModule):
            (
              config: LocalConfig,
            ) =>
                ActorSystem[Coordination.Command](
                  RootBehaviorMainNode(),
                  config.actorSystemName
                )

import io.circe.*, io.circe.syntax.*, io.circe.parser.*, io.circe.generic.auto.*
import campaigns.infrastructure.grpc as proto

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.Transformer
// import java.time.LocalDate
// import com.google.protobuf.util.JsonFormat
import scalapb.json4s.JsonFormat

case class LocalDate(year: Int, month: Int, day: Int)

object LocalDate:
    def of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)

case class Duration(startDate: LocalDate, endDate: LocalDate)
case class DurationReq(d: Duration)

given protoDateToDate: Transformer[proto.Date, LocalDate] with
    def transform(in: proto.Date): LocalDate = LocalDate.of(in.year, in.month, in.day)

object AppY:

    def main(args: Array[String]): Unit =
        var res = JsonFormat.toJsonString(
          proto.Duration(Some(proto.Date(2021, 1, 1)), Some(proto.Date(2021, 1, 1)))
        )
        var a = decode[Duration](res)
        println(res)
        println(a)
        res = JsonFormat.toJsonString(
          proto.Duration(Some(proto.Date(2021, 1, 1)), None)
        )
        a = decode[Duration](res)
        println(res)
        println(a)
        // .transformInto[Duration]
