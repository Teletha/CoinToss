/*
 * Copyright (C) 2019 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.verify;

import static java.time.temporal.ChronoUnit.MINUTES;

import cointoss.Direction;
import cointoss.Market;
import cointoss.market.bitflyer.BitFlyer;
import cointoss.ticker.Indicator;
import cointoss.ticker.Span;
import cointoss.trade.Scenario;
import cointoss.trade.Trader;
import kiss.I;

public class BackTestInvoker {

    public static void main(String[] args) throws InterruptedException {
        BackTest.with.service(BitFlyer.FX_BTC_JPY)
                .start(2019, 11, 9)
                .end(2019, 11, 9)
                .traders(Sample::new, Sample::new, Sample::new)
                .initialBaseCurrency(3000000)
                .run();
    }

    /**
     * 
     */
    private static class Sample extends Trader {

        Indicator losscutRange = indicator(Span.Minute5, tick -> tick.highPrice().minus(tick.lowPrice()).multiply(0.75)).sma(5);

        private Sample(Market market) {
            super(market);

            when(market.tickers.of(Span.Minute5).add.skip(12), tick -> new Scenario() {

                @Override
                protected void entry() {
                    entry(Direction.random(), 0.1, s -> s.make(market.tickers.latestPrice.v).cancelAfter(3, MINUTES));
                }

                @Override
                protected void exit() {
                    exitAt(entryPrice.plus(this, 6400));
                    exitAt(market.tickers.of(Span.Second5).add.flatMap(tick -> {
                        if (tick.openPrice.isGreaterThan(this, entryPrice.plus(this, 2800))) {
                            return I.signal(entryPrice.plus(this, 500));
                        } else {
                            return I.signal();
                        }
                    }).first().startWith(entryPrice.minus(this, losscutRange.last())).to());
                    // exitAt(trailing2(up -> entryPrice.minus(this, 1300).plus(this, up)));
                }
            });
        }
    }
}
