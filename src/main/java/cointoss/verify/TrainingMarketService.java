/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.verify;

import java.lang.reflect.Method;

import cointoss.MarketService;
import cointoss.execution.Execution;
import cointoss.order.Order;
import cointoss.order.OrderBookPageChanges;
import cointoss.order.OrderState;
import cointoss.util.EfficientWebSocket;
import cointoss.util.arithmetic.Num;
import kiss.I;
import kiss.Signal;

public class TrainingMarketService extends MarketService {

    private final MarketService backend;

    private final VerifiableMarketService frontend;

    /**
     * @param backend
     */
    public TrainingMarketService(MarketService backend) {
        super(backend.exchange, backend.marketName, backend.setting);
        this.backend = backend;
        this.frontend = new VerifiableMarketService(backend);
    }

    /**
     * Helper to delegate the internal method by reflection.
     * 
     * @param <T>
     * @param type
     * @param name
     * @return
     */
    private <T> T delegateInternal(Class<T> type, String name) {
        try {
            Method method = MarketService.class.getDeclaredMethod("clientRealtimely");
            method.setAccessible(true);
            return (T) method.invoke(backend);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EfficientWebSocket clientRealtimely() {
        return delegateInternal(EfficientWebSocket.class, "clientRealtimely");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Integer> delay() {
        return backend.delay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> request(Order order) {
        return frontend.request(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> cancel(Order order) {
        return frontend.cancel(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executions(long startId, long endId) {
        return backend.executions(startId, endId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Execution> connectExecutionRealtimely() {
        return delegateInternal(Signal.class, "connectExecutionRealtimely");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatest() {
        return backend.executionLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatestAt(long id) {
        return backend.executionLatestAt(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders() {
        return frontend.orders();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders(OrderState state) {
        return frontend.orders(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Order> connectOrdersRealtimely() {
        return frontend.connectOrdersRealtimely();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<OrderBookPageChanges> orderBook() {
        return backend.orderBook();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<OrderBookPageChanges> connectOrderBookRealtimely() {
        return delegateInternal(Signal.class, "connectOrderBookRealtimely");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> baseCurrency() {
        return backend.baseCurrency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> targetCurrency() {
        return backend.targetCurrency();
    }
}
