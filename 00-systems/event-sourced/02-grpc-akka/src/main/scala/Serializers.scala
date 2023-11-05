package infrastructure

// import com.fasterxml.jackson.databind.SerializationFeature

object Serializers:

    import com.fasterxml.jackson.core.JsonGenerator
    import com.fasterxml.jackson.core.JsonParser
    import com.fasterxml.jackson.databind.DeserializationContext
    import com.fasterxml.jackson.databind.SerializerProvider
    import com.fasterxml.jackson.databind.deser.std.StdDeserializer
    import com.fasterxml.jackson.databind.ser.std.StdSerializer

    import com.fasterxml.jackson.databind.ObjectMapper
    import akka.serialization.jackson.JacksonObjectMapperProvider
    import com.fasterxml.jackson.databind.module.SimpleModule

    import util.TransportError
    import akka.actor.typed.{ ActorSystem => TypedActorSystem }

    def register(sys: TypedActorSystem[_]): ObjectMapper =
        val mapper: ObjectMapper = JacksonObjectMapperProvider(sys).getOrCreate("jackson-cbor", None)
        val mapperJson: ObjectMapper = JacksonObjectMapperProvider(sys).getOrCreate("jackson-json", None)
        val module: SimpleModule = new SimpleModule()

        // mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        // mapperJson.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        module.addSerializer(new TransportErrorSerializer())
        module.addDeserializer(classOf[util.TransportError], new TransportErrorDeserializer())

        module.addSerializer(new AkkaDoneSerializer())
        module.addDeserializer(classOf[akka.Done], new AkkaDoneDeserializer())

        mapper.registerModule(module)
        mapperJson.registerModule(module)

    class TransportErrorSerializer extends StdSerializer[TransportError](classOf[TransportError]):
        import TransportError._

        override def serialize(value: TransportError, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue =
              value match
                case NotFound            => "NF"
                case BadRequest          => "BR"
                case InternalServerError => "IS"
                case Unauthorized        => "UA"
                case Forbidden           => "FB"
                case Unknown             => "UN"
            gen.writeString(strValue)

    class TransportErrorDeserializer extends StdDeserializer[TransportError](classOf[TransportError]):
        import TransportError._

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): TransportError =
          p.getText match
            case "NF" => NotFound
            case "BR" => BadRequest
            case "IS" => InternalServerError
            case "UA" => Unauthorized
            case "FB" => Forbidden
            case "UN" => Unknown

    class AkkaDoneSerializer extends StdSerializer[akka.Done](classOf[akka.Done]):

        override def serialize(value: akka.Done, gen: JsonGenerator, provider: SerializerProvider): Unit =
            val strValue = "Done"
            gen.writeString(strValue)

    class AkkaDoneDeserializer extends StdDeserializer[akka.Done](classOf[akka.Done]):

        override def deserialize(p: JsonParser, ctxt: DeserializationContext): akka.Done = akka.Done
