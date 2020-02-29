/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.trade.extension;

import java.util.function.Consumer;

import cointoss.order.OrderStrategy.Orderable;
import cointoss.util.Num;
import kiss.WiseBiConsumer;

public enum StrategyPart implements TradePart {

    /** Make order. */
    Make(Orderable::make),

    /** Take order. */
    Take((o, p) -> o.take());

    private final WiseBiConsumer<Orderable, Num> strategy;

    private StrategyPart(WiseBiConsumer<Orderable, Num> strrategy) {
        this.strategy = strrategy;
    }

    public Consumer<Orderable> at(Num price) {
        return strategy.bindLast(price);
    }
}