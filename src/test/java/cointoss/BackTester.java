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

import java.util.function.Supplier;
import java.util.stream.IntStream;

import cointoss.Time.Lag;
import cointoss.analyze.TradingLog;
import eu.verdelhan.ta4j.Decimal;
import kiss.I;
import kiss.Signal;

/**
 * @version 2017/09/19 23:46:31
 */
public class BackTester {

    /** 試行回数 */
    private int trial = 7;

    /** 基軸通貨量 */
    private Decimal base = Decimal.ZERO;

    /** 対象通貨量 */
    private Decimal target = Decimal.ZERO;

    /** テスト戦略 */
    private Supplier<Trading> strategy;

    /** The execution log. */
    private Supplier<Signal<Execution>> log;

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
    public BackTester strategy(Supplier<Trading> strategy) {
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
     * @param rangeAll
     */
    public BackTester log(Signal<Execution> log) {
        return log(() -> log);
    }

    /**
     * Set log file.
     * 
     * @param log
     * @return
     */
    public BackTester log(Supplier<Signal<Execution>> log) {
        this.log = log;

        return this;
    }

    /**
     * Set initial balance.
     * 
     * @param base
     * @return
     */
    public BackTester baseCurrency(int base) {
        return baseCurrency(Decimal.of(base));
    }

    /**
     * Set initial balance.
     * 
     * @param base
     * @return
     */
    public BackTester baseCurrency(Decimal base) {
        if (base.isGreaterThanOrEqual(0)) {
            this.base = base;
        }
        return this;
    }

    /**
     * Set initial balance.
     * 
     * @param target
     * @return
     */
    public BackTester targetCurrency(int target) {
        return targetCurrency(Decimal.of(target));
    }

    /**
     * Set initial balance.
     * 
     * @param target
     * @return
     */
    public BackTester targetCurrency(Decimal target) {
        if (target.isGreaterThanOrEqual(0)) {
            this.target = target;
        }
        return this;
    }

    /**
     * Execute back test.
     */
    public void run() {
        IntStream.range(0, trial).parallel().mapToObj(i -> new Market(new BackTestBackend(), log.get(), strategy.get())).forEach(market -> {
            System.out.println(new TradingLog(market, market.tradings));
        });
    }

    /**
     * <p>
     * Build new back tester.
     * </p>
     * 
     * @return
     */
    public static BackTester with() {
        return new BackTester();
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
}
