/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util;

import static java.time.temporal.ChronoUnit.MILLIS;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

import kiss.Observer;
import kiss.WiseFunction;

class RetryPolicyTest {

    ErrorNetwork network = new ErrorNetwork();

    WiseFunction<String, String> throwError = e -> {
        throw new Error();
    };

    @Test
    void limit() {
        Retry rety = Retry.with.limit(3);
        Result result = new Result();

        network.rest(null).retryWhen(rety).to(result);
        assert rety.count == 3;
        assert result.error != null;
    }

    @Test
    void delay() {
        Retry rety = Retry.with.limit(2).delay(100, MILLIS);
        Result result = new Result();

        network.rest(null).retryWhen(rety).to(result);
        assert rety.count == 3;
        assert result.error != null;
    }

    /**
     * 
     */
    private static class Result implements Observer<JsonElement> {

        private JsonElement value;

        private Throwable error;

        private boolean complete;

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept(JsonElement t) {
            value = t;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void complete() {
            complete = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void error(Throwable e) {
            error = e;
        }
    }
}