addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.0")

addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.3.4")
addSbtPlugin("org.typelevel"           % "sbt-fs2-grpc"  % "2.7.4")

addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.17.14")

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.9")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

addDependencyTreePlugin

addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
