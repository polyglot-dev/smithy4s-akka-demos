import sbt.dsl.LinterLevel.Ignore

import Dependencies._
import scala.util.Try
import scala.io.Source

ThisBuild / versionScheme := Some("early-semver")

lazy val autoImportSettingsCommon = Seq(
  "java.lang",
  "scala",
  "scala.Predef",
  "scala.util.chaining",
  "scala.concurrent",
  "scala.concurrent.duration",
  "scala.jdk.CollectionConverters",
  "scala.jdk.FunctionConverters",
  "cats.implicits",
  "cats",
  "cats.effect",
  "cats.effect.std",
)

lazy val autoImportSettingsFs2 =
  autoImportSettingsCommon ++ Seq(
    "fs2",
    "fs2.concurrent",
    "org.http4s",
  )

lazy val commonSettings = Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty,
  scalaVersion := "3.3.0",
  organization := "org",
  organizationName := "Demos",
  ThisBuild / evictionErrorLevel := Level.Info,
  dependencyOverrides ++= Seq(
  ),
  ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
  ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/"),
  scalacOptions ++=
    Seq(
      "-explain",
      "-Ysafe-init",
      "-deprecation",
      "-feature",
      "-Yretain-trees",
      "-Xmax-inlines",
      "50"
      // "-Yexplicit-nulls",
      // "-Wunused:all",
    )
  // ) ++ Seq("-new-syntax", "-rewrite")
  // ) ++ Seq("-rewrite", "-indent")
  // ) ++ Seq("-source", "future-migration")
)

def sysPropOrDefault
  (propName: String, default: String)
  : String = Option(System.getProperty(propName)).getOrElse(default)

// lazy val startupProject =
//   sysPropOrDefault("active-app", "root") match {
//     case "crud-http" => "crudHttpService"
//     case "tracing-http" => "tracingHttpService"
//     case "tracing-grpc" => "tracingGrpcService"
//     case "event-sourced-grpc" => "eventSourcedGrpcService"
//     case "basic-http" => "basic_rest"
//     case _      => "root"
//   }

// lazy val startupTransition: State => State = {
//   s:
//       State =>
//           s"project $startupProject" :: s
// }

lazy val dummy = project
  .in(file("artifacts/dummy"))
  .settings(commonSettings)
  .settings(
    name := "dummy",
    publish / skip := true
  )
  .settings(commonSettings)

lazy val root = project
  .in(file("."))
  .settings(
    // Global / onLoad := {
    //   val old = (Global / onLoad).value
    //   startupTransition.compose(old)
    // },
    name := "root",
    publish / skip := true
  )
  .settings(commonSettings)
  .aggregate(
    if (sysPropOrDefault("active-app", "tracing-rest") == "tracing-rest")
      tracingHttpService
    else
      dummy
  )
  .aggregate(
    if (sysPropOrDefault("active-app", "tracing-grpc") == "tracing-grpc")
      tracingGrpcService
    else
      dummy
  )
  .aggregate(
    if (sysPropOrDefault("active-app", "event-sourced-grpc") == "event-sourced-grpc")
      eventSourcedGrpcService
    else
      dummy
  )
  .aggregate(
    if (sysPropOrDefault("active-app", "crud-rest") == "crud-rest")
      crudHttpService
    else
      dummy
  )
  .aggregate(
    if (sysPropOrDefault("active-app", "basic-rest") == "basic-rest")
      basic_rest
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "tracing-rest") == "tracing-rest")
      tracingHttpService
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "tracing-grpc") == "tracing-grpc")
      tracingGrpcService
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "event-sourced-grpc") == "event-sourced-grpc")
      eventSourcedGrpcService
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "crud-rest") == "crud-rest")
      crudHttpService
    else
      dummy
  )
  .dependsOn(
    if (sysPropOrDefault("active-app", "basic-rest") == "basic-rest")
      basic_rest
    else
      dummy
  )

// Tracing
lazy val tracingHttpService = project
  .in(file("00-systems/tracing/02-http-service"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(smithy4s_defs)
  .dependsOn(api_grpc_fs2_tracing)
  .settings(commonSettings)
  .settings(
    scalacOptions += autoImportSettingsFs2.mkString(start = "-Yimports:", sep = ",", end = ""),
    name := "http-service",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
    libraryDependencies ++= Tracing.httpServiceDependencies,
  )

lazy val tracingGrpcService = project
  .in(file("00-systems/tracing/02-grpc-akka"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api_grpc_akka_tracing)
  .settings(commonSettings)
  .settings(
    scalacOptions += autoImportSettingsCommon.mkString(start = "-Yimports:", sep = ",", end = ""),
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
      libraryDependencies ++= Akka.grpcAkkaDependencies,
    )
  )

lazy val smithy4s_defs = project
  .in(file("00-systems/tracing/01-api-rest"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest",
    libraryDependencies ++= SmithyLibs.interfaceLibsDependencies,
  )

lazy val api_grpc_akka_tracing = project
  .in(file("artifacts/tracing/01-api-grpc-akka"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(commonSettings)
  .settings(
    Compile / run / fork := true,
    PB.protocVersion := "3.25.0",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.grpc",
    name := "api-grpc-akka",
    Compile / PB.protoSources ++= Seq(
      (grpc_def_tracing / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis"
  )

lazy val api_grpc_fs2_tracing = project
  .in(file("artifacts/tracing/01-api-grpc-fs2"))
  .enablePlugins(Fs2Grpc)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.grpc",
    name := "api-grpc-fs2",
    Compile / PB.protoSources ++= Seq(
      (grpc_def_tracing / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    libraryDependencies ++= Tracing.grpcFs2Dependencies,
  )

lazy val grpc_def_tracing = project.in(file("00-apis/tracing/grpc"))
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
  )

// END ==== Tracing

lazy val generateSmithyFromOpenApi = taskKey[Unit]("Generate smithy sources")
lazy val smithyFilesPath = taskKey[String]("Smithy otuput path")
lazy val openApiFilesPath = taskKey[String]("OpenApi file path")
smithyFilesPath := "00-systems/cruds/01-api-rest-crud/src/main/smithy"
openApiFilesPath := "00-apis/cruds/rest/"

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
      if ((shell :+ f"""postprocess.scala -f "${f}" """).! == 0) {
        s.log.success(s"Smithy postprocess successful for ${f}")
      } else
        throw new IllegalStateException(s"Smithy postprocess failed for ${f}")
    }

    (shell :+ f"""cp -R bin/tools/support/* ${smithyFilesPath.value} """).!

  } else
    throw new IllegalStateException("Smithy generation failed!")
}

// REST CRUD demo
lazy val crudHttpService = project
  .in(file("00-systems/cruds/02-http-service-crud"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(rest_crud)
  .dependsOn(avro_crud)
  .aggregate(rest_crud)
  .aggregate(avro_crud)
  .settings(commonSettings)
  .settings(
    scalacOptions += autoImportSettingsFs2.mkString(start = "-Yimports:", sep = ",", end = ""),
    Compile / run / fork := true,
    Test / parallelExecution := false,
    name := "http-service-crud",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
    libraryDependencies ++= Cruds.httpServiceDependencies ++ Seq(
      "com.github.fd4s" %% "fs2-kafka"              % "3.2.0",
       "io.confluent"    % "kafka-avro-serializer"  % "7.5.3",
      // "com.github.fd4s" %% "fs2-kafka-vulcan" % "3.2.0",
    ),
  )

lazy val avro_crud = project
  .in(file("00-systems/cruds/01-avro-crud"))
  .dependsOn(api_rest_avro_artifact)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-avro-kafka",
    libraryDependencies ++= KafkaSupportLibs.interfaceLibsDependencies,
  )

lazy val rest_crud = project
  .in(file("00-systems/cruds/01-api-rest-crud"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest",
    libraryDependencies ++= SmithyLibs.interfaceLibsDependencies,
  )

lazy val api_rest_avro_artifact = project
  .in(file("artifacts/cruds/00-api-kafka"))
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.kafka",
    name := "api-avro",
    Compile / avroSource := (Compile / baseDirectory).value / ".." / ".." / ".." / "00-apis" / "integration" / "cruds" / "00-api-kafka-to-publish" / "avro",
    libraryDependencies ++= KafkaSupportLibs.interfaceLibsDependencies,
  )

// END ==== REST CRUD demo

// Smithy Basic demo
lazy val basic_rest = project
  .in(file("00-systems/smithy-basic/02-http-service-basic"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(basic_smithy4s_defs)
  .settings(commonSettings)
  .settings(
    scalacOptions += autoImportSettingsFs2.mkString(start = "-Yimports:", sep = ",", end = ""),
    Compile / run / fork := true,
    Test / parallelExecution := false,
    name := "http-service-basic",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    Universal / packageName := name.value,
    Compile / mainClass := Some("main.App"),
    Compile / discoveredMainClasses := Seq(),
    libraryDependencies ++= Basic.httpServiceDependencies,
  )

lazy val basic_smithy4s_defs = project
  .in(file("00-systems/smithy-basic/01-api-rest-basic"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest-basic",
    libraryDependencies ++= SmithyLibs.interfaceLibsDependencies,
  )

// END ==== Smithy Basic demo

// Akka event sourced
lazy val eventSourcedGrpcService = project
  .in(file("00-systems/event-sourced/02-grpc-akka"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api_grpc_akka_event_sourced)
  .aggregate(api_grpc_akka_event_sourced)
  .dependsOn(core_event_sourced)
  .aggregate(core_event_sourced)
  .dependsOn(journal_events_akka_event_sourced)
  .aggregate(journal_events_akka_event_sourced)
  .settings(
    // Compile / run / fork := true,
    scalacOptions += autoImportSettingsCommon.mkString(start = "-Yimports:", sep = ",", end = ""),
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
      libraryDependencies ++= Akka.grpcAkkaDependencies ++ Seq(
        //  "com.google.protobuf" % "protobuf-java-util" % "3.25.0",
        "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.0",
      ) ++ Cruds.httpServiceDependencies,
    )
  )

lazy val core_event_sourced = project
  .in(file("00-systems/event-sourced/01-core"))
  .settings(
    name := "event-sourced-core",
  )
  .settings(commonSettings)
  .settings(
    Seq(
      libraryDependencies ++= Akka.coreDependencies,
    )
  )

lazy val base_akka_event_sourced = project
  .in(file("00-systems/event-sourced/00-base"))
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos",
    name := "base-akka",
  )

lazy val journal_events_akka_event_sourced = project
  .in(file("00-systems/event-sourced/01-events"))
  .settings(commonSettings)
  .dependsOn(base_akka_event_sourced)
  .aggregate(base_akka_event_sourced)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    PB.protocVersion := "3.25.0",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos",
    name := "journal-events-akka",
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  )

lazy val api_grpc_akka_event_sourced = project
  .in(file("artifacts/event-sourced/01-api-grpc-akka"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(commonSettings)
  .settings(
    // Compile / run / fork := true,
    PB.protocVersion := "3.25.0",
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    organization := "com.demos.grpc",
    name := "api-grpc-akka",
    Compile / PB.protoSources ++= Seq(
      (grpc_def_event_sourced / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis"
  )

lazy val grpc_def_event_sourced = project.in(file("00-apis/event-sourced"))
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
  )

// END ==== Akka event sourcing

fork := true

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
