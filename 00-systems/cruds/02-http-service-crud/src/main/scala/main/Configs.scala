package main

object Configs:

    case class DBConfig(
                       url: String,
                       user: String,
                       password: String,
                       threadPoolSize: Int)

    case class HttpServerConfig(
                               port: Int)
