/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.trade;

import org.junit.jupiter.api.Test;

import cointoss.Direction;
import cointoss.Profitable;
import cointoss.Scenario;
import cointoss.TraderTestSupport;
import cointoss.execution.Execution;
import cointoss.trade.extension.SidePart;
import cointoss.trade.extension.TradeTest;
import cointoss.util.Num;

class ScenarioSnapshotTest extends TraderTestSupport {

    @TradeTest
    void snapshotCompleteEntryAndCompleteExit(SidePart side) {
        entryAndExit(Execution.with.direction(side, 1)
                .price(10), Execution.with.direction(side.inverse(), 1).price(20).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2 * side.sign);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4 * side.sign);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5 * side.sign);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10 * side.sign);
        assert snapshot.unrealizedProfit(Num.of(25)).is(0);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10 * side.sign);
        assert snapshot.unrealizedProfit(Num.of(5)).is(0 * side.sign);
    }

    @TradeTest
    void snapshotCompleteEntryAndIncompleteExit(SidePart side) {
        entryAndExit(Execution.with.direction(side, 2)
                .price(10), Execution.with.direction(side.inverse(), 1).price(20).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4 * side.sign);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-8 * side.sign);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(10 * side.sign);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10 * side.sign);
        assert snapshot.unrealizedProfit(Num.of(25)).is(15 * side.sign);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10 * side.sign);
        assert snapshot.unrealizedProfit(Num.of(5)).is(-5 * side.sign);
    }

    @Test
    void snapshotCompleteEntryAndPartialExit() {
        entryAndExitPartial(Execution.with.buy(2).price(10), Execution.with.sell(2).price(20).date(afterMinute(5)), 1);

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-8);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(10);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(5)).is(-5);
    }

    @Test
    void snapshotCompleteEntryAndNoExit() {
        entry(Execution.with.buy(1).price(10));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(25)).is(15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(5)).is(-5);
    }

    @Test
    void snapshotPartialEntryAndCompleteExit() {
        entryPartialAndExit(Execution.with.buy(2).price(10), 1, Execution.with.sell(1).price(20).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(0);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(5)).is(0);
    }

    @Test
    void snapshotPartialEntryAndIncompleteExit() {
        entryPartialAndExit(Execution.with.buy(3).price(10), 2, Execution.with.sell(1).price(20).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-8);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(10);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(5)).is(-5);
    }

    @Test
    void snapshotPartialEntryAndPartialExit() {
        entryPartialAndExitPartial(Execution.with.buy(2).price(10), 1, Execution.with.sell(2).price(20).date(afterMinute(5)), 1);

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(0);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(5)).is(0);
    }

    @Test
    void snapshotPartialEntryAndNoExit() {
        entryPartial(Execution.with.buy(2).price(10), 1);

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(25)).is(15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(5)).is(-5);
    }

    @Test
    void snapshotSellCompleteEntryAndPartialExit() {
        entryAndExitPartial(Execution.with.sell(2).price(10), Execution.with.buy(2).price(20).date(afterMinute(5)), 1);

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(8);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(-10);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(-10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(-15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(-10);
        assert snapshot.unrealizedProfit(Num.of(5)).is(5);
    }

    @Test
    void snapshotSellCompleteEntryAndNoExit() {
        entry(Execution.with.sell(1).price(10));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(-2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(-5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(25)).is(-15);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(5)).is(5);
    }

    @Test
    void snapshotLossCompleteEntryAndCompleteExit() {
        entryAndExit(Execution.with.buy(1).price(10), Execution.with.sell(1).price(5).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-4);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(2)).is(0);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(15)).is(0);
    }

    @Test
    void snapshotLossCompleteEntryAndIncompleteExit() {
        entryAndExit(Execution.with.buy(2).price(10), Execution.with.sell(1).price(5).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-8);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(10);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(2)).is(-8);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(20)).is(10);
    }

    @Test
    void snapshotLossCompleteEntryAndPartialExit() {
        entryAndExitPartial(Execution.with.buy(2).price(10), Execution.with.sell(2).price(5).date(afterMinute(5)), 1);

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4);

        // past
        snapshot = snapshotAt(epochAfterMinute(2));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(-8);

        // past
        snapshot = snapshotAt(epochAfterMinute(4));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(15)).is(10);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(8)).is(-2);

        // future
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(-5);
        assert snapshot.unrealizedProfit(Num.of(15)).is(5);
    }

    @Test
    void snapshotFirstProfitSecondLoss() {
        entryAndExit(Execution.with.buy(1).price(10), Execution.with.sell(1).price(20).date(afterMinute(5)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(2);

        // future
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(25)).is(0);

        entryAndExit(Execution.with.buy(1).price(10).date(afterMinute(10)), Execution.with.sell(1).price(5).date(afterMinute(15)));

        // past
        snapshot = snapshotAt(epochAfterMinute(11));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(8)).is(-2);

        // future
        snapshot = snapshotAt(epochAfterMinute(16));
        assert snapshot.realizedProfit().is(5);
        assert snapshot.unrealizedProfit(Num.of(2)).is(0);
    }

    @Test
    void snapshotDontCareSeconds() {
        when(now(), v -> new Scenario() {
            @Override
            protected void entry() {
                entry(Direction.BUY, 1.5, s -> s.make(10));
            }

            @Override
            protected void exit() {
            }
        });

        market.perform(Execution.with.buy(0.5).price(9).date(afterSecond(20)));
        market.perform(Execution.with.buy(0.5).price(9).date(afterSecond(40)));
        market.perform(Execution.with.buy(0.5).price(9).date(afterSecond(60)));

        // past
        Profitable snapshot = snapshotAt(epochAfterSecond(30));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(0);

        // past
        snapshot = snapshotAt(epochAfterSecond(59));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(6)).is(0);

        // now
        snapshot = snapshotAt(epochAfterSecond(60));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(3);

        // future
        snapshot = snapshotAt(epochAfterSecond(80));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(3);
    }

    @Test
    void snapshotShortLongLong() {
        entryAndExitPartial(Execution.with.sell(2).price(20), Execution.with.buy(2).price(10).date(afterMinute(5)), 1);
        entry(Execution.with.buy(1).price(15).date(afterMinute(7)));
        entry(Execution.with.buy(1).price(17).date(afterMinute(9)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(16);

        // exit first scenario partially
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(18)).is(2);

        // entry second scenario
        snapshot = snapshotAt(epochAfterMinute(8));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(18)).is(5);

        // entry third scenario
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(19)).is(7);
    }

    @Test
    void snapshotLongShortShort() {
        entryAndExitPartial(Execution.with.buy(2).price(10), Execution.with.sell(2).price(20).date(afterMinute(5)), 1);
        entry(Execution.with.sell(1).price(17).date(afterMinute(7)));
        entry(Execution.with.sell(1).price(15).date(afterMinute(9)));

        // past
        Profitable snapshot = snapshotAt(epochAfterMinute(1));
        assert snapshot.realizedProfit().is(0);
        assert snapshot.unrealizedProfit(Num.of(12)).is(4);

        // exit first scenario partially
        snapshot = snapshotAt(epochAfterMinute(6));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(18)).is(8);

        // entry second scenario
        snapshot = snapshotAt(epochAfterMinute(8));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(19)).is(7);

        // entry third scenario
        snapshot = snapshotAt(epochAfterMinute(10));
        assert snapshot.realizedProfit().is(10);
        assert snapshot.unrealizedProfit(Num.of(18)).is(4);
    }
}
