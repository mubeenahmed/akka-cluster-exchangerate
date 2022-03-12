package com.trading.exchange

import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils

class DBConfiguration(implicit val system: ActorSystem[_])
{
  val done = SchemaUtils.createIfNotExists()

}
