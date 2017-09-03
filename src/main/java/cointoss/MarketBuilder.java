/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss;

import kiss.Signal;

/**
 * @version 2017/08/16 8:11:25
 */
public interface MarketBuilder {

    /**
     * Read the initial execution data.
     * 
     * @return
     */
    Signal<Execution> initialize();
}
