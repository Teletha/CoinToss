/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.backtest;

import static cointoss.util.Num.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import cointoss.MarketTestSupport;
import cointoss.Side;
import cointoss.order.Order;
import cointoss.order.Order.Quantity;
import cointoss.order.Order.State;
import cointoss.util.Num;

/**
 * @version 2018/04/29 16:12:46
 */
class TestableMarketTest {

    static final Num FIVE = Num.of(5);

    TestableMarket market = new TestableMarket();

    @Test
    void requestOrder() {
        Order order = market.requestTo(Order.limitLong(1, 10));
        assert order.isBuy();
        assert order.executed_size.is(ZERO);
        assert market.orders().size() == 1;
    }

    @Test
    void execute() {
        Order order = market.requestTo(Order.limitLong(1, 10));
        assert order.remaining.is(ONE);
        assert order.executed_size.is(ZERO);

        market.execute(Side.BUY, 1, 10);
        assert order.remaining.is(ZERO);
        assert order.executed_size.is(ONE);
    }

    @Test
    void executeDivided() {
        Order order = market.requestTo(Order.limitLong(10, 10));
        assert order.remaining.is(TEN);
        assert order.executed_size.is(ZERO);

        market.execute(Side.SELL, 5, 10);
        assert order.remaining.is(FIVE);
        assert order.executed_size.is(FIVE);

        market.execute(Side.SELL, 5, 10);
        assert order.remaining.is(ZERO);
        assert order.executed_size.is(TEN);
        assert market.validateExecutionState(2);
    }

    @Test
    void executeOverflow() {
        market.request(Order.limitLong(10, 10)).to(order -> {
            assert order.remaining.get().is(10);
            assert order.executed_size.get().is(0);

            market.execute(Side.BUY, 7, 10);
            assert order.remaining.get().is(3);
            assert order.executed_size.get().is(7);

            market.execute(Side.SELL, 7, 10);
            assert order.remaining.get().is(0);
            assert order.executed_size.get().is(10);
        });

        List<Order> orders = market.orders();
        assert orders.size() == 1;
        assert orders.get(0).state.is(State.COMPLETED);

        // List<Execution> executions = market.backend.executions().toList();
        // assert executions.size() == 2;
        // assert executions.get(0).size.is(7);
        // assert executions.get(1).size.is(3);
    }

    @Test
    void executeExtra() {
        market.request(Order.limitLong(10, 10)).to(order -> {
            assert order.remaining.get().is(10);
            assert order.executed_size.get().is(0);

            market.execute(Side.BUY, 10, 10);
            assert order.remaining.get().is(0);
            assert order.executed_size.get().is(10);

            market.execute(Side.SELL, 1, 10);
            assert order.remaining.get().is(0);
            assert order.executed_size.get().is(10);
        });

        assert market.validateOrderState(0, 1, 0, 0, 0);
        assert market.validateExecutionState(1);
    }

    @Test
    void executeLongWithUpperPrice() {
        market.request(Order.limitLong(10, 10)).to(order -> {
            market.execute(Side.BUY, 5, 12);
            market.execute(Side.SELL, 5, 13);
        });

        assert market.validateOrderState(1, 0, 0, 0, 0);
        assert market.validateExecutionState(0);
    }

    @Test
    void executeLongWithLowerPrice() {
        market.request(Order.limitLong(10, 10)).to(order -> {
            market.execute(Side.BUY, 5, 8);
            market.execute(Side.SELL, 5, 7);
        });

        assert market.validateOrderState(0, 1, 0, 0, 0);
        assert market.validateExecutionState(2);
    }

    @Test
    void executeShortWithUpperPrice() {
        market.request(Order.limitShort(10, 10)).to(order -> {
            market.execute(Side.BUY, 5, 12);
            market.execute(Side.SELL, 5, 13);
        });

        assert market.validateOrderState(0, 1, 0, 0, 0);
        assert market.validateExecutionState(2);
    }

    @Test
    void executeShortWithLowerPrice() {
        market.request(Order.limitShort(10, 10)).to(order -> {
            market.execute(Side.BUY, 5, 8);
            market.execute(Side.SELL, 5, 7);
        });

        assert market.validateOrderState(1, 0, 0, 0, 0);
        assert market.validateExecutionState(0);
    }

    @Test
    void lag() {
        TestableMarket market = new TestableMarket(5);

        market.requestTo(Order.limitLong(10, 10));
        market.execute(Side.BUY, 5, 10, Time.at(3));
        market.execute(Side.BUY, 4, 10, Time.at(4));
        market.execute(Side.BUY, 3, 10, Time.at(5));
        market.execute(Side.BUY, 2, 10, Time.at(6));
        market.execute(Side.BUY, 1, 10, Time.at(7));

        assert market.validateExecutionState(3);
        assert market.validateOrderState(1, 0, 0, 0, 0);
    }

    @Test
    void shortWithTrigger() {
        market.requestTo(Order.limitShort(1, 7).when(8));
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 8);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 7);
        assert market.validateOrderState(0, 1, 0, 0, 0);
    }

    @Test
    void shortWithTriggerSamePrice() {
        market.requestTo(Order.limitShort(1, 8).when(8));
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 8);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 7);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 8);
        assert market.validateOrderState(0, 1, 0, 0, 0);
    }

    @Test
    void shortMarketWithTrigger() {
        Order order = Order.marketShort(1).when(8);
        market.requestTo(order);
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 8);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 7);
        assert market.validateOrderState(0, 1, 0, 0, 0);
        assert order.averagePrice.get().is(7);

        order = Order.marketShort(1).when(8);
        market.requestTo(order);
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(1, 1, 0, 0, 0);
        market.execute(Side.BUY, 1, 8);
        assert market.validateOrderState(1, 1, 0, 0, 0);
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(0, 2, 0, 0, 0);
        assert order.averagePrice.get().is(9);
    }

    @Test
    void longWithTrigger() {
        market.requestTo(Order.limitLong(1, 13).when(12));
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 13);
        assert market.validateOrderState(0, 1, 0, 0, 0);
    }

    @Test
    void longWithTriggerSamePrice() {
        market.requestTo(Order.limitLong(1, 12).when(12));
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 13);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(0, 1, 0, 0, 0);
    }

    @Test
    void longMarketWithTrigger() {
        Order order = Order.marketLong(1).when(12);
        market.requestTo(order);
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.execute(Side.BUY, 1, 13);
        assert market.validateOrderState(0, 1, 0, 0, 0);
        assert order.averagePrice.get().is(13);

        order = Order.marketLong(1).when(12);
        market.requestTo(order);
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(1, 1, 0, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(1, 1, 0, 0, 0);
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(0, 2, 0, 0, 0);
        assert order.averagePrice.get().is(11);
    }

    @Test
    void fillOrKillLong() {
        // success
        market.requestTo(Order.limitLong(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.BUY, 10, 10);
        assert market.validateOrderState(0, 1, 0, 0, 0);

        // large price will success
        market.requestTo(Order.limitLong(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.BUY, 10, 9);
        assert market.validateOrderState(0, 2, 0, 0, 0);

        // large size will success
        market.requestTo(Order.limitLong(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.BUY, 15, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less size will be failed
        market.requestTo(Order.limitLong(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.BUY, 4, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less price will be failed
        market.requestTo(Order.limitLong(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.BUY, 10, 11);
        assert market.validateOrderState(0, 3, 0, 0, 0);
    }

    @Test
    void fillOrKillShort() {
        // success
        market.requestTo(Order.limitShort(1, 10).type(Quantity.FillOrKill));
        market.execute(Side.SELL, 1, 10);
        assert market.validateOrderState(0, 1, 0, 0, 0);

        // large price will success
        market.requestTo(Order.limitShort(1, 10).type(Quantity.FillOrKill));
        market.execute(Side.SELL, 1, 11);
        assert market.validateOrderState(0, 2, 0, 0, 0);

        // large size will success
        market.requestTo(Order.limitShort(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.SELL, 15, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less size will be failed
        market.requestTo(Order.limitShort(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.SELL, 4, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less price will be failed
        market.requestTo(Order.limitShort(10, 10).type(Quantity.FillOrKill));
        market.execute(Side.SELL, 10, 9);
        assert market.validateOrderState(0, 3, 0, 0, 0);
    }

    @Test
    void immediateOrCancelLong() {
        // success
        market.requestTo(Order.limitLong(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.BUY, 1, 10);
        assert market.validateOrderState(0, 1, 0, 0, 0);

        // large price will success
        market.requestTo(Order.limitLong(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.BUY, 1, 9);
        assert market.validateOrderState(0, 2, 0, 0, 0);

        // large size will success
        market.requestTo(Order.limitLong(10, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.BUY, 5, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less size will success
        market.requestTo(Order.limitLong(10, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.BUY, 4, 10);
        assert market.validateOrderState(0, 4, 0, 0, 0);
        assert market.orders().get(3).executed_size.get().is(4);

        // less price will be failed
        market.requestTo(Order.limitLong(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(0, 4, 0, 0, 0);
    }

    @Test
    void immediateOrCancelShort() {
        // success
        market.requestTo(Order.limitShort(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.SELL, 1, 10);
        assert market.validateOrderState(0, 1, 0, 0, 0);

        // large price will success
        market.requestTo(Order.limitShort(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.SELL, 1, 11);
        assert market.validateOrderState(0, 2, 0, 0, 0);

        // large size will success
        market.requestTo(Order.limitShort(10, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.SELL, 15, 10);
        assert market.validateOrderState(0, 3, 0, 0, 0);

        // less size will success
        market.requestTo(Order.limitShort(10, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.SELL, 4, 10);
        assert market.validateOrderState(0, 4, 0, 0, 0);
        assert market.orders().get(3).executed_size.get().is(4);

        // less price will be failed
        market.requestTo(Order.limitShort(1, 10).type(Quantity.ImmediateOrCancel));
        market.execute(Side.SELL, 1, 9);
        assert market.validateOrderState(0, 4, 0, 0, 0);
    }

    @Test
    void marketLong() {
        market.requestTo(Order.marketLong(1));
        market.execute(Side.SELL, 1, 10);
        assert market.orders().get(0).averagePrice.get().is(10);
        assert market.orders().get(0).executed_size.get().is(1);

        // divide
        market.requestTo(Order.marketLong(10));
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 20);
        assert market.orders().get(1).averagePrice.get().is(15);
        assert market.orders().get(1).executed_size.get().is(10);

        // divide overflow
        market.requestTo(Order.marketLong(10));
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 14, 20);
        assert market.orders().get(2).averagePrice.get().is(15);
        assert market.orders().get(2).executed_size.get().is(10);

        // divide underflow
        market.requestTo(Order.marketLong(10));
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 3, 20);
        assert market.orders().get(3).averagePrice.get().is("13.75");
        assert market.orders().get(3).executed_size.get().is(8);
        market.execute(Side.BUY, 2, 20);
        assert market.orders().get(3).averagePrice.get().is("15");

        // down price
        market.requestTo(Order.marketLong(10));
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 5);
        assert market.orders().get(4).averagePrice.get().is("10");
        assert market.orders().get(4).executed_size.get().is(10);

        // up price
        market.requestTo(Order.marketLong(10));
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 20);
        assert market.orders().get(5).averagePrice.get().is("15");
        assert market.orders().get(5).executed_size.get().is(10);
    }

    @Test
    void marketShort() {
        Order order = Order.marketShort(1);
        market.requestTo(order);
        market.execute(Side.SELL, 1, 10);
        assert order.averagePrice.get().is(10);
        assert order.executed_size.get().is(1);

        // divide
        order = Order.marketShort(10);
        market.requestTo(order);
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 5);
        assert order.averagePrice.get().is("7.5");
        assert order.executed_size.get().is(10);

        // divide overflow
        order = Order.marketShort(10);
        market.requestTo(order);
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 14, 5);
        assert order.averagePrice.get().is("7.5");
        assert order.executed_size.get().is(10);

        // divide underflow
        order = Order.marketShort(10);
        market.requestTo(order);
        market.execute(Side.BUY, 5, 20);
        market.execute(Side.BUY, 3, 15);
        assert order.averagePrice.get().is("18.125");
        assert order.executed_size.get().is(8);
        market.execute(Side.BUY, 2, 10);
        assert order.averagePrice.get().is("16.5");

        // down price
        order = Order.marketShort(10);
        market.requestTo(order);
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 5);
        assert order.averagePrice.get().is("7.5");
        assert order.executed_size.get().is(10);

        // up price
        order = Order.marketShort(10);
        market.requestTo(order);
        market.execute(Side.BUY, 5, 10);
        market.execute(Side.BUY, 5, 20);
        assert order.averagePrice.get().is("10");
        assert order.executed_size.get().is(10);
    }

    @Test
    void cancel() {
        Order order = market.requestTo(Order.limitShort(1, 12));
        market.execute(Side.BUY, 1, 11);
        assert market.validateOrderState(1, 0, 0, 0, 0);
        market.cancel(order).to();
        assert market.validateOrderState(0, 0, 1, 0, 0);
        market.execute(Side.BUY, 1, 12);
        assert market.validateOrderState(0, 0, 1, 0, 0);
    }

    @Test
    void observeSequencialExecutionsBySellSize() {
        AtomicReference<Num> size = new AtomicReference<>();

        market.timelineByTaker.to(e -> {
            size.set(e.cumulativeSize);
        });

        MarketTestSupport.executionSerially(4, Side.SELL, 10, 5).forEach(market::execute);
        market.execute(Side.SELL, 5, 10);
        assert size.get().is(20);
    }

    @Test
    void observeSequencialExecutionsByBuySize() {
        AtomicReference<Num> size = new AtomicReference<>();

        market.timelineByTaker.to(e -> {
            size.set(e.cumulativeSize);
        });

        MarketTestSupport.executionSerially(4, Side.BUY, 10, 5).forEach(market::execute);
        market.execute(Side.BUY, 5, 10);
        assert size.get().is(20);
    }
}