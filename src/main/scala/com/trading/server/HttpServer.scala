package com.trading.server

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.{Entity, EntityTypeKey}
import akka.http.scaladsl.Http
import com.trading.trader.{Command, TraderPurchases}

import scala.util.{Failure, Success}

object HttpServer {


  def start(interface: String, port: Int)
           (implicit actorSystem: ActorSystem[_]): Unit =
  {
    implicit val ec = actorSystem.executionContext

    val router = new Router()
    val binding = Http().newServerAt(interface, port)
      .bind(router.apply())

    binding.onComplete {
      case Success(value) =>
        actorSystem.log.info(s"$value: Server has bind to port: $port")
      case Failure(exception) =>
        actorSystem.log.error(s"Error: ${exception.getMessage}")
    }
  }

}
