import munit.*
import cats.effect.*

import common.*

import domain.data.*

import infrastructure.internal.common.*
import infrastructure.http.*

class AdvertiserRepositoryGetImplTestSuite extends CatsEffectSuite with CommonRepositoryContext {

  val fixture1 =
    new Fixture[Unit]("fixture1") {

      import doobie.*
      import doobie.implicits.*

      def apply() = ()

      override def beforeEach(context: BeforeEach): Unit = {
        sql"insert into person (name, town) values ('c', 'k')".update.run.transact(xa).unsafeRunSync()
      }

      override def afterEach(context: AfterEach): Unit = {
        sql"delete from person where true".update.run.transact(xa).unsafeRunSync()
        sql"ALTER SEQUENCE person_id_seq RESTART WITH 1".update.run.transact(xroot).unsafeRunSync()
      }

    }

  override def munitFixtures = List(fixture1)

  test("Get a second person by id") {

    repo.getPersonById(1).map(
      it => assertEquals(it, Right(Some(Person(Some("c"), Some("k")))))
    )

  }

  test("Update a person 1") {

    repo.updatePerson(PersonInfo(Some("b"), Some("d")), 1).map(
      it => assertEquals(it, Right(Some(Person(Some("b"), Some("d")))))
    )

  }

  test("Update a person 1, b") {

    repo.updatePerson(PersonInfo(Some("b"), None), 1).map(
      it => assertEquals(it, Right(Some(Person(Some("b"), Some("k")))))
    )

  }

  test("Update a missing person") {

    repo.updatePerson(PersonInfo(Some("b"), None), 100).map(
      it => assertEquals(it, Right(None))
    )

  }

  test("Get a person by id") {
    repo.getPersonById(2).map(
      it => assertEquals(it, Right(None))
    )
  }

  intercept[NotFound] {
    service.getPersonById("4").unsafeRunSync()
  }

  interceptMessage[NotFound]("Data with id: 33 missing") {
    service.getPersonById("33").unsafeRunSync()
  }

}
