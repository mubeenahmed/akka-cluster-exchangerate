package com.trading.server

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http

import scala.io.StdIn
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

    StdIn.readLine() // let it run until user presses return
      .binding
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => actorSystem.terminate()) // and shutdown when done

  }

}
