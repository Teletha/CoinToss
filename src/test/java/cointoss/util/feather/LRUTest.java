/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util.feather;

import org.junit.jupiter.api.Test;

class LRUTest {

    @Test
    void access() {
        LRU cache = new LRU(3);
        cache.access(1);
        assert cache.eldest() == 1;
        assert cache.latest() == 1;

        cache.access(2);
        assert cache.eldest() == 1;
        assert cache.latest() == 2;

        cache.access(3);
        assert cache.eldest() == 1;
        assert cache.latest() == 3;
    }

    @Test
    void overSameValue1() {
        LRU cache = new LRU(3);
        cache.access(1);
        cache.access(2);
        cache.access(3);
        cache.access(1);

        assert cache.eldest() == 2;
        assert cache.latest() == 1;
    }

    @Test
    void overSameValue2() {
        LRU cache = new LRU(3);
        cache.access(1);
        cache.access(2);
        cache.access(3);
        cache.access(2);
        assert cache.eldest() == 1;
        assert cache.latest() == 2;

        cache.access(1);
        assert cache.eldest() == 3;
        assert cache.latest() == 1;
    }

    @Test
    void overSameValue3() {
        LRU cache = new LRU(3);
        cache.access(1);
        cache.access(2);
        cache.access(3);
        cache.access(3);

        assert cache.eldest() == 1;
        assert cache.latest() == 3;
    }

    @Test
    void overNewValue() {
        LRU cache = new LRU(3);
        assert cache.access(1) == -1;
        assert cache.access(2) == -1;
        assert cache.access(3) == -1;
        assert cache.access(4) == 1;
        assert cache.eldest() == 2;
        assert cache.latest() == 4;

        assert cache.access(5) == 2;
        assert cache.eldest() == 3;
        assert cache.latest() == 5;
    }
}
