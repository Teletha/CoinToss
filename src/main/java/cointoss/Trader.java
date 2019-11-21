/*
 * Copyright (C) 2019 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

import static java.time.temporal.ChronoUnit.*;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.collections.impl.list.mutable.FastList;

import com.google.common.annotations.VisibleForTesting;

import cointoss.execution.Execution;
import cointoss.ticker.Indicator;
import cointoss.ticker.Span;
import cointoss.ticker.Tick;
import cointoss.util.Chrono;
import cointoss.util.Num;
import kiss.Disposable;
import kiss.Signal;
import kiss.WiseFunction;
import kiss.WiseSupplier;

public abstract class Trader {

    /** The identity element of {@link Snapshot}. */
    @VisibleForTesting
    static final Snapshot EMPTY_SNAPSHOT = new Snapshot(Num.ZERO, Num.ZERO, Num.ZERO);

    /** The market. */
    protected final Market market;

    /** The fund management. */
    protected final FundManager funds;

    /** All managed entries. */
    final FastList<Scenario> scenarios = new FastList();

    /** The alive state. */
    private final AtomicBoolean enable = new AtomicBoolean(true);

    /** The disposer manager. */
    private final Disposable disposer = Disposable.empty();

    /** The state snapshot. */
    @VisibleForTesting
    final NavigableMap<ZonedDateTime, Snapshot> snapshots = new TreeMap<>(Map.of(Chrono.MIN, EMPTY_SNAPSHOT));

    /** The actual maximum holding size. (historical data) */
    private Num maxHoldSize = Num.ZERO;

    /** The current holding size. */
    private Num currentHoldSize = Num.ZERO;

    /**
     * Declare your strategy.
     * 
     * @param market A target market to deal.
     */
    public Trader(Market market) {
        this.market = Objects.requireNonNull(market);
        this.market.managedTraders.add(this);
        this.funds = FundManager.with.totalAssets(market.service.baseCurrency().first().to().v);
    }

    /**
     * Set up entry at your timing.
     * 
     * @param <T>
     * @param timing
     * @param builder
     * @return Chainable API.
     */
    public final <T> Trader when(Signal<T> timing, WiseSupplier<Scenario> builder) {
        return when(timing, v -> builder.get());
    }

    /**
     * Set up entry at your timing.
     * 
     * @param <T>
     * @param timing
     * @param builder
     * @return Chainable API.
     */
    public final <T> Trader when(Signal<T> timing, WiseFunction<T, Scenario> builder) {
        Objects.requireNonNull(timing);
        Objects.requireNonNull(builder);

        disposer.add(timing.takeWhile(v -> enable.get()).to(value -> {
            Scenario scenario = builder.apply(value);

            if (scenario != null) {
                scenario.trader = this;
                scenario.market = market;
                scenario.funds = funds;
                scenario.entry();

                if (scenario.entries.isEmpty() == false) {
                    scenarios.add(scenario);
                }
            }
        }));
        return this;
    }

    /**
     * Calcualte the sanpshot when market is the specified datetime and price.
     * 
     * @param time The specified date and time.
     * @return A snapshot of this {@link Scenario}.
     */
    public final Profitable snapshotAt(ZonedDateTime time) {
        return snapshots.floorEntry(time).getValue();
    }

    /**
     * Return the maximum hold size of target currency.
     * 
     * @return A maximum hold size.
     */
    public final Num maxHoldSize() {
        return maxHoldSize;
    }

    /**
     * Return the current hold size of target currency.
     * 
     * @return A current hold size.
     */
    public final Num currentHoldSize() {
        return currentHoldSize;
    }

    /**
     * Create the snapshot of trading log.
     * 
     * @return
     */
    public final TradingLog log() {
        return new TradingLog(market, funds, scenarios, this);
    }

    /**
     * Build your {@link Indicator}.
     * 
     * @param <T>
     * @param span
     * @param calculator
     * @return
     */
    protected final Indicator indicator(Span span, Function<Tick, Num> calculator) {
        return Indicator.build(market.tickers.of(span), calculator);
    }

    /**
     * Create rule which the specified condition is fulfilled during the specified duration.
     * 
     * @param time
     * @param unit
     * @param condition
     * @return
     */
    public static final Predicate<Execution> keep(int time, TemporalUnit unit, BooleanSupplier condition) {
        return keep(time, unit, e -> condition.getAsBoolean());
    }

    /**
     * Create rule which the specified condition is fulfilled during the specified duration.
     * 
     * @param time
     * @param unit
     * @param condition
     * @return
     */
    public static final Predicate<Execution> keep(int time, TemporalUnit unit, Predicate<Execution> condition) {
        AtomicBoolean testing = new AtomicBoolean();
        AtomicReference<ZonedDateTime> last = new AtomicReference(ZonedDateTime.now());

        return e -> {
            if (condition.test(e)) {
                if (testing.get()) {
                    if (e.date.isAfter(last.get())) {
                        testing.set(false);
                        return true;
                    }
                } else {
                    testing.set(true);
                    last.set(e.date.plus(time, unit).minusNanos(1));
                }
            } else {
                if (testing.get()) {
                    if (e.date.isAfter(last.get())) {
                        testing.set(false);
                    }
                }
            }
            return false;
        };
    }

    /**
     * Update snapshot.
     * 
     * @param deltaRealizedProfit
     * @param deltaRemainingSize
     * @param price
     */
    final void updateSnapshot(Num deltaRealizedProfit, Num deltaRemainingSize, Num price) {
        Snapshot letest = snapshots.lastEntry().getValue();

        ZonedDateTime now = market.service.now().plus(59, SECONDS).truncatedTo(MINUTES);
        Num newRealized = letest.realizedProfit.plus(deltaRealizedProfit);
        Num newSize = letest.remainingSize.plus(deltaRemainingSize);
        Num newPrice = newSize.isZero() ? Num.ZERO
                : price == null ? letest.entryPrice
                        : letest.remainingSize.multiply(letest.entryPrice).plus(deltaRemainingSize.multiply(price)).divide(newSize);

        snapshots.put(now, new Snapshot(newRealized, newPrice, newSize));

        // update holding size
        currentHoldSize = newSize;
        if (newSize.abs().isGreaterThan(maxHoldSize)) {
            maxHoldSize = newSize;
        }
    }

    /**
     * The snapshot of {@link Trader}'s state.
     */
    private static class Snapshot implements Profitable {

        /** The realized profit. */
        private final Num realizedProfit;

        /** The entry price. */
        private final Num entryPrice;

        /** The entry size which is . */
        private final Num remainingSize;

        /**
         * Store the current state.
         * 
         * @param realizedProfit
         * @param entryPrice
         * @param entryExecutedUnexitedSize
         */
        private Snapshot(Num realizedProfit, Num entryPrice, Num remainingSize) {
            this.realizedProfit = realizedProfit;
            this.entryPrice = entryPrice;
            this.remainingSize = remainingSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Num realizedProfit() {
            return realizedProfit;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Num unrealizedProfit(Num price) {
            return price.diff(remainingSize.isPositive() ? Direction.BUY : Direction.SELL, entryPrice).multiply(remainingSize);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Num entryRemainingSize() {
            return remainingSize;
        }
    }
}
