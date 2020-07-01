/*
 * Copyright (C) 2020 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

import java.util.List;

import org.junit.jupiter.api.Test;

import cointoss.order.Order;
import cointoss.trade.FundManager;
import cointoss.trade.Trader;
import cointoss.verify.VerifiableMarket;

class MarketTest {

    private VerifiableMarket market = new VerifiableMarket();

    @Test
    void stopWithoutPosition() {
        List<Order> orders = market.stop().toList();

        assert orders.isEmpty();
    }

    @Test
    void registerCallDeclare() {
        CallDeclare trader = new CallDeclare();
        market.register(trader);
        assert trader.declaredMarket == market;
    }

    private static class CallDeclare extends Trader {

        private Market declaredMarket;

        @Override
        protected void declare(Market market, FundManager fund) {
            this.declaredMarket = market;
        }
    }
}