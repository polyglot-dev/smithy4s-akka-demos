import munit.*
import cats.effect.*

import common.*

import domain.data.*

// @IgnoreSuite
class ComplexRepositoryGetImplTestSuite extends CatsEffectSuite with CommonRepositoryContext {

  val fixture1 =
    new Fixture[Unit]("fixture1") {

      import doobie.*
      import doobie.implicits.*

      def apply() = ()

      override def beforeEach(context: BeforeEach): Unit = {
        sql"""insert into advertiser (id, name) values (1, 'a');
              insert into advertiser (id, name) values (2, 'b');
              insert into advertiser (id, name) values (3, 'c');
              insert into campaign (id, name, advertiser_id) values (1, 'c', 1);
              insert into campaign (id, name, advertiser_id) values (2, 'd', 1);
              insert into campaign (id, name, advertiser_id) values (3, 'e', 2);
              insert into campaign (id, name, advertiser_id) values (4, 'f', 2);
              insert into campaign (id, name, advertiser_id) values (5, 'g', 2);
              insert into campaign (id, name, advertiser_id) values (6, 'h', 2);
          """.update.run.transact(xa).unsafeRunSync()
      }

      override def afterEach(context: AfterEach): Unit = {
        sql"delete from advertiser where true; delete from campaign where true".update.run.transact(xa).unsafeRunSync()
        sql"ALTER SEQUENCE person_id_seq RESTART WITH 1".update.run.transact(xa).unsafeRunSync()
      }

    }

  override def munitFixtures = List(fixture1)

  test("Get list of 2 advertisers") {

    repo.getAdvertisers(2).map(
      it =>
        assertEquals(it,
                     Right(value =
                       List(
                         Advertiser(
                           id = 1,
                           name = "a",
                           campaigns = List(
                             Campaign(
                               id = 1,
                               name = "c"
                             ),
                             Campaign(
                               id = 2,
                               name = "d"
                             )
                           )
                         ),
                         Advertiser(
                           id = 2,
                           name = "b",
                           campaigns = List(
                             Campaign(
                               id = 3,
                               name = "e"
                             ),
                             Campaign(
                               id = 4,
                               name = "f"
                             ),
                             Campaign(
                               id = 5,
                               name = "g"
                             ),
                             Campaign(
                               id = 6,
                               name = "h"
                             )
                           )
                         )
                       )
                     )
                    )
    )

  }

}
