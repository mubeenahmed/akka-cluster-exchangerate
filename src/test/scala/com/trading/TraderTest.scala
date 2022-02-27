package com.trading

import akka.http.scaladsl.model.DateTime
import com.trading.exchange.Currency
import com.trading.trader.{Buy, Order, State, Trader}
import org.scalatest.flatspec.AnyFlatSpec

import java.util.UUID

class TraderTest extends AnyFlatSpec {

  val uuid = UUID.randomUUID()
  val trader = Trader(uuid, 100.0, List.empty)
  val db = Map(uuid -> trader)
  "Trader" should "add to the state" in {
    val state = State(db)

    val orders = Order(UUID.randomUUID(), Buy, 0.01, 1.35,
      Currency("Great Britain Pound", "GBP"), Currency("United State Dollar", "USD"),
      DateTime.now)
    val t = Trader(trader.traderId, trader.equity - trader.purchasingSize(0.01), trader.orders.::(orders) )
    val newState = state.add(t)

    assert(newState.get(trader.traderId).get.orders.size == 1)
    assert(newState.get(trader.traderId).get.equity == 99.01)
  }

  "Trader" should "calculate the equity" in {
    val purchasedSize = trader.purchasingSize(0.01);
    assert(purchasedSize == 1);
  }

  "Trader" should "have enough equity" in {
    val state = State(db)
    assert(!state.canPurchase(trader.traderId, 100.0))
    assert(state.canPurchase(trader.traderId, 0.01))
  }
}
