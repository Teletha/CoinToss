/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.ticker.data;

import java.time.ZonedDateTime;

import cointoss.Direction;
import cointoss.Directional;
import cointoss.util.arithmetic.Num;
import icy.manipulator.Icy;

@Icy
interface LiquidationModel extends TimeseriesData, Directional {

    /**
     * {@inheritDoc}
     */
    @Override
    @Icy.Property
    ZonedDateTime date();

    @Override
    @Icy.Property
    Direction direction();

    @Icy.Property
    double size();

    @Icy.Property
    Num price();
}