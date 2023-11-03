package infrastructure
package resources

import main.Configs.*

import doobie.hikari.HikariTransactor

class PostgresResource(ec: ExecutionContext, config: DBConfig):

    val resource: Resource[IO, HikariTransactor[IO]] = HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      config.url,
      config.user,
      config.password,
      ec
    )
