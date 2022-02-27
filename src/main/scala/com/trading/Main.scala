package com.trading

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.cluster.typed.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.trading.exchange.{Command, ExchangeRate}
import com.trading.server.HttpServer
import com.trading.trader.TraderPurchases
import com.typesafe.config.ConfigFactory

object Main extends App {

  val config = ConfigFactory.load()
  implicit val system = ActorSystem[Nothing](Behaviors.empty, "ClusterSystem", config)

  val TypeKey = EntityTypeKey[Command]("Exchanger")
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ExchangeRate.init(system)
  TraderPurchases.init(system)

  val httpInterface = config.getString("http.interface")
  val httpPort = config.getInt("http.port")

  HttpServer.start(httpInterface, httpPort)
}