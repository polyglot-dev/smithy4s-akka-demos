package infrastructure
package http
package types

import _root_.infrastructure.internal.*

trait AdvertiserService[F[_]]:
    def getPersonById(id: String): F[Person]
    def createPerson(body: Person): F[Long]
    def updatePerson(body: PersonInfo, id: Long): F[PersonInfo]
