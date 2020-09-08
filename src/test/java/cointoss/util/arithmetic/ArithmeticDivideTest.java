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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;

import com.google.common.math.DoubleMath;

import kiss.Variable;

class ArithmeticDivideTest extends ArithmeticTestSupport {

    @ArithmeticTest
    void primitiveInt(int one, int other) {
        if (other != 0) {
            assert equalityVaguely(Num.of(one).divide(other), big(one).divide(big(other), Num.CONTEXT));
        } else {
            Assertions.assertThrows(ArithmeticException.class, () -> Num.of(one).divide(other));
        }
    }

    @ArithmeticTest
    void primitiveLong(long one, long other) {
        if (other != 0) {
            assert equalityVaguely(Num.of(one).divide(other), big(one).divide(big(other), Num.CONTEXT));
        } else {
            Assertions.assertThrows(ArithmeticException.class, () -> Num.of(one).divide(other));
        }
    }

    @ArithmeticTest
    void primitiveDouble(double one, double other) {
        if (DoubleMath.fuzzyEquals(other, 0, 0.0001)) {
            assert equalityVaguely(Num.of(one).divide(other), big(one).divide(big(other), Num.CONTEXT));
        } else {
            Assertions.assertThrows(ArithmeticException.class, () -> Num.of(one).divide(other));
        }
    }

    @Disabled
    @ArithmeticTest
    void numeralString(String one, String other) {
        assert equalityVaguely(Num.of(one).divide(other), big(one).divide(big(other)));
    }

    @Disabled
    @ArithmeticTest
    void number(Num value) {
        assert Num.ONE.divide(value).equals(value);
    }

    @Disabled
    @ArithmeticTest
    void number(Num one, Num other) {
        assert equalityVaguely(one.divide(other), big(one).divide(big(other)));
    }

    @Disabled
    @ArithmeticTest
    void numberVariable(Variable<Num> one, Variable<Num> other) {
        assert equalityVaguely(one.v.divide(other), big(one).divide(big(other)));
    }
}
