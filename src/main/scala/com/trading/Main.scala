package com.trading

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.cluster.typed.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.trading.exchange.{Command, DBConfiguration, ExchangeRate}
import com.trading.server.HttpServer
import com.trading.trader.TraderPurchases
import com.typesafe.config.ConfigFactory

object Main extends App {
  var nodeConfig = args(0)
  val config = ConfigFactory.load(nodeConfig)
  implicit val system = ActorSystem[Nothing](Behaviors.empty, "ExchangerService", config)

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  ExchangeRate.init(system)
  TraderPurchases.init(system)

  val dbConfiguration = new DBConfiguration()

  val httpInterface = config.getString("http.interface")
  val httpPort = config.getInt("http.port")

  HttpServer.start(httpInterface, httpPort)
}