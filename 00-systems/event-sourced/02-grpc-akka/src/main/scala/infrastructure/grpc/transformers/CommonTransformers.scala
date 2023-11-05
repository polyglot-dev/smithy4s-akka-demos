package infrastructure
package grpc
package transformers

import io.github.arainko.ducktape.*

import com.google.protobuf.ByteString

object CommonTransformers:

    given optionStringToString: Transformer[Option[String], String] with
        def transform(in: Option[String]): String = in.getOrElse("")
