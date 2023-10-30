#!/usr/bin/env -S scala-cli shebang -S 3

//> using scala 3.3.1
//> using toolkit latest
//> using dep com.typesafe:config:1.4.3
//> using dep org.apache.commons:commons-text:1.10.0
//> using dep com.monovore::decline::2.4.1

//> using resourceDir ./resources

import java.util.regex.*
import scala.io.Source

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue
import java.util.Map.Entry
import org.apache.commons.text.StringSubstitutor

import scala.jdk.CollectionConverters.*
import scala.jdk.FunctionConverters.*
import com.monovore.decline.*
import cats.implicits.*

val namespaceOpt = Opts.option[String]("namespace", short = "n", metavar = "namespace", help = "Set the namespace.")
val namespaceOptOrDefault = namespaceOpt.withDefault("infrastructure.internal")
val inputFileOpt = Opts.option[String]("file", short = "f", metavar = "filePath", help = "Set the file to transform.")

val transformOptions = (inputFileOpt, namespaceOptOrDefault).mapN { (f, nsp) =>
   println(s"LOG: transforming $f with namespace $nsp")
   (f, nsp)
}

val transforCommand = Command(
  name = "transform",
  header = "Transforms a smithy file.",
) {
  transformOptions
}

@main
def main(args: String*) = transforCommand.parse(args, sys.env) match

    case Left(errors) =>
      System.err.println(errors)
      sys.exit(1)

    case Right((fileName, namespaceName)) =>

      println(s"LOG: fileName: $fileName")

      val errorsPattern = Pattern.compile("""errors:\s*(\[(\s*\w+\s*)*])""")
      def errorsSubstitutor(mr: MatchResult): String =
        val conf = ConfigFactory.defaultApplication().getConfig("subs").resolve()
        val subs: Map[String, String] = conf.entrySet().toArray().map{
          case e: Entry[String, ConfigValue] => (e.getKey(), e.getValue().unwrapped().toString())
        }.toMap
        val parts = mr.group(1).split('\n').toList
        val allKeys = subs.keys.toList
        var msg = 
          parts.drop(1).dropRight(1)
               .map( e => {
                  allKeys.find( key => e.endsWith(key))
                  .map(subs(_)).get//OrElse("NotSupportedErrorLib")
        })
        .mkString("     ", "\n      ", "")
        s"""errors: [
          | $msg
          |    ] """.stripMargin

      val namespacePattern = Pattern.compile("""namespace\s*\w+""")
      def namespaceSubstitutor(mr: MatchResult): String =
        val conf = ConfigFactory .defaultApplication().getConfig("data").resolve()
        val text = conf.getString("headers")
        val valuesMap = Map(
          "namespace" -> namespaceName,
        )
        val sub = new StringSubstitutor(valuesMap.asJava)
        sub.replace(text)

      val servicePattern = Pattern.compile("""service""")
      def serviceSubstitutor(mr: MatchResult): String =
        "@simpleRestJson\nservice"

      val pipeline = List(
        (errorsPattern, errorsSubstitutor),
        (namespacePattern, namespaceSubstitutor),
        (servicePattern, serviceSubstitutor),
        )
      val str = Source.fromFile(fileName).getLines().mkString("\n")
      val res = pipeline.foldLeft(str){
        case (acc, (pattern, substitutor)) => 
          pattern.matcher(acc).replaceAll(substitutor.asJava)
      }

      import java.nio.file.{Paths, Files}
      import java.nio.charset.StandardCharsets

      Files.write(Paths.get(s"${fileName}.original"), str.getBytes(StandardCharsets.UTF_8))
      Files.write(Paths.get(fileName), res.getBytes(StandardCharsets.UTF_8))
