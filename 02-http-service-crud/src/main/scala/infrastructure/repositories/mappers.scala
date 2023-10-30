package infrastructure
package repositories
package mappers

import doobie.*

import domain.data.Address

import io.circe.Encoder
import io.circe.Decoder
import io.circe.generic.semiauto.*
import doobie.postgres.circe.json.implicits.*

object AdvertisersMappers {
  given addressEncoder: Encoder[Address] = deriveEncoder[Address]
  given addressDecoder: Decoder[Address] = deriveDecoder[Address]

  given pgAddressDecoderGet: Get[Address] = pgDecoderGetT[Address]
  given pgAddressEncoderPut: Put[Address] = pgEncoderPutT[Address]

  given addressMeta: Meta[Address] = new Meta(pgAddressDecoderGet, pgAddressEncoderPut)

}
