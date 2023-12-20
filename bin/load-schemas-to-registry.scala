#!/usr/bin/env -S scala-cli shebang -S 3

//> using scala 3.3.1
//> using toolkit latest
//> using dep com.typesafe:config:1.4.3
//> using dep com.monovore::decline::2.4.1

//> using resourceDir ./resources

import java.util.regex.*
import scala.io.Source

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue

import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*
import com.monovore.decline.*
import cats.implicits.*

import sttp.client4.quick.*
import sttp.client4.Response

val registryBasePath = Opts.option[String](
  "registry-base",
  short = "r",
  metavar = "registry",
  help = "Set the registry base path."
)

val registryBasePathOrDefault = registryBasePath.withDefault("http://0.0.0.0:8081")

val subject = Opts.option[String](
  "schema-subject",
  short = "s",
  metavar = "subject",
  help = "Set the schema subject."
)

val dirPath = Opts.option[String](
  "path-of-schemas",
  short = "p",
  metavar = "path",
  help = "Set the schemas paths."
)

val appOptions = (registryBasePathOrDefault, subject, dirPath).mapN {
  (r, s, p) =>
      println(s"LOG: registry base path: $r")
      println(s"LOG: schema subject: $s")
      println(s"LOG: path of schemas: $p")
      (r, s, p)
}

val appCommand =
  Command(
    name = "load-schemas-to-registry",
    header = "Loads schemas to registry.",
  ) {
    appOptions
  }

@main
def main(args: String*) =
  appCommand.parse(args, sys.env) match

    case Left(errors) =>
      System.err.println(errors)
      sys.exit(1)

    case Right((registryBasePath, subjectName, dirPath)) =>
      val avroFilesDir = os.RelPath(dirPath)
      val files = os.list(os.pwd / avroFilesDir).filter(os.isFile)

      for
        file <- files
      do
          val content = os.read(file)
          val response = quickRequest
            .post(uri"http://0.0.0.0:8081/subjects/${subjectName}/versions")
            .header("Content-Type", "application/vnd.schemaregistry.v1+json")
            .body(content)
            .send()
