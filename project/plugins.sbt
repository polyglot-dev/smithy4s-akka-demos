resolvers += "Akka library repository".at("https://repo.akka.io/maven")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")

addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.4.0")
addSbtPlugin("org.typelevel"           % "sbt-fs2-grpc"  % "2.7.11")

addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.3")

addSbtPlugin("com.github.sbt"            % "sbt-avro"      % "3.4.3")
libraryDependencies += "org.apache.avro" % "avro-compiler" % "1.11.3"

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.5.11")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

addDependencyTreePlugin

addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"
