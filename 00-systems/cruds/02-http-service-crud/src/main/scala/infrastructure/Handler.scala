package infrastructure

import fs2.concurrent.Channel

class Handler[T](var queue: Option[Channel[IO, T]] = None)

