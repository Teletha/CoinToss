/*
 * Copyright (C) 2020 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.chart.builtin;

import cointoss.Market;
import cointoss.ticker.DoubleIndicator;
import cointoss.ticker.Tick;
import cointoss.ticker.Ticker;
import stylist.Style;
import stylist.StyleDSL;
import trademate.CommonText;
import trademate.chart.ChartStyles;
import trademate.chart.PlotArea;
import trademate.chart.PlotScript;

public class VolumeIndicator extends PlotScript implements StyleDSL {

    public Style Long = () -> {
        stroke.color(ChartStyles.buy);
    };

    public Style Short = () -> {
        stroke.color(ChartStyles.sell.opacify(-0.49));
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare(Market market, Ticker ticker) {
        // volume
        int volumeScale = market.service.setting.targetCurrencyMinimumBidSize.scale();

        DoubleIndicator buyVolume = DoubleIndicator.build(ticker, Tick::longVolume);
        DoubleIndicator sellVolume = DoubleIndicator.build(ticker, Tick::shortVolume);

        in(PlotArea.Bottom, () -> {
            line(buyVolume.scale(volumeScale).name(CommonText.Buy), Long);
            line(sellVolume.scale(volumeScale).name(CommonText.Sell), Short);
        });
    }
}