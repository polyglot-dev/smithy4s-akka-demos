package main

import infrastructure.services.*
import Configs.*

import akka.actor.typed.ActorSystem
// import akka.actor.typed.{ ActorSystem as TypedActorSystem }
import akka.actor.CoordinatedShutdown
import akka.actor.ActorSystem as UntypedActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.Done
import scala.concurrent.duration.*
import scala.concurrent.Future

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

import akka.kafka.scaladsl.Consumer.DrainingControl

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory

import infrastructure.services.person.PersonService

import infrastructure.services.PersonServiceImpl

import infrastructure.grpc.GrpcApi

import akka.grpc.GrpcClientSettings

import akka.persistence.typed.PersistenceId
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.actor.typed.ActorRef

object DIConfigs:

    val configModule =
      // format: off
      new ConfigModuleDef:
          makeConfig[GrpcConfig]          ("ports.grpc")

          makeConfig[ServiceConfig]       ("service")
          makeConfig[LocalConfig]         ("local.config")

          makeConfig[PersonEntityConfig]("person")
          // format: on

          make[AppConfig].from(AppConfig(
            ConfigFactory
              .defaultApplication().getConfig("app").resolve()
          ))

class DI(config: LocalConfig):

    val clusterModule =
      new ModuleDef:
          make[PersonSharding]

    val servicesModule =
      new ModuleDef:

          // format: off
          make[PersonService]         .from[PersonServiceImpl]

          make[GrpcApi]

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
                          personProjection: PersonProjection,
                          personEntityConfig: PersonEntityConfig,
                          grpcConfig: GrpcConfig) =>
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
                                    )
                                  )
                              )
                            )

                            if config.first then
                                grpcApi.init().map(_.addToCoordinatedShutdown(hardTerminationDeadline =
                                  grpcConfig.hardTerminationDeadline
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
