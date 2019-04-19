/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.analyze;

import org.junit.jupiter.api.Test;

import cointoss.util.Num;

class NumSummaryTest {

    @Test
    void mean() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.mean().is(10);

        summary.add(Num.of(20));
        assert summary.mean().is(15);

        summary.add(Num.of(30));
        assert summary.mean().is(20);
    }

    @Test
    void total() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.total().is(10);

        summary.add(Num.of(20));
        assert summary.total().is(30);

        summary.add(Num.of(30));
        assert summary.total().is(60);
    }

    @Test
    void size() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.size() == 1;

        summary.add(Num.of(20));
        assert summary.size() == 2;

        summary.add(Num.of(30));
        assert summary.size() == 3;
    }

    @Test
    void min() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.min().is(10);

        summary.add(Num.of(20));
        assert summary.min().is(10);

        summary.add(Num.of(5));
        assert summary.min().is(5);
    }

    @Test
    void max() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.max().is(10);

        summary.add(Num.of(20));
        assert summary.max().is(20);

        summary.add(Num.of(5));
        assert summary.max().is(20);
    }

    @Test
    void variance() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.variance().is(0);

        summary.add(Num.of(20));
        assert summary.variance().is(25);

        summary.add(Num.of(30));
        assert summary.variance().is(66.6666666666667);
    }

    @Test
    void standardDeviation() {
        NumSummary summary = new NumSummary();
        summary.add(Num.of(10));
        assert summary.standardDeviation().is(0);

        summary.add(Num.of(20));
        assert summary.standardDeviation().is(5);

        summary.add(Num.of(30));
        assert summary.standardDeviation().is(8.16496580927726);
    }
}
