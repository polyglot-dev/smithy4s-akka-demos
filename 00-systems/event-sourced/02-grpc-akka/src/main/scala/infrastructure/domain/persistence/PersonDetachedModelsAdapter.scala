package infrastructure
package entities
package person

import akka.actor.ActorSystem
import akka.serialization.{ SerializationExtension, SerializerWithStringManifest }
import scala.concurrent.duration.*

import infrastructure.entities.person.DataModel as DomainModel

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.protobufs.*
// import io.scalaland.chimney.javacollections.*
import akka.persistence.typed.EventAdapter
import _root_.journal.infrastructure.entities.person.events as DataModel
import infrastructure.entities.person.Events as DomainEvents

import akka.persistence.typed.EventSeq

// import org.slf4j.{ Logger, LoggerFactory }

class PersonDetachedModelsAdapter extends EventAdapter[DomainEvents.Event, DataModel.Event] {

  // protected val logger: Logger = LoggerFactory.getLogger(getClass)

  override def manifest(event: DomainEvents.Event): String = ""

  transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues

  override def toJournal(event: DomainEvents.Event): DataModel.Event =
    event match {
      case DomainEvents.PersonCreated(name, town, address) =>
        // logger.info(s"PersonCreated toJournal: $name, $town, $address")
        DataModel.PersonCreated(name, town, address.map(_.transformInto[DataModel.Address]))
      case DomainEvents.PersonUpdated(town, address)       =>
        DataModel.PersonUpdated(town, address.map(_.transformInto[DataModel.Address]))
      case DomainEvents.PersonFixed(name, town, address)   =>
        DataModel.PersonFixed(name, town, address.map(_.transformInto[DataModel.Address]))
      case DomainEvents.Fixing(value)   =>
        DataModel.Fixing(value)
    }

  override def fromJournal(event: DataModel.Event, manifest: String): EventSeq[DomainEvents.Event] =
    event match {
      case DataModel.PersonCreated(name, town, address) =>
        // logger.info(s"PersonCreated fromJournal: $name, $town, $address")
        EventSeq.single(DomainEvents.PersonCreated(name, town, address.map(_.transformInto[DomainModel.Address])))
      case DataModel.PersonUpdated(town, address)       =>
        EventSeq.single(DomainEvents.PersonUpdated(town, address.map(_.transformInto[DomainModel.Address])))
      case DataModel.PersonFixed(name, town, address)   =>
        EventSeq.single(DomainEvents.PersonFixed(name, town, address.map(_.transformInto[DomainModel.Address])))
      case DataModel.Fixing(value)   =>
        EventSeq.single(DomainEvents.Fixing(value))
    }

}
