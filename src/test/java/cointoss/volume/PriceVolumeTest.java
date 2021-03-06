/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.volume;

import org.junit.jupiter.api.Test;

import cointoss.util.arithmetic.Num;

class PriceVolumeTest {

    @Test
    void volumeAt() {
        PriceRangedVolumePeriod volume = new PriceRangedVolumePeriod(0, Num.of(2000), Num.of(10));
        for (int i = 0; i < 4000; i++) {
            volume.update(Num.of(i), 1);
        }

        for (int i = 0; i < 4000; i++) {
            assert volume.volumeAt(i) == 10;
        }
    }

    @Test
    void volumeAtWithDecimal() {
        PriceRangedVolumePeriod volume = new PriceRangedVolumePeriod(0, Num.of(0.2), Num.of(0.001));
        for (int i = 0; i < 4000; i++) {
            volume.update(Num.of(i * 0.0001), 1);
        }

        for (int i = 0; i < 4000; i++) {
            assert volume.volumeAt(i * 0.0001F) == 10;
        }
    }
}