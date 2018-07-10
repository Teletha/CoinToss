/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

/**
 * @version 2017/08/20 18:46:21
 */
public interface Directional {

    /**
     * Utility to detect.
     * 
     * @return
     */
    default boolean isBuy() {
        return side() == Side.BUY;
    }

    /**
     * Utility to detect.
     * 
     * @return
     */
    default boolean isSell() {
        return side() == Side.SELL;
    }

    /**
     * Utility to inverse {@link Side}.
     * 
     * @return
     */
    default Side inverse() {
        return side() == Side.BUY ? Side.SELL : Side.BUY;
    }

    /**
     * Get {@link Side}.
     * 
     * @return
     */
    Side side();
}
