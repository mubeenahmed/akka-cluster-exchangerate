package com.trading.trader

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.http.scaladsl.model.DateTime
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.trading.exchange.Currency

import java.util.UUID
import scala.concurrent.duration.DurationInt


trait OrderType
case object Buy extends OrderType
case object Sell extends OrderType


case class Trader(traderId: UUID, equity: Double, orders: List[Order]) {
  def purchasingSize(lot: Double): Double = equity * lot
}
case class Order(orderId: UUID,
                 orderType: OrderType,
                 lot: Double,
                 atRate: Double,
                 base: Currency,
                 quote: Currency,
                 purchasedAt: DateTime)


trait Command
case class OrderBuy(orderId: UUID, lot: Double,
                    atRate: Double, base: Currency, quote: Currency,
                    replyTo: ActorRef[StatusReply[Event]]) extends Command

case class State(trader: Map[UUID, Trader]) {
  def canPurchase(entityId: UUID, lot: Double): Boolean = {
    val information = trader.get(entityId)
    if(information.isEmpty)
      false
    else
      information.get.equity > information.get.purchasingSize(lot)
  }

  def add(updateTrader: Trader): State = {
    val previous = get(updateTrader.traderId)
    if(previous.isDefined) {
      val tr = previous.get.copy(equity = previous.get.equity -
        updateTrader.purchasingSize(updateTrader.orders.head.lot),
        orders = previous.get.orders ::: updateTrader.orders)
      copy(trader + (tr.traderId -> tr))
    }
    else {
      this
    }
  }

  def get(entityId: UUID): Option[Trader] = trader.get(entityId)
}

object State {
  val empty = State(Map.empty)
}

trait Event
case class BuyOrdered(traderId: UUID,
                      orderId: UUID, lot: Double,
                      atRate: Double, base: Currency, quote: Currency,
                      purchasedAt: DateTime)
  extends Event

object TraderPurchases {

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("Trader")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { context =>
      TraderPurchases(context.entityId)
    })
  }

  def apply(entityId: String): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, entityId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(entityId, command, state),
        eventHandler = (event, state) => handleEvent(entityId, state, event)
      )
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )

  def handleCommand(entityId: String, command: Command, state: State): ReplyEffect[Event, State] =
    command match {
      case
        OrderBuy(orderId, lot, atRate, base, quote, replyTo) =>
        if(state.canPurchase(UUID.fromString(entityId), lot))
          Effect
            .persist[Event, State](
              BuyOrdered(UUID.fromString(entityId), orderId, lot, atRate, base, quote, DateTime.now))
            .thenReply(replyTo) { updated =>
              StatusReply.Success(
                BuyOrdered(UUID.fromString(entityId), orderId, lot, atRate, base, quote, DateTime.now))
            }
        else
          Effect.reply(replyTo)(
            StatusReply.Error(s"Error: order cannot be placed"))
    }

  def handleEvent(entityId: String, event: Event, state: State) = {
    event match {
      case bought: BuyOrdered =>
        val previousEntity = state.get(bought.traderId).get
        state.add(previousEntity.copy(orders =
          List(Order(UUID.randomUUID(), Buy, bought.lot,
            bought.atRate, bought.base, bought.quote, bought.purchasedAt))))
    }
  }
}
