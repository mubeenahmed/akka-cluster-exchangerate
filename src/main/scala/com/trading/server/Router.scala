package com.trading.server

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directives, RequestContext, RouteResult}
import akka.http.scaladsl.server.Directives.{complete, path}
import akka.pattern.StatusReply
import akka.util.Timeout
import com.trading.exchange.{AddRate, Command, Currency, CurrentState, ExchangeRate, GetRate, Rate, RateAdded, UpdateRate}

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import Directives._

trait MsgSerializerMaker

class Router(implicit val actorSystem: ActorSystem[_]) {

  import spray.json.DefaultJsonProtocol._

  implicit val currency = jsonFormat2(Currency)
  implicit val addRateRequest = jsonFormat4(AddRateRequest)
  implicit val exchangeResponse = jsonFormat1(MessageResponse)

  sealed trait ExchangeResponse extends MsgSerializerMaker
  final case class AddRateRequest(entityId: String, base: Currency, quote: Currency, rate: Double)
    extends MsgSerializerMaker

  final case class MessageResponse(message: String) extends ExchangeResponse

  private val uuid = UUID.randomUUID()
  private val gbp = Currency("Great Britain Bound", "GBP")
  private val usd = Currency("United State Dollar", "USD")
  private val jpy = Currency("Japanese Yen", "JPY")
  val rate = Rate(uuid, gbp, usd, 1.36)

  val TypeKey = EntityTypeKey[Command]("ExchangeRate")
  implicit val ec = actorSystem.executionContext

  def apply()(implicit context: ActorSystem[_]): RequestContext => Future[RouteResult] = {
    val sharding = ClusterSharding(context)

    path("add") {
      post {
        val entityId = UUID.randomUUID().toString
        val psCommandActor: EntityRef[Command] = sharding.entityRefFor(TypeKey, entityId)
        complete(StatusCodes.OK, addRates(psCommandActor, AddRateRequest(entityId, gbp, usd, 1.40d)))
      }
    } ~
    path("get") {
      parameters("entityId") { entityId =>
        val psCommandActor: EntityRef[Command] = sharding.entityRefFor(TypeKey, entityId)
        complete(StatusCodes.OK, queryRates(psCommandActor, entityId))
      }
    }
  }

  def addRates(psCommandActor: EntityRef[Command], req: AddRateRequest): Future[String] = {
    implicit val timeout = Timeout(5.seconds)
    val result = psCommandActor ? { ref : ActorRef[StatusReply[RateAdded]] =>
      AddRate(
        Rate(UUID.fromString(req.entityId), req.base, req.quote, req.rate), ref)
    }
    handleResponse(req, result)
  }

  def queryRates(psCommandActor: EntityRef[Command], entityId: String): Future[String] = {
    implicit val timeout = Timeout(5.seconds)
    val result = psCommandActor ? { ref: ActorRef[StatusReply[CurrentState]] =>
      GetRate(UUID.fromString(entityId), ref)
    }
    result.map {
      case rate: StatusReply[CurrentState] => s"${rate.getValue}"
    }
  }

  def handleResponse(req: AddRateRequest, f: Future[StatusReply[RateAdded]]): Future[String] = {
    f.map {
      case rateAdded: StatusReply[RateAdded] =>
        s"${rateAdded.getValue.rate} is added! Entity Id is ${rateAdded.getValue.entityId}"
    }
  }

}
