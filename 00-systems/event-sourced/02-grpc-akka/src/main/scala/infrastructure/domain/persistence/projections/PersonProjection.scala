package infrastructure
package entities
package person
package projections

import util.person.EventsTags

import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.SingletonActor
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.ExactlyOnceProjection
import akka.projection.scaladsl.SourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal

import akka.projection.ProjectionId
import akka.projection.r2dbc.scaladsl.R2dbcProjection
import services.Configs.PersonEntityConfig

class PersonProjection()(using system: ActorSystem[Nothing], config: PersonEntityConfig):

    val logger: Logger = LoggerFactory.getLogger(getClass)

    def init(): Unit =
      val projection = makeProjection("person", EventsTags.PersonCreateUpdated.value)
      ClusterSingleton(system).init(
        SingletonActor(
          ProjectionBehavior(projection),
          projection.projectionId.id
        )
          .withStopMessage(ProjectionBehavior.Stop)
      )
    end init

    private def makeProjection(projectionTag: String, targetTag: String) =
        given ec: ExecutionContext = system.executionContext
        val sourceProvider: SourceProvider[Offset, EventEnvelope[entities.person.Events.Event]] =
          EventSourcedProvider.eventsByTag[entities.person.Events.Event](
            system = system,
            readJournalPluginId = CassandraReadJournal.Identifier,
            tag = targetTag
          )
        R2dbcProjection
          .groupedWithin(
            ProjectionId(projectionTag, targetTag),
            settings = None,
            sourceProvider,
            handler = () => new PersonProjectionHandler()
          )
          .withGroup(groupAfterEnvelopes = config.projection.groupAfterEnvelopes,
                     groupAfterDuration = config.projection.groupAfterDuration
                    )
    end makeProjection
