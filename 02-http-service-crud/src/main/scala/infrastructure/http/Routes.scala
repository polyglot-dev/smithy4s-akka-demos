package infrastructure
package http

import java.net.URI
import software.amazon.awssdk.regions.Region

import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.transfer.s3.model.CompletedUpload

import java.io.ByteArrayOutputStream
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.transfer.s3.model.Upload
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener

import java.util.UUID

import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dsl.io.*

import logstage.IzLogger

import types.*

// import software.amazon.awssdk.crt.Log

import infrastructure.internal.common.*

class ServerRoutes(
                service: AdvertiserService[IO],
                logger: Option[IzLogger]):

    private val mainRoutes: Resource[IO, HttpRoutes[IO]] =
      SimpleRestJsonBuilder.routes(HttpServerImpl(service, logger))
        // .mapErrors{
        //   case e: Throwable => ServiceUnavailableError(503, e.getMessage())
        // }
        .resource

    private val healthCheck: HttpRoutes[IO] = HttpRoutes.of[IO]:
        // TODO: Check DB connection
        case GET -> Root / "alive" => Ok()
        case GET -> Root / "ready" => Ok()

    import org.http4s.multipart.Part
    import org.http4s.EntityDecoder.multipart

    val serviceM: HttpRoutes[IO] = HttpRoutes.of {

      case req @ POST -> Root / "multipart" =>
        req.decodeWith(multipart[IO], strict = true) {
          response =>

              def filterFileTypes(part: Part[IO]): Boolean = part.headers.headers.exists(_.value.contains("filename"))

              val buffer: ByteArrayOutputStream = new ByteArrayOutputStream()

              // def store(part: Part[IO]): Stream[IO, PutObjectResponse] =
              def store(part: Part[IO]): Stream[IO, CompletedUpload] =
                for {
                  result <- part.body.through(
                              (p: Stream[IO, Byte]) => {

                                val xx: IO[List[ByteArrayOutputStream]] =
                                  p.fold(buffer) {
                                    case (a, b) => a.write(b); a
                                  }.compile.toList // .map(_.length)

                                val put: IO[CompletedUpload] = {
                                  val endpointOverride: URI = new URI("http://localhost:4566")
                                  val region: Region = Region.US_EAST_1

                                  // Execute this statement before constructing the SDK service client.
                                  // Log.initLoggingToStdout(Log.LogLevel.Trace)

                                  val s3AsyncClient: S3AsyncClient = S3AsyncClient.crtBuilder()
                                    .endpointOverride(endpointOverride)
                                    .forcePathStyle(true)
                                    // .credentialsProvider(DefaultCredentialsProvider.create())
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                      "112233445566",
                                      "test"
                                    )))
                                    .region(region)
                                    .targetThroughputInGbps(20.0)
                                    .minimumPartSizeInBytes(8 * 1024)
                                    .build()

                                  val transferManager: S3TransferManager = S3TransferManager
                                    .builder()
                                    .s3Client(s3AsyncClient)
                                    .build()

                                  val uploadRequest: UploadRequest = UploadRequest
                                    .builder()
                                    .putObjectRequest(
                                      req => req.bucket("sample-bucket").key(UUID.randomUUID().toString)
                                    )
                                    .addTransferListener(LoggingTransferListener.create())
                                    .requestBody(AsyncRequestBody.fromBytes(buffer.toByteArray()))
                                    .build()

                                  val upload: Upload = transferManager.upload(uploadRequest)

                                  IO.fromCompletableFuture(IO { upload.completionFuture() })

                                }

                                Stream.eval(xx *> put)
                              }
                            )
                } yield result

              // val stream = response.parts.filter(filterFileTypes).traverse(store)
              // stream.compile.toList.map(_.toString)
              val stream = response.parts.filter(filterFileTypes).traverse(store)
              // val res = stream.map((x: Vector[Int]) => s" ${x.toString} data")

              Ok(stream.map(
                _ => "done"
              ))
        }
    }

    val all: Resource[IO, HttpRoutes[IO]] = mainRoutes.map(_ <+> serviceM <+> healthCheck)
