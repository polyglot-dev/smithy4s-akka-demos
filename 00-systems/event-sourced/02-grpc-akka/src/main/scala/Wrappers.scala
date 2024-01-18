package infrastructure
package services

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.actor.typed.ActorSystem

class PersonSharding(using sys: ActorSystem[Nothing]):
    val sharding: ClusterSharding = ClusterSharding(sys)
    export sharding.*
