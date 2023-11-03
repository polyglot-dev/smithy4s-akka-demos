import sbt.dsl.LinterLevel.Ignore

import Dependencies._
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
  ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
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

lazy val startupProject =
  sysPropOrDefault("active-app", "root") match {
    case "crud-http" => "crudHttpService"
    case "tracing-http" => "tracingHttpService"
    case "tracing-grpc" => "tracingGrpcService"
    case "basic-http" => "basic_rest"
    case _      => "root"
  }

// This prepends the String you would type into the shell
lazy val startupTransition: State => State = {
  s:
      State =>
          s"project $startupProject" :: s
}

lazy val dummy = project
  .in(file("dummy"))
  .settings(commonSettings)
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
      startupTransition.compose(old)
    },
    name := "root",
    publish / skip := true
  )
  .settings(commonSettings)
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
    libraryDependencies ++= Tracing.httpServiceDependencies,
  )

lazy val tracingGrpcService = project
  .in(file("00-systems/tracing/02-grpc-akka"))
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
      libraryDependencies ++= Tracing.grpcAkkaDependencies,
    )
  )

lazy val `smithy4s-defs` = project
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

lazy val api_grpc_akka = project
  .in(file("artifacts/tracing/01-api-grpc-akka"))
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
      (`grpc-def` / Compile / baseDirectory).value / "grpc" / "v1",
    ),
    libraryDependencies ++= Tracing.grpcFs2Dependencies,
  )

lazy val `grpc-def` = project.in(file("00-apis/tracing/grpc"))
  .settings(commonSettings)
  .disablePlugins(AkkaGrpcPlugin)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
  )

// END ==== Tracing


lazy val generateSmithyFromOpenApi = taskKey[Unit]("Generate smithy sources")
lazy val smithyFilesPath = taskKey[String]("Smithy otuput path")
lazy val openApiFilesPath = taskKey[String]("OpenApi file path")
smithyFilesPath  := "00-systems/cruds/01-api-rest-crud/src/main/smithy"
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
      if ((shell :+ f"""bin/tools/postprocess.scala -f "${f}" """).! == 0) {
        s.log.success(s"Smithy postprocess successful for ${f}")
      } else
        throw new IllegalStateException(s"Smithy postprocess failed for ${f}")
    }

    (shell :+ f"""cp -R bin/tools/support/* ${smithyFilesPath.value} """).!

  } else
    throw new IllegalStateException("Smithy generation failed!")
}

PB.protocVersion := "3.23.1"

// REST CRUD demo
lazy val rest_crud = project
  .in(file("00-systems/cruds/01-api-rest-crud"))
  .enablePlugins(Smithy4sCodegenPlugin)
  // .disablePlugins(AkkaGrpcPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest",
    libraryDependencies ++= SmithyLibs.interfaceLibsDependencies,

    /*

    libraryDependencies ++= Seq(
        //  "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % "0.17.14",
         "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % "0.18.3",
        //  "com.disneystreaming.smithy4s" %% "smithy4s-http4s"        % smithy4sVersion.value,
         "com.disneystreaming.smithy"    % "smithytranslate-traits" % "0.3.14",
    ),
    
    */
  )

lazy val crudHttpService = project
  .in(file("00-systems/cruds/02-http-service-crud"))
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
    libraryDependencies ++= Cruds.httpServiceDependencies,
  )
  

// END ==== REST CRUD demo


// Smithy Basic demo
lazy val `basic-smithy4s-defs` = project
  .in(file("00-systems/smithy-basic/01-api-rest-basic"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .disablePlugins(AkkaGrpcPlugin)
  .settings(commonSettings)
  .settings(
    version := Try(Source.fromFile((Compile / baseDirectory).value / "version").getLines.mkString).getOrElse(
      "0.1.0-SNAPSHOT"
    ),
    name := "api-rest-basic",
    libraryDependencies ++= SmithyLibs.interfaceLibsDependencies,
  )
  
lazy val basic_rest = project
  .in(file("00-systems/smithy-basic/02-http-service-basic"))
  .enablePlugins(JavaAppPackaging)
  .disablePlugins(AkkaGrpcPlugin)
  .dependsOn(`basic-smithy4s-defs`)
  .settings(commonSettings)
  .settings(autoImportSettings)
  .settings(
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


// END ==== Smithy Basic demo


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
