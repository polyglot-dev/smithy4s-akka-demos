package integration
package serializers

import org.apache.avro.io.*
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificDatumReader

import org.integration.avro.ad.*
import java.io.ByteArrayOutputStream

// object BinaryAvroSerializer:

//     def serialize(ad: Advertiser): Array[Byte] = {
//       val writer: DatumWriter[Advertiser] = new SpecificDatumWriter(classOf[Advertiser])
//       val stream = new ByteArrayOutputStream()
//       val jsonEncoder: Encoder = EncoderFactory.get().binaryEncoder(stream, null)
//       writer.write(ad, jsonEncoder)
//       jsonEncoder.flush()
//       stream.toByteArray()
//     }

//     def deserialize(data: Array[Byte]) = {
//       val reader: DatumReader[Advertiser] = new SpecificDatumReader(classOf[Advertiser])
//       val decoder: Decoder = DecoderFactory.get().binaryDecoder(
//         data,
//         null
//       )
//       reader.read(null, decoder)
//     }

// object AdvertiserJsonAvroSerializer:

//     def serialize(ad: Advertiser): Array[Byte] = {
//       val writer: DatumWriter[Advertiser] = new SpecificDatumWriter(classOf[Advertiser])
//       val stream = new ByteArrayOutputStream()
//       val jsonEncoder: Encoder = EncoderFactory.get().jsonEncoder(Advertiser.getClassSchema(), stream)
//       writer.write(ad, jsonEncoder)
//       jsonEncoder.flush()
//       stream.toByteArray()
//     }

//     def deserialize(data: Array[Byte]): Advertiser = {
//       val reader: DatumReader[Advertiser] = new SpecificDatumReader(classOf[Advertiser])
//       val decoder: Decoder = DecoderFactory.get().jsonDecoder(
//         Advertiser.getClassSchema(),
//         new String(data),
//       )
//       reader.read(null, decoder)
//     }

// object AvroSerializer:

//     def serialize[T](ad: T)(using sdata: SerializerData[T]): Array[Byte] = {
//       val writer = new SpecificDatumWriter(sdata.className)
//       val stream = new ByteArrayOutputStream()
//       val jsonEncoder: Encoder = EncoderFactory.get().jsonEncoder(sdata.classSchema, stream)
//       writer.write(ad, jsonEncoder)
//       jsonEncoder.flush()
//       stream.toByteArray()
//     }

//     def deserialize[T](data: Array[Byte])(using sdata: SerializerData[T]): T = {
//       val reader: DatumReader[T] = new SpecificDatumReader(sdata.className)
//       val decoder: Decoder = DecoderFactory.get().jsonDecoder(
//         sdata.classSchema,
//         new String(data),
//       )
//       reader.read(null.asInstanceOf[T], decoder)
//     }

import java.nio.charset.StandardCharsets

trait SerializerData[T]:
    def className: Class[T]
    def classSchema: org.apache.avro.Schema

object JsonAvroSerializer:

    def serialize[T](ad: T)(using sdata: SerializerData[T]): String = {
      val writer = new SpecificDatumWriter(sdata.className)
      val stream = new ByteArrayOutputStream()
      val jsonEncoder: Encoder = EncoderFactory.get().jsonEncoder(sdata.classSchema, stream)
      writer.write(ad, jsonEncoder)
      jsonEncoder.flush()
      new String(stream.toByteArray(), StandardCharsets.UTF_8)
    }

    def deserialize[T](data: String)(using sdata: SerializerData[T]): T = {
      val reader: DatumReader[T] = new SpecificDatumReader(sdata.className)
      val decoder: Decoder = DecoderFactory.get().jsonDecoder(
        sdata.classSchema,
        data,
      )
      reader.read(null.asInstanceOf[T], decoder)
    }

object Dtos:

    enum Status(val id: String):
        case ACTIVE   extends Status("ACTIVE")
        case INACTIVE extends Status("INACTIVE")
        case PENDING  extends Status("PENDING")
        case DELETED  extends Status("DELETED")

    case class Advertiser(id: Long, status: Status)

import io.scalaland.chimney.dsl.*

@main
def main() =

    transparent inline given TransformerConfiguration[?] =
      TransformerConfiguration.default.enableDefaultValues.enableBeanSetters.enableBeanGetters.enableInheritedAccessors

    given adSerData: SerializerData[Advertiser] with
        def className = classOf[Advertiser]
        def classSchema = Advertiser.getClassSchema()

    val ad = Advertiser.newBuilder()
      .setId(1)
      .setStatus(Status.ACTIVE)
      .build()

    val dr: Dtos.Advertiser = ad.transformInto[Dtos.Advertiser]
    println(dr)

    val r = JsonAvroSerializer.serialize(ad)
    println(r)
    val adBack = JsonAvroSerializer.deserialize(r)
    println(adBack)

    println(adBack.transformInto[Dtos.Advertiser])

    val x = dr.transformInto[Advertiser]
    println(x)
