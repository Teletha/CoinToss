/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.order;

/**
 * @version 2018/07/08 10:36:09
 */
public enum QuantityConditionsEnforcement {
    GoodTillCanceled("GTC"), ImmediateOrCancel("IOC"), FillOrKill("FOK");

    /** A standard abbreviation. */
    public final String abbreviation;

    /**
     * @param abbreviation
     */
    private QuantityConditionsEnforcement(String abbreviation) {
        this.abbreviation = abbreviation;
    }
}