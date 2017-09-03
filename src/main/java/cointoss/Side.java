/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import java.util.Objects;

import kiss.Decoder;
import kiss.Encoder;

/**
 * @version 2017/07/21 19:47:35
 */
public enum Side implements Directional {
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
    public Side inverse() {
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
    public Side side() {
        return this;
    }

    /**
     * @param position
     * @return
     */
    public boolean isPair(Side position) {
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
    public boolean isSame(Side position) {
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
    public static Side random() {
        return Generator.random(Side.class);
    }

    /**
     * <p>
     * Parse by value.
     * </p>
     * 
     * @param value
     * @return
     */
    public static Side parse(String value) {
        return value == null || value.startsWith("S") ? SELL : BUY;
    }

    /**
     * @version 2017/08/23 0:08:13
     */
    @SuppressWarnings("unused")
    private static class Codec implements Encoder<Side>, Decoder<Side> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Side decode(String value) {
            return parse(value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String encode(Side value) {
            return value.mark();
        }
    }
}
