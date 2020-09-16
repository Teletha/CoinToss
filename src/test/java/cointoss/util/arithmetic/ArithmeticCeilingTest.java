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
import org.junit.jupiter.api.Test;

class ArithmeticCeilingTest extends ArithmeticTestSupport {

    @Test
    void ceiling() {
        assert Num.of(100).ceiling(Num.of(30)).is(120);
        assert Num.of(100).ceiling(Num.of(1)).is(100);
        assert Num.of(100).ceiling(Num.of(0.1)).is(100);
        assert Num.of(100).ceiling(Num.of(0.3)).is(100.2);
    }

    @ArithmeticTest
    void ceiling(int value) {
        if (!zeroIsEqualTo(value)) {
            assert equality(Num.of(100).ceiling(Num.of(value)), BigDecimals.ceiling(big(100), big(value)));
        } else {
            Assertions.assertThrows(ArithmeticException.class, () -> Num.of(100).ceiling(Num.of(value)));
        }
    }

    @ArithmeticTest
    void ceiling(double value) {
        if (!zeroIsEqualTo(value)) {
            assert equalityVaguely(Num.of(100).ceiling(Num.of(value)), BigDecimals.ceiling(big(100), big(value)));
        } else {
            Assertions.assertThrows(ArithmeticException.class, () -> Num.of(100).ceiling(Num.of(value)));
        }
    }
}