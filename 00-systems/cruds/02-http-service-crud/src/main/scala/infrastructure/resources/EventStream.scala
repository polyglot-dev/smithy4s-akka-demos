package infrastructure.resources

import fs2.kafka.*
import fs2.kafka.KafkaProducer.*
import cats.effect.std.*
import fs2.*
import cats.effect.*
import scala.concurrent.duration.*

import cats.effect.kernel.Outcome.{ Canceled, Errored, Succeeded }

type Row = List[String]
type RowOrError = Either[Throwable, Row]

trait CSVHandle {
  def withRows(cb: RowOrError => Unit): Unit
}

trait Handle {
  val signal: IO[Ref[IO, IO[Deferred[IO, String]]]]
  def resetSignal(): Unit
}

class HandleImpl extends Handle {
  val signal: IO[Ref[IO, IO[Deferred[IO, String]]]] = IO.ref(IO.deferred[String])

  def resetSignal(): Unit = {
    signal.flatMap {
      psignal =>
        psignal.set(IO.deferred[String])
    }
  }

}

import logstage.IzLogger

class EventStream(
                 ch: IO[Channel[IO, String]],
                 logger: Option[IzLogger] = None) {

  val producerSettings = ProducerSettings[IO, String, String]
    .withBootstrapServers("localhost:19092")

  def init()(implicit S: Async[IO]): Stream[IO, Any] = {
    Stream.eval(ch).flatMap {
      channel =>
        channel.stream
          .evalTap(
            e => IO.println(s"Element: $e")
          )
          .map {
            value =>
                logger.foreach(_.error(s"EventStream =>>>>>>>>>>>> $value"))
                val record = ProducerRecord("topic", "key", value)
                ProducerRecords.one(record)
          }
          .through(KafkaProducer.pipe(producerSettings))
          .drain
    }
  }

}

// class EventStream(outSideQueue: Stream[IO, String]) {

//   val producerSettings =
//          ProducerSettings[IO, String, String]
//            .withBootstrapServers("localhost:19092")

//   def enqueue(q: Queue[IO, String]): Stream[IO, Unit] = outSideQueue.through(q.enqueue)

//   def deqWithKafka(q: Queue[IO,String], kafkaProducer: KafkaProducer[IO, String, String]): Stream[IO, Unit] =
//     q.dequeue
//       .map{
//         organisationEvent =>
//           val record =  ProducerRecord("topicName", None, organisationEvent)
//           ProducerRecords.one(record)
//       }
//       .evalMap{
//         record =>
//           kafkaProducer.produce(record)
//       }.as(())

//   def stream: Stream[IO, Unit] = {

//     val kafkaProducer: Stream[IO, KafkaProducer.Metrics[IO, String, String]] =
//       KafkaProducer.stream(producerSettings)

//     val queue: Stream[IO, Queue[IO, String]] =
//       fs2.Stream.eval(Queue.unbounded[IO, String])

//     kafkaProducer.flatMap{
//       producer =>
//         queue.flatMap{
//           q =>
//             val enqueueStream = enqueue(q)

//             val dequeueStream = deqWithKafka(q, producer)

//             dequeueStream.concurrently(enqueueStream)
//         }
//     }
//   }
// }
