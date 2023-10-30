import cats.Parallel
import sbt.dsl.LinterLevel.Ignore

import Dependencies.Libraries
import scala.util.Try
import scala.io.Source

ThisBuild / versionScheme := Some("early-semver")

lazy val autoImportSettings = Seq(
  scalacOptions += Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "scala.util.chaining",
    "scala.concurrent",
    "scala.concurrent.duration",
    "cats.implicits",
    "cats",
    "cats.effect",
    "fs2",
    "fs2.concurrent",
    "org.http4s",
  )
    .mkString(start = "-Yimports:", sep = ",", end = ""),
)

lazy val commonSettings = Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty,
  scalaVersion := "3.3.0",
  organization := "org",
  organizationName := "Demos",
  ThisBuild / evictionErrorLevel := Level.Info,
  dependencyOverrides ++= Seq(
  ),
  scalacOptions ++=
    Seq(
      "-explain",
      // "-Yexplicit-nulls",
      "-Ysafe-init",
      // "-Wunused:all",
      "-deprecation",
      "-feature",
      "-Yretain-trees",
    )
  // ) ++ Seq("-new-syntax", "-rewrite")
  // ) ++ Seq("-rewrite", "-indent")
  // ) ++ Seq("-source", "future-migration")
)

def sysPropOrDefault
  (propName: String, default: String)
  : String = Option(System.getProperty(propName)).getOrElse(default)

lazy val startupProject = sysPropOrDefault("active-app", "root") match{
  case "crud" => "http_rest_crud"
  case "rest" => "cats_Http"
  case "grpc" => "akka_gRPC"
  case _ => "root"
}
  
// This prepends the String you would type into the shell
lazy val startupTransition: State => State = { s: State =>
  s"project $startupProject" :: s
}

lazy val dummy = project
  .in(file("dummy"))
  .settings(
    name := "dummy",
    publish / skip := true
  )
  .settings(commonSettings)

lazy val root = project
  .in(file("."))
  .settings(
    Global / onLoad := {
      val old = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose old
    },
    name := "root",
    publish / skip := true
  )
  .settings(commonSettings)
  .dependsOn(
    if (sysPropOrDefault("active-app", "rest") == "rest")
      cats_Http
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "grpc") == "grpc")
      akka_gRPC
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "crud") == "crud")
      http_rest_crud
    else
      dummy
  )
// .aggregate(
//   if (sysPropOrDefault("active-app", "rest") == "rest") cats_Http else dummy
// )
// .aggregate(
//   if (sysPropOrDefault("active-app", "grpc") == "grpc") akka_gRPC else dummy
// )

lazy val `grpc-def` = project.in(file("00-apis/grpc"))
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
  )

lazy val generateSmithyFromOpenApi = taskKey[Unit]("Generate smithy sources")
lazy val smithyFilesPath = taskKey[String]("Smithy otuput path")
lazy val openApiFilesPath = taskKey[String]("OpenApi file path")
smithyFilesPath := "01-api-rest-crud/src/main/smithy"
openApiFilesPath := "00-apis/rest/"

generateSmithyFromOpenApi := {
  import scala.sys.process.*
  val s: TaskStreams = streams.value
  val shell: Seq[String] =
    if (sys.props("os.name").contains("Windows"))
      Seq("cmd", "/c")
    else
      Seq("bash", "-c")
  val clean: Seq[String] = shell :+ f"rm -Rf ${smithyFilesPath.value} || true"
  val createSmithyFolder: Seq[String] = shell :+ f"mkdir -p ${smithyFilesPath.value} || true"
  val smithyTranslate: Seq[String] =
    shell :+ f"smithytranslate openapi-to-smithy --input ${openApiFilesPath.value}* ${smithyFilesPath.value}"
  s.log.info("Generating smithy...")
  if (((clean #&& createSmithyFolder #&& smithyTranslate) !) == 0) {
    s.log.success("Smithy generation successful!")

    val dirContents = (shell :+ f"ls ${smithyFilesPath.value}/*.smithy").!! // .trim

    for (f <- dirContents.split("\n").toList) {
      if ((shell :+ f"""bin/tools/postprocess.scala -f "${f}" """).! == 0) {
        s.log.success(s"Smithy postprocess successful for ${f}")
      } else throw new IllegalStateException(s"Smithy postprocess failed for ${f}")
    }

    (shell :+ f"""cp -R bin/tools/support/* ${smithyFilesPath.value} """).!

  } else throw new IllegalStateException("Smithy generation failed!")
}

PB.protocVersion := "3.23.1"
lazy val awsVersion = "2.21.0"

lazy val rest_crud = project
  .in(file("01-api-rest-crud"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % smithy4sVersion.value,
      "com.disneystreaming.smithy"    % "smithytranslate-traits" % "0.3.13",
    ),
  )

lazy val `smithy4s-defs` = project
  .in(file("01-api-rest"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % smithy4sVersion.value,
      "com.disneystreaming.smithy"    % "smithytranslate-traits" % "0.3.13",
    ),
  )

lazy val api_grpc_akka = project
  .in(file("modules/01-api-grpc-akka"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(commonSettings)
  .settings(
    Compile / run / fork := true,
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.grpc",
    name := "api-grpc-akka",
    Compile / PB.protoSources ++= Seq(
      (`grpc-def` / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis"
  )

lazy val api_grpc_fs2 = project
  .in(file("modules/01-api-grpc-fs2"))
  .enablePlugins(Fs2Grpc)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.grpc",
    name := "api-grpc-fs2",
    Compile / PB.protoSources ++= Seq(
      (`grpc-def` / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    libraryDependencies ++= Seq(
      Libraries.grpc,
      Libraries.grpcNettyShaded,
      Libraries.scalapbCommonProtos,
    ),
  )

lazy val http_rest_crud = project
  .in(file("02-http-service-crud"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(rest_crud)
  .settings(commonSettings)
  .settings(autoImportSettings)
  .settings(
    Compile / run / fork := true,
    Test / parallelExecution := false, 
    name := "http-service-crud",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
    libraryDependencies ++= Seq(
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

    ),
  )

lazy val cats_Http = project
  .in(file("02-http-service"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(`smithy4s-defs`)
  .dependsOn(api_grpc_fs2)
  .settings(commonSettings)
  .settings(autoImportSettings)
  .settings(
    name := "http-service",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
    libraryDependencies ++= Seq(
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
    ),
  )

lazy val akka_gRPC = project
  .in(file("02-grpc-akka"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api_grpc_akka)
  .settings(
    name := "grpc-akka",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
  )
  .settings(commonSettings)
  .settings(
    Seq(
      libraryDependencies ++= Seq(
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
      ),
    )
  )

// fork := true

run / javaOptions ++= {
  val props = sys.props.toList
  props.filter(
    (p: (String, String)) => p._1 == "config.file"
  ).map {
    case (key, value) => s"""-D$key="$value""""
  }
}

TaskKey[Unit]("r") := (root / Compile / runMain)
  .toTask(" main.App")
  .value

TaskKey[Unit]("rc") := (root / Compile / runMain)
  .toTask(" mainCE.App")
  .value

reStart / mainClass := Some("main.App")
