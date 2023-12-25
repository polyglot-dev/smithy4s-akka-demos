package infrastructure
package grpc
package transformers

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.Transformer

object CommonTransformers:

    transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues

    // given optionStringToString: Transformer[Option[String], String] with
    //     def transform(in: Option[String]): String = in.getOrElse("")
