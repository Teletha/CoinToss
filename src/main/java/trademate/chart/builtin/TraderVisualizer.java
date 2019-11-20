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

import static cointoss.ticker.Span.Minute1;

import cointoss.Market;
import cointoss.Trader;
import cointoss.ticker.Indicator;
import cointoss.ticker.Ticker;
import cointoss.util.Num;
import stylist.Style;
import stylist.StyleDSL;
import stylist.value.Color;
import trademate.chart.PlotScript;

public class TraderVisualizer extends PlotScript implements StyleDSL {

    public Style profit = () -> {
        stroke.color(Color.rgb(158, 208, 221));
    };

    public Style realized = () -> {
        stroke.color(Color.rgb(201, 216, 150));
    };

    public Style unrealized = () -> {
        stroke.color(Color.rgb(201, 216, 150)).dashArray(1, 6);
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare(Market market, Ticker ticker) {
        int scale = market.service.setting.baseCurrencyScaleSize;

        Indicator<TraderState> indicator = Indicator.build(market.tickers.of(Minute1), tick -> {
            Num realized = Num.ZERO;
            Num unrealized = Num.ZERO;
            Num price = market.tickers.latestPrice.v;

            for (Trader trader : market.traders) {
                realized = realized.plus(trader.realizedProfit());
                unrealized = unrealized.plus(trader.unrealizedProfit(price));
            }
            return new TraderState(realized.scale(scale), unrealized.scale(scale));
        }).memoize();

        up.line(indicator.map(s -> s.realized), realized);
        up.line(indicator.map(s -> s.unrealized), unrealized);
        up.line(indicator.map(s -> s.profit), profit);
    }

    /**
     * 
     */
    public static class TraderState {

        /** The realized profit. */
        public final Num realized;

        /** The unrealized profit. */
        public final Num unrealized;

        /** The total profit. */
        public final Num profit;

        /**
         * @param realizedProfit
         * @param unrealizedProfit
         */
        private TraderState(Num realizedProfit, Num unrealizedProfit) {
            this.realized = realizedProfit;
            this.unrealized = unrealizedProfit;
            this.profit = realizedProfit.plus(unrealizedProfit);
        }
    }
}