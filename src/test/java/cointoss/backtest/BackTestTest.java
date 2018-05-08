/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.backtest;

import java.time.temporal.ChronoUnit;

import cointoss.Side;
import cointoss.Trader;
import cointoss.market.bitflyer.BitFlyerService;
import cointoss.ticker.Tick;
import cointoss.ticker.TickSpan;
import cointoss.util.Num;

/**
 * @version 2018/04/29 14:35:43
 */
class BackTestTest {

    public static void main(String[] args) {
        BackTest.log(() -> BitFlyerService.FX_BTC_JPY.log().at(2017, 6, 1)).currency(100000, 0).strategy(() -> new NOP()).trial(1).run();
    }

    /**
     * @version 2018/04/29 14:43:59
     */
    private static class NOP extends Trader {

        private int update;

        private Num underPrice;

        /**
         * {@inheritDoc}
         */
        @Override
        protected void initialize() {
            // various events
            market.timeline.to(exe -> {
                if (hasPosition() == false) {
                    Entry latest = latest();
                    Side side;

                    if (latest == null) {
                        side = Side.random();
                    } else {
                        side = latest.isWin() ? latest.side() : latest.inverse();
                    }

                    entryMarket(side, maxPositionSize, entry -> {
                        update = 1;
                        underPrice = exe.price.minus(entry, 4000);

                        // cancel timing
                        market.timeline.takeUntil(completingEntry)
                                .take(keep(5, ChronoUnit.MINUTES, entry.order::isNotCompleted))
                                .take(1)
                                .mapTo(entry.order)
                                .to(t -> {
                                    System.out.println("cancel " + entry.order);
                                    cancel(entry);
                                });

                        // rise under price line
                        market.tickerBy(TickSpan.Second10).add.takeUntil(closingPosition) //
                                .map(Tick::getClosePrice)
                                .takeAt(i -> i % 5 == 0)
                                .to(e -> {
                                    Num next = e.minus(entry, Math.max(0, 4000 - update * 200));

                                    if (next.isGreaterThan(entry, underPrice)) {
                                        entry.log("最低価格を%sから%sに再設定 参考値%s", underPrice, next, e);
                                        update++;
                                        underPrice = next;
                                    }
                                });

                        // loss cut
                        market.timeline.takeUntil(closingPosition) //
                                .take(keep(5, ChronoUnit.SECONDS, e -> e.price.isLessThan(entry, underPrice)))
                                .take(1)
                                .to(e -> {
                                    entry.exitLimit(entry.entrySize(), underPrice, exit -> {
                                        entry.log("10秒以上約定値が%s以下になったので指値で決済開始", underPrice);

                                        market.timeline.takeUntil(completingEntry)
                                                .take(keep(30, ChronoUnit.SECONDS, exit::isNotCompleted))
                                                .take(1)
                                                .to(x -> {
                                                    market.cancel(exit).to(() -> {
                                                        entry.log("30秒待っても処理されないので指値をキャンセルして成行決済 " + exit.remaining);
                                                        // entry.exitMarket(exit.outstanding_size);
                                                    });
                                                });
                                    });
                                });
                    });
                }
            });
        }
    }
}