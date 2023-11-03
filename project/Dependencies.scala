import sbt._

object Dependencies {

// format: off
  object V {
    val cats                 = "2.9.0"
    val catsEffect           = "3.5.0"
    val organizeImports      = "0.6.0"
    val semanticDB           = "4.5.4"
    val logback              = "1.4.5"
    val postgress            = "42.2.24"
    val akka                 = "2.9.0"
    val akkaHttp             = "10.6.0"
    val cassandra            = "1.1.1"
    val akkaProjection       = "1.5.0"
    val hikaryCP             = "4.0.3"
    val akkaPersistenceR2dbc = "1.2.0"
    val doobie               = "1.0.0-RC4"
    val grpc                 = "1.56.0"
    val ducktape             = "0.1.10"
    val scalapbCommonProtos  = "2.9.6-0"
    val alpakka              = "7.0.0"
    val kafka                = "5.0.0"
    val izumi                = "1.1.0-M26"
    val akkaManagement       = "1.5.0"
    val http4s               = "0.23.18"
    val munitCatsEffect      = "2.0.0-M3"
    val circe                = "0.14.5"
  }

  object Libraries {
    val cats                       = "org.typelevel"      %% "cats-core"                     % V.cats
    val catsEffect                 = "org.typelevel"      %% "cats-effect"                   % V.catsEffect

    val akkaActor                  = "com.typesafe.akka"  %% "akka-actor-typed"              % V.akka
    val akkaCluster                = "com.typesafe.akka"  %% "akka-cluster-typed"            % V.akka
    val akkaClusterSharding        = "com.typesafe.akka"  %% "akka-cluster-sharding-typed"   % V.akka
    val akkaSlf4j                  = "com.typesafe.akka"  %% "akka-slf4j"                    % V.akka
    val akkaSerialization          = "com.typesafe.akka"  %% "akka-serialization-jackson"    % V.akka
    val akkaPersistence            = "com.typesafe.akka"  %% "akka-persistence-typed"        % V.akka
    val akkaStream                 = "com.typesafe.akka"  %% "akka-stream"                   % V.akka
    val akkaDiscovery              = "com.typesafe.akka"  %% "akka-discovery"                % V.akka

    val akkaMangmentCltrBootstrap  = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % V.akkaManagement
    val akkaMangementClusterHttp   = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % V.akkaManagement
    val akkaDiscoveryKubernetes    = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % V.akkaManagement

    val akkaPersistenceCassandra   = "com.typesafe.akka"  %% "akka-persistence-cassandra"    % V.cassandra
    val akkaPersistenceR2dbc       = "com.lightbend.akka" %% "akka-persistence-r2dbc"        % V.akkaPersistenceR2dbc
    val akkaProjectionR2dbc        = "com.lightbend.akka" %% "akka-projection-r2dbc"         % V.akkaProjection

    val akkaProjectionCore         = "com.lightbend.akka" %% "akka-projection-core"          % V.akkaProjection
    val akkaProjectionEventsourced = "com.lightbend.akka" %% "akka-projection-eventsourced"  % V.akkaProjection

    val akkaHttp                   = "com.typesafe.akka"  %% "akka-http"                     % V.akkaHttp
    val akkaHttpSprayJson          = "com.typesafe.akka"  %% "akka-http-spray-json"          % V.akkaHttp

    val alpakkaCsv                 = "com.lightbend.akka" %% "akka-stream-alpakka-csv"       % V.alpakka

    val kafka                      = "com.typesafe.akka" %% "akka-stream-kafka"               % V.kafka

    val postgresql                 = "org.postgresql"     %  "postgresql"                    % V.postgress

    val logback                    = "ch.qos.logback"     %  "logback-classic"               % V.logback

    val grpcNettyShaded            = "io.grpc"            %  "grpc-netty-shaded"              % scalapb.compiler.Version.grpcJavaVersion
    val grpc                       = "io.grpc"            %  "grpc-services"                  % V.grpc
    val doobieCore                 = "org.tpolecat"       %% "doobie-core"                    % V.doobie
    val doobiePostgres             = "org.tpolecat"       %% "doobie-postgres"                % V.doobie
    val doobieHikari               = "org.tpolecat"       %% "doobie-hikari"                  % V.doobie
    val ducktape                   = "io.github.arainko"  %% "ducktape"                       % V.ducktape

    val scalapbCommonProtos        = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % V.scalapbCommonProtos % "protobuf"

    val distageCore                = "io.7mind.izumi"     %% "distage-core"                   % V.izumi
    val distageConfig              = "io.7mind.izumi"     %% "distage-extension-config"       % V.izumi

    val izumiLogstage              = "io.7mind.izumi"     %% "logstage-core"                  % V.izumi
    val izumiLogstageAdapterSlf4j  = "io.7mind.izumi"     %% "logstage-adapter-slf4j"         % V.izumi
    val izumiLogstageCirce         = "io.7mind.izumi"     %% "logstage-rendering-circe"       % V.izumi
    val izumiDistageExtension      = "io.7mind.izumi"     %% "distage-extension-logstage"     % V.izumi
    val izumiLogstageSinkSlf4j     = "io.7mind.izumi"     %% "logstage-sink-slf4j"            % V.izumi
    val http4s                     = "org.http4s"         %% "http4s-ember-server"            % V.http4s

    val munitCatsEffect            = "org.typelevel"      %% "munit-cats-effect"              % V.munitCatsEffect % Test
    val circeGeneric               = "io.circe"           %% "circe-generic"                  % V.circe
    val postgresCirce              = "org.tpolecat"       %% "doobie-postgres-circe"          % V.doobie

  }
  
  object Tracing{
    val httpServiceDependencies = Seq(
      Libraries.doobieCore,
      Libraries.doobiePostgres,
      Libraries.doobieHikari,
      Libraries.distageCore,
      Libraries.distageConfig,
      Libraries.izumiLogstage,
      Libraries.izumiLogstageAdapterSlf4j,
      Libraries.izumiLogstageCirce,
      Libraries.izumiDistageExtension,
      Libraries.izumiLogstageSinkSlf4j,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.ducktape,
      Libraries.http4s,
      "io.opentelemetry"                 % "opentelemetry-api"           % "1.31.0",
      "io.opentelemetry"                 % "opentelemetry-sdk"           % "1.31.0",
      "io.opentelemetry"                 % "opentelemetry-exporter-otlp" % "1.31.0",
      "io.opentelemetry"                 % "opentelemetry-semconv"       % "1.30.1-alpha",
      "io.opentelemetry.instrumentation" % "opentelemetry-grpc-1.6"      % "1.31.0-alpha", // % "runtime"
    )
    
    val grpcAkkaDependencies = Seq(
        Libraries.akkaCluster,
        Libraries.akkaClusterSharding,
        Libraries.akkaDiscovery,
        Libraries.akkaSlf4j,
        Libraries.akkaPersistence,
        Libraries.akkaPersistenceCassandra,
        Libraries.akkaSerialization,
        Libraries.akkaProjectionCore,
        Libraries.akkaProjectionEventsourced,
        Libraries.akkaHttp,
        Libraries.postgresql,
        Libraries.logback,
        Libraries.akkaProjectionR2dbc,
        Libraries.akkaPersistenceR2dbc,
        Libraries.scalapbCommonProtos,
        Libraries.akkaHttpSprayJson,
        Libraries.akkaStream,
        Libraries.kafka,
        Libraries.distageCore,
        Libraries.distageConfig,
        Libraries.akkaMangmentCltrBootstrap,
        Libraries.akkaMangementClusterHttp,
        Libraries.akkaDiscoveryKubernetes,
      )
    
    val grpcFs2Dependencies = Seq(
      Libraries.grpc,
      Libraries.grpcNettyShaded,
      Libraries.scalapbCommonProtos,
    )
      
      
  }
  
  object Cruds{
    
    lazy val awsVersion = "2.21.0"

    val httpServiceDependencies = Seq(
      Libraries.doobieCore,
      Libraries.doobiePostgres,
      Libraries.doobieHikari,
      Libraries.distageCore,
      Libraries.distageConfig,
      Libraries.izumiLogstage,
      Libraries.izumiLogstageAdapterSlf4j,
      Libraries.izumiLogstageCirce,
      Libraries.izumiDistageExtension,
      Libraries.izumiLogstageSinkSlf4j,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.ducktape,
      Libraries.http4s,
      Libraries.munitCatsEffect,
      Libraries.circeGeneric,
      Libraries.postgresCirce,
      "software.amazon.awssdk"     % "s3"                  % awsVersion,
      "software.amazon.awssdk"     % "s3-transfer-manager" % awsVersion,
      "software.amazon.awssdk.crt" % "aws-crt"             % "0.27.3",
    )
      
  }

  object SmithyLibs{
    val interfaceLibsDependencies = Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % "0.18.3",
      "com.disneystreaming.smithy"    % "smithytranslate-traits" % "0.3.14",
    )
  }
  
  object Basic{
    val httpServiceDependencies = Seq(
      Libraries.doobieCore,
      Libraries.doobiePostgres,
      Libraries.doobieHikari,
      Libraries.distageCore,
      Libraries.distageConfig,
      Libraries.izumiLogstage,
      Libraries.izumiLogstageAdapterSlf4j,
      Libraries.izumiLogstageCirce,
      Libraries.izumiDistageExtension,
      Libraries.izumiLogstageSinkSlf4j,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.ducktape,
      Libraries.http4s,
      Libraries.munitCatsEffect,
      Libraries.circeGeneric,
    )
  }

// format: on

}
