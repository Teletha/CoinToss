/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.indicator.simple;

import cointoss.Amount;
import cointoss.TimeSeries;
import cointoss.indicator.AbstractIndicator;

/**
 * @version 2017/08/25 22:47:25
 */
public class TypicalPriceIndicator extends AbstractIndicator<Amount> {

    private static final long serialVersionUID = 1680028662640659379L;

    /**
     * @param series
     */
    public TypicalPriceIndicator(TimeSeries series) {
        super(series);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Amount calculate(int index) {
        return series.getTick(index).getTypical();
    }
}
