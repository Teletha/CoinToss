/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util.arithmetic;

import java.math.BigDecimal;

import cointoss.Direction;
import kiss.Variable;

class ArithmeticIsGreaterThanOrEqualTest extends ArithmeticTestSupport {

    @ArithmeticTest
    void primitiveInt(int one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void primitiveInt(int one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }

    @ArithmeticTest
    void primitiveLong(long one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void primitiveLong(long one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }

    @ArithmeticTest
    void primitiveDouble(double one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void primitiveDouble(double one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }

    @ArithmeticTest
    void numeralString(String one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void numeralString(String one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }

    @ArithmeticTest
    void number(Num one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void number(Num one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }

    @ArithmeticTest
    void numberVariable(Variable<Num> one) {
        assert Num.ZERO.isGreaterThanOrEqual(one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
    }

    @ArithmeticTest
    void numberVariable(Variable<Num> one, Direction side) {
        if (side.isBuy()) {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == 0 <= BigDecimal.ZERO.compareTo(big(one));
        } else {
            assert Num.ZERO.isGreaterThanOrEqual(side, one) == BigDecimal.ZERO.compareTo(big(one)) <= 0;
        }
    }
}