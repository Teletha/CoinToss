/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.ticker;

import java.time.ZonedDateTime;

import org.magicwerk.brownies.collections.GapList;

import cointoss.Execution;
import cointoss.util.Num;
import kiss.Signal;

/**
 * @version 2018/01/30 12:36:31
 */
public class RealtimeTicker {

    /** The latest execution. */
    public Execution latest = new Execution();

    /** The volume. */
    public Num volume = Num.ZERO;

    /** Volume of the period */
    public Num longVolume = Num.ZERO;

    /** Volume of the period */
    public Num longPriceIncrese = Num.ZERO;

    /** Volume of the period */
    public Num shortVolume = Num.ZERO;

    /** Volume of the period */
    public Num shortPriceDecrease = Num.ZERO;

    /** The recorder. */
    private final GapList<Execution> buffer = GapList.create();

    /**
     * 
     */
    public RealtimeTicker(TickSpan span, Signal<Execution> signal) {
        signal.to(incoming -> {
            // incoming
            volume = volume.plus(incoming.size);

            if (incoming.side.isBuy()) {
                longVolume = longVolume.plus(incoming.size);
                longPriceIncrese = longPriceIncrese.plus(incoming.price.minus(latest.price));
            } else {
                shortVolume = shortVolume.plus(incoming.size);
                shortPriceDecrease = shortPriceDecrease.plus(latest.price.minus(incoming.price));
            }

            // outgoing
            ZonedDateTime threshold = incoming.exec_date.minus(span.duration);
            Execution first = buffer.peek();

            while (first.exec_date.isBefore(threshold)) {
                Execution outgoing = buffer.remove();
                Execution second = buffer.peek();

                volume = volume.minus(outgoing.size);

                if (outgoing.side.isBuy()) {
                    longVolume = longVolume.minus(outgoing.size);
                    longPriceIncrese = longPriceIncrese.minus(second.price.minus(outgoing.price));
                } else {
                    shortVolume = shortVolume.minus(outgoing.size);
                    shortPriceDecrease = shortPriceDecrease.minus(outgoing.price.minus(second.price));
                }

                // check next
                first = second;
            }

            // update latest
            latest = incoming;
        });
    }

    /**
     * Compute volume diff.
     * 
     * @return
     */
    public Num volume() {
        return longVolume.minus(shortVolume);
    }

    /**
     * Compute indicator.
     * 
     * @return
     */
    public Num estimateUpPotential() {
        return longVolume.isZero() ? Num.ZERO : longPriceIncrese.divide(longVolume).scale(3);
    }

    /**
     * Compute indicator.
     * 
     * @return
     */
    public Num estimateDownPotential() {
        return shortVolume.isZero() ? Num.ZERO : shortPriceDecrease.divide(shortVolume).scale(3);
    }
}