package infrastructure
package resources

import http.RequestInfo

import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import main.Configs.*
import types.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server
import javax.net.ssl.SSLContext

import java.nio.file.Paths
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

import fs2.io.net.tls.*
import fs2._
import fs2.io.net._
import fs2.io.net.tls._
import org.http4s._
import org.http4s.dsl._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

def loadContextFromClasspath[F[_]](keystorePassword: String, keyManagerPass: String)(implicit
    F: Sync[F]
): F[SSLContext] =
  F.delay {
    val ksStream = this.getClass.getResourceAsStream("/server.jks")
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, keystorePassword.toCharArray)
    ksStream.close()

    val kmf = KeyManagerFactory.getInstance(
      Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
        .getOrElse(KeyManagerFactory.getDefaultAlgorithm)
    )

    kmf.init(ks, keyManagerPass.toCharArray)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)

    context
  }

class SSLResource[IO[_]: Async: Network]{
  def resource: Resource[IO, TLSContext[IO]] = for {
          sslContext <- Resource.eval(
            loadContextFromClasspath("changeit", "changeit")
          )
          tlsContext = Network[IO].tlsContext.fromSSLContext(sslContext)
          
  } yield tlsContext
}

class HttpServerResource(
                           tlsResource: Resource[IO, TLSContext[IO]],
                           logger: IzLogger,
                           )(using
                           config: HttpServerConfig):

    def resource(local: IOLocal[Option[RequestInfo]]): Resource[IO, Server] = 
      
      ServerRoutes(Some(logger)).getAll(local)
      .flatMap:
          routes =>
                for {
                      tlsContext <- tlsResource
                      res <- EmberServerBuilder
                        .default[IO]
                        .withTLS(tlsContext, TLSParameters.Default)
                        .withHttp2
                        .withHost(host"0.0.0.0")
                        .withPort(Port.fromInt(config.port).get)
                        .withHttpApp(routes.orNotFound)
                        .build
                } yield res
              // EmberServerBuilder
              //   .default[IO]
              //   .withHost(host"0.0.0.0")
              //   .withPort(Port.fromInt(config.port).get)
              //   .withHttpApp(routes.orNotFound)
              //   .build
