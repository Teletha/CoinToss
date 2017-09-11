/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import static java.time.temporal.ChronoUnit.*;

import java.util.stream.IntStream;

import cointoss.Time.Lag;
import cointoss.chart.Tick;
import cointoss.market.bitflyer.BitFlyer;
import eu.verdelhan.ta4j.Decimal;
import kiss.I;
import kiss.Signal;

/**
 * @version 2017/07/24 20:56:39
 */
public class BackTester {

    /** 試行回数 */
    private int trial = 1;

    /** 基軸通貨量 */
    private Decimal base = Decimal.ZERO;

    /** 対象通貨量 */
    private Decimal target = Decimal.ZERO;

    /** テスト対象マーケット */
    private MarketLog marketLog;

    /** テスト戦略 */
    private Class<? extends Trading> strategy;

    /** ラグ生成器 */
    private Lag lag = Time.lag(2, 15);

    /**
     * Hide
     */
    private BackTester() {
    }

    /**
     * Set the test strategy.
     * 
     * @param strategy
     * @return
     */
    public BackTester strategy(Class<? extends Trading> strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
        return this;
    }

    /**
     * Set a number of trial.
     * 
     * @param number
     * @return
     */
    public BackTester trial(int number) {
        if (0 < number) {
            this.trial = number;
        }
        return this;
    }

    /**
     * Set initial balance.
     * 
     * @param base
     * @param target
     * @return
     */
    public BackTester balance(int base, int target) {
        return balance(Decimal.of(base), Decimal.of(target));
    }

    /**
     * Set initial balance.
     * 
     * @param base
     * @param target
     * @return
     */
    public BackTester balance(Decimal base, Decimal target) {
        if (base.isGreaterThanOrEqual(0) && target.isLessThanOrEqual(0)) {
            this.base = base;
            this.target = target;
        }
        return this;
    }

    /**
     * Execute back test.
     */
    public void execute() {
        IntStream.range(0, trial)
                .parallel()
                .mapToObj(i -> new Market(new BackTestBackend(), marketLog.rangeRandom(5), strategy))
                .forEach(market -> {
                    market.logger.analyze();
                });
    }

    /**
     * <p>
     * Create new back tester.
     * </p>
     * 
     * @return
     */
    public static BackTester initialize(MarketLog marketLog) {
        BackTester tester = new BackTester();
        tester.marketLog = marketLog;

        return tester;
    }

    /**
     * @version 2017/08/16 9:16:09
     */
    private class BackTestBackend extends TestableMarketBackend {

        /**
         */
        private BackTestBackend() {
            super(lag);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<BalanceUnit> getCurrency() {
            BalanceUnit base = new BalanceUnit();
            base.currency_code = "JPY";
            base.amount = base.available = BackTester.this.base;

            BalanceUnit target = new BalanceUnit();
            target.currency_code = "BTC";
            target.amount = target.available = BackTester.this.target;

            return I.signal(base, target);
        }
    }

    /**
     * Run back test.
     * 
     * @param args
     */
    public static void main(String[] args) {
        BackTester tester = BackTester.initialize(BitFlyer.FX_BTC_JPY.log()).balance(1000000, 0).strategy(BreakoutTrading.class);
        tester.execute();
    }

    /**
     * @version 2017/09/05 20:19:04
     */
    private static class BreakoutTrading extends Trading {

        private Decimal underPrice;

        /**
         * @param market
         * @param exe
         */
        private BreakoutTrading(Market market) {
            super(market);

            // various events
            market.timeline.to(exe -> {
                if (hasNoPosition()) {
                    entryLimit(Side.random(), maxPositionSize, exe.price, entry -> {
                        calculateUnderline(exe.price);

                        // // cancel timing
                        // market.timeline.takeUntil(completingEntry)
                        // .take(keep(5, MINUTES, entry::isNotCompleted))
                        // .take(1)
                        // .mapTo(entry)
                        // .to(market::cancel);

                        // rise under price line
                        market.minute1.tick.takeUntil(closingPosition) //
                                .map(Tick::getClosePrice)
                                .to(this::calculateUnderline);

                        // loss cut
                        market.timeline.takeUntil(closingPosition) //
                                .take(keep(5, SECONDS, e -> e.price.isLessThan(entry, underPrice)))
                                .take(1)
                                .to(e -> {
                                    exitMarket(entry.executed());
                                });
                    });
                }
            });
        }

        private void calculateUnderline(Decimal consultation) {
            Decimal next = consultation.minus(position, 2000);
            Decimal d = underPrice;
            underPrice = underPrice == null || next.isGreaterThan(position, underPrice) ? next : underPrice;

            if (d != underPrice) {
                System.out.println(d + " is up to " + underPrice);
            }
        }
    }
}
