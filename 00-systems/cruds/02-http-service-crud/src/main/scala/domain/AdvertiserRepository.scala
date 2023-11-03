package domain
package types

import data.*

trait AdvertiserRepository[F[_]] {
  def getPersonById(id: Long): F[Either[Throwable, Option[Person]]]
  def savePerson(p: Person): F[Either[Throwable, Long]]
  def updatePerson(p: PersonInfo, id: Long): F[Either[Throwable, Option[Person]]]
  def updatePerson2(p: PersonInfo, id: Long): F[Either[Throwable, Option[PersonInfo]]]
  def getAdvertisers(limit: Int): F[Either[Throwable, List[Advertiser]]]
}
