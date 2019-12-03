/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.trading;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import com.google.common.base.Predicate;

import cointoss.Direction;
import cointoss.FundManager;
import cointoss.Market;
import cointoss.Scenario;
import cointoss.Trader;
import cointoss.ticker.Indicator;
import cointoss.ticker.Span;
import cointoss.ticker.Tick;
import cointoss.ticker.Ticker;
import cointoss.util.Num;
import kiss.Signal;
import stylist.Style;
import stylist.StyleDSL;
import trademate.TradeMateStyle;
import trademate.chart.PlotScript;

/**
 * 
 */
public class VolumeCross extends Trader {

    public int smaLength = 3;

    Indicator<Num> volumeDiff;

    Indicator<Boolean> upPrediction;

    Indicator<Boolean> downPrediction;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare(Market market, FundManager fund) {
        Indicator<Num> buyVolume = Indicator.build(market.tickers.of(Span.Minute5), Tick::buyVolume);
        Indicator<Num> sellVolume = Indicator.build(market.tickers.of(Span.Minute5), Tick::sellVolume);
        volumeDiff = buyVolume.map(sellVolume, (b, s) -> b.minus(s)).scale(market.service.setting.targetCurrencyScaleSize).sma(7);
        upPrediction = Indicator.build(market.tickers.of(Span.Minute5), Tick::isBear).map(volumeDiff, (t, d) -> t && d.isPositive());
        downPrediction = Indicator.build(market.tickers.of(Span.Minute5), Tick::isBull).map(volumeDiff, (t, d) -> t && d.isNegative());

        // disableWhile(observeProfit().map(p -> p.isLessThan(-10000)));

        double size = 0.3;

        when(volumeDiff.observe().plug(near(5, o -> o.isGreaterThan(0))), v -> new Scenario() {
            @Override
            protected void entry() {
                entry(Direction.BUY, size, o -> o.make(market.tickers.latestPrice.v.minus(300)).cancelAfter(3, ChronoUnit.MINUTES));
            }

            @Override
            protected void exit() {
                exitWhen(volumeDiff.observe().plug(near(2, o -> o.isLessThan(0))), o -> o.take());
            }
        });

        when(volumeDiff.observe().plug(near(5, o -> o.isLessThan(0))), v -> new Scenario() {
            @Override
            protected void entry() {
                entry(Direction.SELL, size, o -> o.make(market.tickers.latestPrice.v.plus(300)).cancelAfter(3, ChronoUnit.MINUTES));
            }

            @Override
            protected void exit() {
                exitWhen(volumeDiff.observe().plug(near(2, o -> o.isGreaterThan(0))), o -> o.take());
            }
        });
    }

    private <In> Function<Signal<In>, Signal<List<In>>> near(int size, Predicate<In> condition) {
        return signal -> signal.buffer(size, 1).take(buff -> buff.stream().allMatch(condition));
    }

    /**
     * 
     */
    class Plot extends PlotScript implements StyleDSL {

        Style diff = () -> {
            stroke.color("#eee");
        };

        Style upMark = () -> {
            fill.color(TradeMateStyle.BUY);
        };

        Style downMark = () -> {
            fill.color(TradeMateStyle.SELL);
        };

        @Override
        protected void declare(Market market, Ticker ticker) {
            lowN.line(volumeDiff, diff);
            main.mark(upPrediction, upMark);
            main.mark(downPrediction, downMark);
        }
    }
}