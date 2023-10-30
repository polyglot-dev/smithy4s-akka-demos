package main

object Configs:

    case class DBConfig(
                       url: String,
                       user: String,
                       password: String,
                       threadPoolSize: Int)

    case class HttpServerConfig(
                                    port: Int)


    case class HostConfig(
                         hostname: String,
                         port: Int)
    
    case class GrpcClientConfig(
                             conf: HostConfig):
        export conf.*
