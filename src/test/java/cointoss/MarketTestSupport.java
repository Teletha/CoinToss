/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import cointoss.util.Num;

/**
 * @version 2018/04/29 21:47:23
 */
public class MarketTestSupport {

    /** The execution id manager. */
    private static final AtomicLong executionId = new AtomicLong();

    /**
     * Create {@link Execution}.
     * 
     * @param price
     * @param size
     * @return
     */
    public static Execution buy(double price, double size) {
        return buy(Num.of(price), Num.of(size));
    }

    /**
     * Create {@link Execution}.
     * 
     * @param price
     * @param size
     * @return
     */
    public static Execution buy(Num price, Num size) {
        return execution(Side.BUY, price, size);
    }

    /**
     * Create {@link Execution}.
     * 
     * @param price
     * @param size
     * @return
     */
    public static Execution sell(double price, double size) {
        return sell(Num.of(price), Num.of(size));
    }

    /**
     * Create {@link Execution}.
     * 
     * @param price
     * @param size
     * @return
     */
    public static Execution sell(Num price, Num size) {
        return execution(Side.SELL, price, size);
    }

    /**
     * Create {@link Execution}.
     * 
     * @param side
     * @param price
     * @param size
     * @return
     */
    public static Execution execution(Side side, double price, double size) {
        return execution(side, Num.of(price), Num.of(size));
    }

    /**
     * Create {@link Execution}.
     * 
     * @param side
     * @param price
     * @param size
     * @return
     */
    public static Execution execution(Side side, Num price, Num size) {
        Execution exe = new Execution();
        exe.id = executionId.getAndIncrement();
        exe.side = Objects.requireNonNull(side);
        exe.price = Objects.requireNonNull(price);
        exe.size = exe.cumulativeSize = Objects.requireNonNull(size);
        exe.exec_date = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        return exe;
    }

    /**
     * Create {@link Execution}.
     * 
     * @param price
     * @param size
     * @return
     */
    public static List<Execution> executionSerially(int count, Side side, double price, double size) {
        List<Execution> list = new ArrayList();

        for (int i = 0; i < count; i++) {
            Execution e = execution(side, price, size);
            if (i != 0) e.consecutive = side.isBuy() ? Execution.ConsecutiveSameBuyer : Execution.ConsecutiveSameSeller;
            list.add(e);
        }
        return list;
    }

    /**
     * Create {@link Position}.
     * 
     * @param side
     * @param price
     * @param size
     * @return
     */
    public static Position position(Side side, double price, double size) {
        return position(side, Num.of(price), Num.of(size));
    }

    /**
     * Create {@link Position}.
     * 
     * @param side
     * @param price
     * @param size
     * @return
     */
    public static Position position(Side side, Num price, Num size) {
        Position position = new Position();
        position.date = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        position.side = Objects.requireNonNull(side);
        position.price = Objects.requireNonNull(price);
        position.size.set(Objects.requireNonNull(size));

        return position;
    }
}