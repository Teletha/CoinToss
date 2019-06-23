/*
 * Copyright (C) 2019 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.trader;

import static java.time.temporal.ChronoUnit.*;

import org.junit.jupiter.api.Test;

import cointoss.Direction;
import cointoss.execution.Execution;
import kiss.Variable;

class TraderTest extends TraderTestSupport {

    @Test
    void entryMake() {
        when(now(), v -> {
            return new Entry(Direction.BUY) {

                @Override
                protected void order() {
                    order(1, s -> s.make(10));
                }
            };
        });

        Entry entry = latest();
        assert entry != null;
        assert entry.isBuy();
        assert entry.entrySize.is(1);
        assert entry.entryExecutedSize.is(0);
        assert entry.entryPrice.is(0);

        // execute entry order
        market.perform(Execution.with.buy(1).price(9));
        assert entry.entrySize.is(1);
        assert entry.entryExecutedSize.is(1);
        assert entry.entryPrice.is(10);
    }

    @Test
    void exitMakeAtPrice() {
        when(now(), v -> {
            return new Entry(Direction.BUY) {

                @Override
                protected void order() {
                    order(1, s -> s.make(10));
                }

                @Override
                protected void exit() {
                    exitAt(20);
                }
            };
        });

        Entry entry = latest();

        // execute entry order
        market.perform(Execution.with.buy(1).price(9));
        assert entry.entrySize.is(1);
        assert entry.entryExecutedSize.is(1);
        assert entry.entryPrice.is(10);

        // exit order
        assert entry.exitSize.is(1);
        assert entry.exitExecutedSize.is(0);
        assert entry.exitPrice.is(0);

        // don't execute exit order
        market.perform(Execution.with.buy(1).price(15));
        assert entry.exitSize.is(1);
        assert entry.exitExecutedSize.is(0);
        assert entry.exitPrice.is(0);

        // execute exit order
        market.perform(Execution.with.buy(1).price(21));
        assert entry.exitSize.is(1);
        assert entry.exitExecutedSize.is(1);
        assert entry.exitPrice.is(20);
    }

    @Test
    void exitWillStopAllEntries() {
        when(now(), v -> {
            return new Entry(Direction.BUY) {

                @Override
                protected void order() {
                    order(3, s -> s.make(10));
                }

                @Override
                protected void exit() {
                    exitAt(20);
                }
            };
        });

        Entry e = latest();

        // entry partially
        market.perform(Execution.with.buy(2).price(9));
        assert e.isEntryTerminated() == false;
        assert e.isExitTerminated() == false;

        // exit pertially
        market.perform(Execution.with.buy(1).price(21));
        assert e.isEntryTerminated();
        assert e.isExitTerminated() == false;

        // exit all
        market.perform(Execution.with.buy(1).price(21));
        assert e.isEntryTerminated();
        assert e.isExitTerminated();
    }

    @Test
    void profitBuy() {
        when(now(), v -> {
            return new Entry(Direction.BUY) {

                @Override
                protected void order() {
                    order(3, s -> s.make(10));
                }

                @Override
                protected void exit() {
                    exitAt(20);
                }
            };
        });

        Entry e = latest();
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // entry partially
        market.perform(Execution.with.buy(2).price(9));
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // execute profit
        market.perform(Execution.with.buy(1).price(15));
        assert e.profit.is(10);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(10);

        // exit partially
        market.perform(Execution.with.buy(1).price(21));
        assert e.profit.is(20);
        assert e.realizedProfit.is(10);
        assert e.unrealizedProfit.is(10);

        // exit all
        market.perform(Execution.with.buy(1).price(21));
        assert e.profit.is(20);
        assert e.realizedProfit.is(20);
        assert e.unrealizedProfit.is(0);
    }

    @Test
    void profitSell() {
        when(now(), v -> {
            return new Entry(Direction.SELL) {

                @Override
                protected void order() {
                    order(3, s -> s.make(20));
                }

                @Override
                protected void exit() {
                    exitAt(10);
                }
            };
        });

        Entry e = latest();
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // entry partially
        market.perform(Execution.with.buy(2).price(21));
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // execute profit
        market.perform(Execution.with.buy(1).price(15));
        assert e.profit.is(10);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(10);

        // exit partially
        market.perform(Execution.with.buy(1).price(9));
        assert e.profit.is(20);
        assert e.realizedProfit.is(10);
        assert e.unrealizedProfit.is(10);

        // exit all
        market.perform(Execution.with.buy(1).price(9));
        assert e.profit.is(20);
        assert e.realizedProfit.is(20);
        assert e.unrealizedProfit.is(0);
    }

    @Test
    void loss() {
        when(now(), v -> {
            return new Entry(Direction.BUY) {

                @Override
                protected void order() {
                    order(2, s -> s.make(20));
                }

                @Override
                protected void exit() {
                    exitAt(10);
                }
            };
        });

        Entry e = latest();
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // entry partially
        market.perform(Execution.with.buy(2).price(19));
        assert e.profit.is(0);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(0);

        // execute loss
        market.perform(Execution.with.buy(1).price(15));
        assert e.profit.is(-10);
        assert e.realizedProfit.is(0);
        assert e.unrealizedProfit.is(-10);

        // activate stop loss
        market.perform(Execution.with.buy(1).price(10));

        // exit partially
        market.perform(Execution.with.buy(1).price(10));
        assert e.profit.is(-20);
        assert e.realizedProfit.is(-10);
        assert e.unrealizedProfit.is(-10);

        // exit all
        market.perform(Execution.with.buy(1).price(10));
        assert e.profit.is(-20);
        assert e.realizedProfit.is(-20);
        assert e.unrealizedProfit.is(0);
    }

    @Test
    void keep() {
        Variable<Execution> state = market.timeline.take(keep(5, SECONDS, e -> e.price.isLessThan(10))).to();
        assert state.isAbsent();

        // keep more than 10
        market.perform(Execution.with.buy(1).price(15), 2);
        market.perform(Execution.with.buy(1).price(15), 2);
        market.perform(Execution.with.buy(1).price(15), 2);
        assert state.isAbsent();

        // keep less than 10 during 3 seconds
        market.perform(Execution.with.buy(1).price(9), 1);
        market.perform(Execution.with.buy(1).price(9), 1);
        market.perform(Execution.with.buy(1).price(15), 3);
        market.perform(Execution.with.buy(1).price(15), 3);
        assert state.isAbsent();

        // keep less than 10 during 5 seconds
        market.perform(Execution.with.buy(1).price(9), 3);
        market.perform(Execution.with.buy(1).price(9), 3);
        market.perform(Execution.with.buy(1).price(9), 3);
        assert state.isPresent();
    }
}
