/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package trademate.chart.builtin;

import cointoss.Market;
import cointoss.ticker.Indicator;
import cointoss.ticker.Span;
import cointoss.ticker.Tick;
import cointoss.ticker.Ticker;
import kiss.Variable;
import stylist.Style;
import stylist.StyleDSL;
import stylist.value.Color;
import trademate.chart.PlotScript;

public class SMAIndicator extends PlotScript implements StyleDSL {

    public final Variable<Integer> shortDays = Variable.of(21);

    public final Variable<Integer> longDays = Variable.of(75);

    private double alpha = 0.5;

    public Style shortSMA = () -> {
        stroke.color(Color.rgb(181, 212, 53, alpha));
    };

    public Style longSMA = () -> {
        stroke.color(Color.rgb(54, 78, 161, alpha));
    };

    public Style SMA30M = () -> {
        stroke.color(Color.rgb(107, 191, 71, alpha));
    };

    public Style SMA1H = () -> {
        stroke.color(Color.rgb(17, 132, 66, alpha));
    };

    public Style SMA4H = () -> {
        stroke.color(Color.rgb(57, 130, 195, alpha));
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare(Market market, Ticker ticker) {
        overlay.line(Indicator.build(ticker, Tick::closePrice).sma(shortDays).scale(baseScale), shortSMA);
        overlay.line(Indicator.build(market.tickers.of(Span.Minute30), Tick::closePrice).sma(shortDays).scale(baseScale), SMA30M);
        overlay.line(Indicator.build(market.tickers.of(Span.Hour1), Tick::closePrice).sma(shortDays).scale(baseScale), SMA1H);
        overlay.line(Indicator.build(market.tickers.of(Span.Hour4), Tick::closePrice).sma(shortDays).scale(baseScale), SMA4H);
        overlay.line(Indicator.build(ticker, Tick::closePrice).sma(longDays).scale(baseScale), longSMA);
    }
}