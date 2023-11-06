package infrastructure

import akka.persistence.testkit.scaladsl.UnpersistentBehavior
import akka.persistence.typed.PersistenceId
import akka.Done
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import infrastructure.entities.PersonEntity
import infrastructure.entities.PersonEntity.*
import infrastructure.entities.person.Events.*
import infrastructure.entities.person.Commands.*
import infrastructure.entities.person.DataModel.*

import services.Configs.*

class AccountExampleUnpersistentDocSpec extends AnyWordSpecLike {

  "Person" must {
    "be updated with a new name" in {
      onAnExistingPerson {
        (testkit, eventProbe, snapshotProbe) =>
            testkit.runAsk[Done | util.ResultError](UpdatePersonCommand(PersonUpdate(Some("new name"), None, None), _))
              .expectReply(Done)

            eventProbe.expectPersisted(PersonUpdated(Some("new name"), None, None))

            testkit.runAsk[Person | util.ResultError](GetPersonCommand(_)).expectReply(Person("new name", None, None))
      }
    }
  }

  given config: PersonEntityConfig = PersonEntityConfig(EntityConfig(1, 1, 2.second, 2.second, 2.0),
                                                        ProjectionConfig(1, 2.second)
                                                       )

  private def onAnExistingPerson: UnpersistentBehavior.EventSourced[Command, Event, Option[PersonEntity.State]] =
    UnpersistentBehavior.fromEventSourced(PersonEntity(PersistenceId("person", "1")),
                                          Some(PersonEntity.State("name", None, None))
                                         )

}
