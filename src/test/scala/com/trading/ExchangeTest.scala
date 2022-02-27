package com.trading

import com.trading.exchange.{Currency, Rate, State}
import org.scalatest.flatspec.AnyFlatSpec

import java.util.UUID

class ExchangeTest extends AnyFlatSpec {

  private val uuid = UUID.randomUUID()
  private val gbp = Currency("Great Britain Bound", "GBP")
  private val usd = Currency("United State Dollar", "USD")
  private val jpy = Currency("Japanese Yen", "JPY")
  val rates = List(Rate(uuid, gbp, usd, 1.36))

  val db: Map[UUID, Rate] = Map(uuid -> rates.head)
  "Exchange" should "return available update rate" in {
    val state = State(db)
    val newState = state.update(rates.head.copy(rate = 1.37))
    assert(newState.rates(uuid).rate == 1.37)
  }

  "Exchange" should "add new currency" in {
    val id = UUID.randomUUID()
    val state = State(db)
    val newRates = Rate(id, jpy, usd, 0.9)
    val newState = state.add(newRates)
    assert(newState.hasItem(id))
  }
}
