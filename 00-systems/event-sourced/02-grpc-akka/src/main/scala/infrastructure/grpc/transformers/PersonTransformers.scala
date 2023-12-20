package infrastructure
package grpc
package transformers

import io.scalaland.chimney.Transformer

import api.eventSourced.grpc as protoMsgs
import infrastructure.entities.person.DataModel.*

object PersonTransformers:
// import transformers.CommonTransformers.given
    
    given protoStatusToStatus: Transformer[protoMsgs.Status, Status] with
        def transform(in: protoMsgs.Status): Status = 
            in match
                case protoMsgs.Status.active   => Status.active
                case _ => Status.inactive
