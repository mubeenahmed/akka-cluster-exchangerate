package com.trading.exchange


import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.trading.server.MsgSerializerMaker
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.duration.DurationInt


case class Rate(entityId: UUID, base: Currency, quote: Currency, rate: Double)
case class Currency(name: String, iso3: String)

trait Command
case class UpdateRate(rate: Rate, replyTo: ActorRef[StatusReply[RateUpdated]]) extends Command
case class AddRate(rate: Rate, replyTo: ActorRef[StatusReply[RateAdded]]) extends Command
case class GetRate(entityId: UUID, replyTo: ActorRef[StatusReply[CurrentState]]) extends Command


final case class State(rates: Map[UUID, Rate]) {
  def hasItem(key: UUID): Boolean = rates.contains(key)
  def update(rate: Rate): State = copy(rates + (rate.entityId -> rate))
  def add(rate: Rate): State = update(rate)
  def get(entityId: UUID): Option[Rate] = rates.get(entityId)
}

object State {
  val empty = State(rates = Map.empty)
}

trait Event extends MsgSerializerMaker
case class RateUpdated(entityId: UUID, base: Currency, quote: Currency, rate: Double) extends Event
case class RateAdded(entityId: UUID, base: Currency, quote: Currency, rate: Double) extends Event
case class CurrentState(entityId: UUID, rate: Option[Rate]) extends Event

object ExchangeRate
{

  val log = LoggerFactory.getLogger(getClass)

  val EntityKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("ExchangeRate")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { context =>
      ExchangeRate(context.entityId)
    })
  }

  def apply(entityId: String) : Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, entityId),
        emptyState = State.empty,
        commandHandler = (state, command) => handleCommand(entityId, state, command),
        eventHandler = (event, state) => handleEvent(event, state)
      )
      .withRetention(RetentionCriteria
        .snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  def handleCommand(entityId: String,
                    state: State,
                    command: Command): ReplyEffect[Event, State] = {
    command match {
      case UpdateRate(rate, replyTo) =>
        if(state.hasItem(rate.entityId)) {
          Effect
            .persist[Event, State](RateUpdated(UUID.fromString(entityId), rate.base, rate.quote, rate.rate))
            .thenReply(replyTo) { updated =>
              StatusReply.Success(RateUpdated(UUID.fromString(entityId), rate.base, rate.quote, rate.rate))
            }
        }
        else {
          Effect.reply(replyTo)(
            StatusReply.Error(s"Error: no currency supported"))
        }
      case AddRate(rate, replyTo) =>
        log.info(s"Rate is started adding ${rate.entityId}")
        if(!state.hasItem(rate.entityId)) {
          Effect
            .persist[Event, State](RateAdded(rate.entityId, rate.base, rate.quote, rate.rate))
            .thenReply(replyTo) { added =>
              StatusReply.Success(RateAdded(rate.entityId, rate.base, rate.quote, rate.rate))
            }
        } else {
          Effect.reply(replyTo)(
            StatusReply.Error(s"Error: currency already exists")
          )
        }
      case GetRate(entityId, replyTo) =>
        Effect.reply(replyTo)(StatusReply.Success(CurrentState(entityId, state.get(entityId))))

      case _ => ???
    }
  }


  def handleEvent(state: State, event: Event) = {
    event match {
      case rate: RateUpdated =>
        state.update(Rate(rate.entityId, rate.base, rate.quote, rate.rate))
      case rate: RateAdded =>
        log.info(s"Rate is storing added event ${rate.entityId}")
        state.add(Rate(rate.entityId, rate.base, rate.quote, rate.rate))
    }
  }
}