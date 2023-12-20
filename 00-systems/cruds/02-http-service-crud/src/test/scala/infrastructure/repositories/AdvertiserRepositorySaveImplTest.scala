import munit.*
import cats.effect.*

import common.*

import domain.data.*

// @IgnoreSuite
// class AdvertiserRepositorySaveImplTestSuite extends CatsEffectSuite with CommonRepositoryContext {

//   val fixture1 =
//     new Fixture[Unit]("fixture2") {

//       import doobie.*
//       import doobie.implicits.*

//       def apply() = ()

//       override def beforeEach(context: BeforeEach): Unit = {}

//       override def afterEach(context: AfterEach): Unit = {
//         sql"delete from person where true".update.run.transact(xa).unsafeRunSync()
//         sql"ALTER SEQUENCE person_id_seq RESTART WITH 1".update.run.transact(xroot).unsafeRunSync()
//       }

//     }

//   override def munitFixtures = List(fixture1)

//   test("Save person") {

//     repo.savePerson(Person(Some("c"), Some("c"))).map(
//       it =>
//         assert {
//           it match
//             case Right(x: Long) => true
//             case _              => false
//         }
//     )

//   }

// }
