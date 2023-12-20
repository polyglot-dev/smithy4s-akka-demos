package infrastructure
package repositories

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import logstage.IzLogger

import domain.types.*

import domain.data.Person
import domain.data.PersonInfo
import domain.data.Advertiser as AdvertiserDTO
import domain.data.Campaign as CampaignDTO

// import io.github.arainko.ducktape.*
// import io.github.arainko.ducktape.to as duckTapeTo
// import io.github.arainko.ducktape.into as duckTapeInto
import _root_.io.scalaland.chimney.dsl.*
import _root_.io.scalaland.chimney.Transformer

// import io.github.arainko.ducktape.Field

import doobie.util.fragment.Fragment

case class Advertiser(id: Long, name: String)
case class Campaign(id: Long, name: String, advertiserId: Long)

import mappers.AdvertisersMappers.given

class AdvertiserRepositoryImpl(xa: Transactor[IO], logger: Option[IzLogger] = None)
    extends AdvertiserRepository[IO] {

  def getPersonById(id: Long): IO[Either[Throwable, Option[Person]]] = {
    logger.foreach(_.info(s"Getting a person: '$id'"))

    val stmt =
      sql"""
            SELECT name, town, address_at
            FROM person
            WHERE id = ${id}
            """
    stmt
      .query[Person]
      .option
      .transact(xa)
      .attempt

  }

  def savePerson(p: Person): IO[Either[Throwable, Long]] = {
    val stmt =
      sql"""
          insert into person (name, town, address_at) 
          values (${p.name}, ${p.town}, ${p.address})"""

    stmt
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
      .attempt
  }

  import Fragments.*

  def updatePerson(p: PersonInfo, id: Long): IO[Either[Throwable, Option[Person]]] = {
    val f1: Option[Fragment] = p.name.map(
      n => fr"name = $n"
    )
    val f2: Option[Fragment] = p.town.map(
      t => fr"town = $t"
    )
    val updates = Foldable[List].intercalate(List(f1, f2).flatten, fr", ")
    val stmt = fr"update person" ++ fr"set " ++ updates ++ fr"""where id = $id"""

    stmt
      .update
      .withGeneratedKeys[Person]("name", "town", "address_at")
      .take(1)
      .compile
      .toList
      .transact(xa)
      .attempt
      .map {
        case Left(value)  => Left(value)
        case Right(value) => Right(value.headOption)
      }
  }

  import cats.data.Validated.condNel
  import cats.data.NonEmptyList
  import cats.syntax.apply._
  import cats.syntax.either._
  import cats.effect._

  def updatePerson2(p: PersonInfo, id: Long): IO[Either[Throwable, Option[PersonInfo]]] = {
    val f1: Option[Fragment] = p.name.map(
      n => fr"name = $n"
    )
    val f2: Option[Fragment] = p.town.map(
      t => fr"town = $t"
    )
    val updates = Foldable[List].intercalate(List(f1, f2).flatten, fr", ")
    val stmt = fr"update person" ++ fr"set " ++ updates ++ fr"""where id = $id"""

    // (
    //   condNel({
    //     true
    //   }, (), "username is already in use"),
    //   condNel({
    //     true
    //   }, (), "username is already in use")
    // )
    // .mapN((_, _) => ())
    // .toEither
    def checkExists(): IO[Either[Throwable, Unit]] = {
      for {
        o1 <- getPersonById(id)
      } yield {

        o1 match {
          case Right(Some(_)) => Right(())
          case Right(None)    => Left(new Exception("Person not found"))
          case Left(ex)       => Left(ex)
        }

      }
    }

    def doUpdate(): IO[Either[Throwable, Unit]] = {
      val act =
        stmt
          .update
          .run
          .transact(xa)
          .attempt
      for {
        o1 <- act
      } yield {

        o1 match {
          case Right(0) => Left(new Exception("Person not updated"))
          case Right(_) => Right(())
          case Left(ex) => Left(ex)
        }

      }
    }

    // for {
    //   e <- checkExists()
    //   res <- e.fold(
    //     errors => IO.pure(errors.asLeft),
    //     _ =>
    //       stmt
    //         .update
    //         .withUniqueGeneratedKeys[PersonInfo]("name", "town")
    //         .transact(xa)
    //         .attempt
    //         .map{
    //           case Left(value) => Left(value)
    //           case Right(value) => Right(Some(value))
    //         }
    //   )
    // } yield res

    for {
      e <- checkExists()
      res2 <- e.fold(
                errors => IO.pure(errors.asLeft),
                _ =>
                  doUpdate()
              )
      res <- res2.fold(
               errors => IO.pure(errors.asLeft),
               _ =>
                 getPersonById(id)
                   .map {
                     case Left(value)  => Left(value)
                     case Right(value) => Right(value.map(_.transformInto[PersonInfo]))
                   }
             )
    } yield res
  }

  def getAdvertisers(limit: Int): IO[Either[Throwable, List[AdvertiserDTO]]] = {

    val stmtCampaigns =
      sql"""
          select id, name, advertiser_id as advertiserId from campaign where advertiser_id in (select id from advertiser limit $limit);
          """

    val stmtAdvertisers =
      sql"""
          select id, name from advertiser limit $limit;
          """

// """select a.id as advertiser_id, a.name as advertiser_name, c.id as campaign_id, c.name as campaign_name
// from campaign c left join advertiser a on a.id = c.advertiser_id
// offset 0 limit 2;"""

    val campaignsAttempt: IO[Either[Throwable, List[Campaign]]] =
      stmtCampaigns
        .query[Campaign]
        .to[List]
        .transact(xa)
        .attempt

    val advertisersAttempt: IO[Either[Throwable, List[Advertiser]]] =
      stmtAdvertisers
        .query[Advertiser]
        .to[List]
        .transact(xa)
        .attempt

    for {
      campaignsIO <- campaignsAttempt
      advertiserIO <- advertisersAttempt
    } yield for {
      campaigns <- campaignsIO
      advertisers <- advertiserIO
    } yield {
      val campaignsMap = campaigns.groupBy(_.advertiserId)
      advertisers.map {
        advertiser =>
            val campaigns: List[Campaign] = campaignsMap.getOrElse(advertiser.id, List.empty)
            advertiser
              .into[AdvertiserDTO]
              .withFieldComputed(ad => ad.campaigns,
                                        _ =>
                                          campaigns.map(
                                            campaign => campaign.transformInto[CampaignDTO]
                                          )
)
              .transform
      }
    }

  }

}
