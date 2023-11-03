package mainCE

import scala.util.{ Failure, Success, Try }

// IO: pure, delay, defer
// create failed effects
val aFailedCompute: IO[Int] = IO.delay(throw new RuntimeException("A FAILURE"))
val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper fail"))

// handle exceptions
val dealWithIt: IO[Int] = aFailure.handleErrorWith {
  case _: RuntimeException =>
    IO.delay {
      println("I'm still here")
      42
    }
  // add more cases
}

// turn into an Either
val effectAsEither: IO[Either[Throwable, Int]] = aFailure.attempt
// redeem: transform the failure and the success in one go
val resultAsString: IO[String] = aFailure.redeem(ex => s"FAIL: $ex", value => s"SUCCESS: $value")

// redeemWith
val resultAsEffect: IO[Unit] = aFailure.redeemWith(ex => IO(println(s"FAIL: $ex")),
                                                   value => IO(println(s"SUCCESS: $value"))
                                                  )

object App:

    def main(args: Array[String]): Unit =
        import cats.effect.unsafe.implicits.global
        // resultAsEffect.unsafeRunSync()
        println(effectAsEither.unsafeRunSync())
