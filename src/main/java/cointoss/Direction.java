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

import java.util.Objects;

import cointoss.util.Generator;
import kiss.Decoder;
import kiss.Encoder;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2017/07/21 19:47:35
 */
public enum Direction implements Directional {
    BUY, SELL;

    /**
     * Return ID.
     * 
     * @return
     */
    public String mark() {
        return this == BUY ? "B" : "S";
    }

    /**
     * @return
     */
    @Override
    public Direction inverse() {
        return this == BUY ? SELL : BUY;
    }

    /**
     * Helper to detect.
     * 
     * @return
     */
    @Override
    public boolean isBuy() {
        return this == BUY;
    }

    /**
     * Helper to detect.
     * 
     * @return
     */
    @Override
    public boolean isSell() {
        return this == SELL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction direction() {
        return this;
    }

    /**
     * @param position
     * @return
     */
    public boolean isPair(Direction position) {
        return this != Objects.requireNonNull(position);
    }

    /**
     * @param position
     * @return
     */
    public boolean isPair(String position) {
        return isPair(parse(position));
    }

    /**
     * @param position
     * @return
     */
    public boolean isSame(Direction position) {
        return this == Objects.requireNonNull(position);
    }

    /**
     * @param position
     * @return
     */
    public boolean isSame(String position) {
        return isSame(parse(position));
    }

    /**
     * @return
     */
    public static Direction random() {
        return Generator.random(Direction.class);
    }

    /**
     * <p>
     * Parse by value.
     * </p>
     * 
     * @param value
     * @return
     */
    public static Direction parse(String value) {
        return value == null || value.startsWith("S") ? SELL : BUY;
    }

    /**
     * 
     */
    @Manageable(lifestyle = Singleton.class)
    private static class Codec implements Encoder<Direction>, Decoder<Direction> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Direction decode(String value) {
            return parse(value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String encode(Direction value) {
            return value.mark();
        }
    }
}