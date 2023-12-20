package infrastructure
package http

import cats.data.EitherT
import scala.util.{ Failure, Success, Try }

import ErrorsBuilder.*

object EitherTProducerTypesConversion:

    extension [T](self: ServiceError)
      def toResult: EitherT[IO, ServiceError, T] = EitherT(IO.pure(Left(self)))

object TypesConversion:

    extension [T](self: IO[Either[ServiceError, T]])
      def toResult: EitherT[IO, ServiceError, T] = EitherT(self)

    extension [T](self: ServiceError)
      def toResult: IO[Either[ServiceError, T]] = IO.pure(Left(self))

    def convertId(idValue: String): Either[ServiceError, Long] =
      Try(idValue.trim.toLong) match
        case Success(value) => Right(value)
        case Failure(ex)    => Left(badRequestError(s"Invalid id: ${idValue}"))

    given dateFromStdToSmithy: Conversion[java.util.Date, smithy4s.Timestamp] with

        def apply(self: java.util.Date): smithy4s.Timestamp =
            val seconds = self.getTime() / 1000
            val nanos = (self.getTime() % 1000) * 1000000
            smithy4s.Timestamp(seconds, nanos.toInt)
