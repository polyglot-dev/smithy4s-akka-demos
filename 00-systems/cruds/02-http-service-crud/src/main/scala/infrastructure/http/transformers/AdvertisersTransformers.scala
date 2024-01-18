package infrastructure
package http
package transformers

import _root_.io.scalaland.chimney.dsl.*
import io.scalaland.chimney.{ partial, PartialTransformer, Transformer }

import _root_.infrastructure.internal as restEntities
import domain.data.*

object AdvertisersTransformers:

    given restPersonToPerson: PartialTransformer[restEntities.Person, Person] =
      PartialTransformer[restEntities.Person, Person] {
        (obj: restEntities.Person) =>
          {
            partial.Result.fromCatching {
              val address =
                obj.address match
                  case Some(add) =>
                    (add.address, add.fullAddress) match
                      case (Some(restEntities.Address(name, n)), _)                 => Some(AddressTypes.Address(name, n))
                      case (None, Some(restEntities.FullAddress(name, n, country))) =>
                        Some(AddressTypes.FullAddress(name, n, country))
                      case (None, None)                                             => None
                  case None      => None

              Person(obj.name, obj.town, address)
            }

          }
      }

    given personToRestPerson: PartialTransformer[Person, restEntities.Person] =
      PartialTransformer[Person, restEntities.Person] {
        (obj: Person) =>
          {
            partial.Result.fromCatching {
              val address: Option[restEntities.AddressTypes] =
                obj.address match
                  case Some(add: AddressTypes) =>
                    add match
                      case AddressTypes.Address(name, n)              =>
                        Some(
                          restEntities.AddressTypes(address =
                            Some(restEntities.Address(name, n))
                          )
                        )
                      case AddressTypes.FullAddress(name, n, country) =>
                        Some(
                          restEntities.AddressTypes(fullAddress =
                            Some(restEntities.FullAddress(name, n, country))
                          )
                        )
                  case None                    => None

              restEntities.Person(obj.name, obj.town, address)
            }

          }
      }
