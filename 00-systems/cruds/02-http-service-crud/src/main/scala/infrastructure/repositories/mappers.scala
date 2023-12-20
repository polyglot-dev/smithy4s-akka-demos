package infrastructure
package repositories
package mappers

import doobie.*

import domain.data.*

import io.circe.generic.semiauto.*
import doobie.postgres.circe.json.implicits.*

import io.circe.{ Decoder, Encoder }
import io.circe.Codec
import io.circe.Decoder
import io.circe.derivation.Configuration

object AdvertisersMappers {

  given Configuration = Configuration.default.withDiscriminator("type")
  given Codec[AddressTypes] = Codec.AsObject.derivedConfigured                          

  given pgAddressDecoderGet: Get[AddressTypes] = pgDecoderGetT[AddressTypes]
  given pgAddressEncoderPut: Put[AddressTypes] = pgEncoderPutT[AddressTypes]

  given addressMeta: Meta[AddressTypes] = new Meta(pgAddressDecoderGet, pgAddressEncoderPut)

}
