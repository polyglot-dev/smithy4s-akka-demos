package common

import cats.effect.IO
import com.typesafe.config.{ Config, ConfigFactory }

import doobie.util.transactor.Transactor

import infrastructure.repositories.*
import infrastructure.http.services.*

trait CommonRepositoryContext {

  val repo = new AdvertiserRepositoryImpl(xa)

  val service = new AdvertiserServiceImpl(repo)

  lazy val xa = {

    val config: Config = ConfigFactory.defaultApplication().getConfig("app").resolve()

    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = config.getString("hikariTransactor.url"),
      user = config.getString("hikariTransactor.user"),
      password = config.getString("hikariTransactor.password"),
      logHandler = None
    )
  }

  lazy val xroot = {

    val config: Config = ConfigFactory.defaultApplication().getConfig("app").resolve()

    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = config.getString("hikariTransactor.url"),
      user = "duser",
      password = "dpass",
      logHandler = None
    )
  }

}
