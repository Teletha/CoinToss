/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.order;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import cointoss.Direction;
import cointoss.Directional;
import cointoss.execution.Execution;
import cointoss.util.Num;
import icy.manipulator.Icy;
import kiss.I;
import kiss.Signal;
import kiss.Signaling;
import kiss.Variable;

@Icy(grouping = 2, classicSetterModifier = "final")
public abstract class OrderModel implements Directional {

    /** The total cost. */
    final Variable<Num> cost = Variable.of(Num.ZERO);

    /** The relation holder. */
    private Map<Class, Object> relations;

    /** The entry holder. */
    private final LinkedList<Execution> entries = new LinkedList();

    /**
     * {@inheritDoc}
     */
    @Icy.Property
    @Override
    public abstract Direction direction();

    @Icy.Intercept("direction")
    private Direction validate(Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Required");
        }
        return direction;
    }

    /**
     * The initial ordered size.
     * 
     * @return
     */
    @Icy.Property
    public abstract Num size();

    /**
     * Set order size by value.
     * 
     * @param size An executed size.
     * @return Chainable API.
     */
    @Icy.Overload("size")
    private Num size(int size) {
        return Num.of(size);
    }

    /**
     * Set order size by value.
     * 
     * @param size An executed size.
     * @return Chainable API.
     */
    @Icy.Overload("size")
    private Num size(float size) {
        return Num.of(size);
    }

    /**
     * Set order size by value.
     * 
     * @param size An executed size.
     * @return Chainable API.
     */
    @Icy.Overload("size")
    private Num size(long size) {
        return Num.of(size);
    }

    /**
     * Set order size by value.
     * 
     * @param size An executed size.
     * @return Chainable API.
     */
    @Icy.Overload("size")
    private Num size(double size) {
        return Num.of(size);
    }

    /**
     * Size validation.
     * 
     * @param size
     * @return
     */
    @Icy.Intercept("size")
    private Num validateSize(Num size, Consumer<Num> remainingSize) {
        if (size.isNegativeOrZero()) {
            throw new IllegalArgumentException("Order size must be positive.");
        }
        remainingSize.accept(size);
        return size;
    }

    /**
     * The initial ordered price.
     * 
     * @return
     */
    @Icy.Property(mutable = true)
    public Num price() {
        return Num.ZERO;
    }

    /**
     * Set price by value.
     * 
     * @param price A price.
     * @return Chainable API.
     */
    @Icy.Overload("price")
    private Num price(int price) {
        return Num.of(price);
    }

    /**
     * Set price by value.
     * 
     * @param price A price.
     * @return Chainable API.
     */
    @Icy.Overload("price")
    private Num price(long price) {
        return Num.of(price);
    }

    /**
     * Set price by value.
     * 
     * @param price A price.
     * @return Chainable API.
     */
    @Icy.Overload("price")
    private Num price(float price) {
        return Num.of(price);
    }

    /**
     * Set price by value.
     * 
     * @param price A price.
     * @return Chainable API.
     */
    @Icy.Overload("price")
    private Num price(double price) {
        return Num.of(price);
    }

    /**
     * Validate order price.
     * 
     * @param price
     * @return
     */
    @Icy.Intercept("price")
    private Num price(Num price, Consumer<OrderType> type) {
        if (price.isNegative()) {
            price = Num.ZERO;
        }

        if (state() == OrderState.INIT) {
            type.accept(price.isZero() ? OrderType.MARKET : OrderType.LIMIT);
        }
        return price;
    }

    /**
     * The order type.
     * 
     * @return
     */
    @Icy.Property
    public OrderType type() {
        return OrderType.MARKET;
    }

    /**
     * The quantity conditions enforcement.
     * 
     * @return
     */
    @Icy.Property
    public QuantityCondition quantityCondition() {
        return QuantityCondition.GoodTillCanceled;
    }

    /**
     * Calculate the remaining size of this order.
     * 
     * @return
     */
    @Icy.Property
    public Num remainingSize() {
        return Num.ZERO;
    }

    /** The value stream. */
    private final Signaling<Num> remainingSize = new Signaling();

    @Icy.Intercept("remainingSize")
    private Num remainingSize(Num size) {
        remainingSize.accept(size);
        return size;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<Num> observeRemainingSize() {
        return remainingSize.expose;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<Num> observeRemainingSizeNow() {
        return observeRemainingSize().startWith(remainingSize());
    }

    /**
     * Observe value diff.
     * 
     * @return
     */
    public final Signal<Num> observeRemainingSizeDiff() {
        return observeRemainingSizeNow().maps(Num.ZERO, (prev, now) -> now.minus(prev));
    }

    /**
     * Calculate executed size of this order.
     * 
     * @return
     */
    @Icy.Property
    public Num executedSize() {
        return Num.ZERO;
    }

    /** The value stream. */
    private final Signaling<Num> executedSize = new Signaling();

    @Icy.Intercept("executedSize")
    private Num executedSize(Num size) {
        executedSize.accept(size);
        return size;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<Num> observeExecutedSize() {
        return executedSize.expose;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<Num> observeExecutedSizeNow() {
        return observeExecutedSize().startWith(executedSize());
    }

    /**
     * Observe value diff.
     * 
     * @return
     */
    public final Signal<Num> observeExecutedSizeDiff() {
        return observeExecutedSizeNow().maps(Num.ZERO, (prev, now) -> now.minus(prev));
    }

    /**
     * Observe value diff.
     * 
     * @return
     */
    final Signal<Num> observeCostDiff() {
        return cost.observeNow().maps(Num.ZERO, (prev, now) -> now.minus(prev));
    }

    /**
     * The order identifier for the specific market.
     * 
     * @return
     */
    @Icy.Property
    public String id() {
        return "";
    }

    /**
     * The requested time of this order.
     * 
     * @return
     */
    @Icy.Property
    public ZonedDateTime creationTime() {
        return null;
    }

    /** The value stream. */
    private final Signaling<ZonedDateTime> creationTime = new Signaling();

    @Icy.Intercept("creationTime")
    private ZonedDateTime creationTime(ZonedDateTime date) {
        creationTime.accept(date);
        return date;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<ZonedDateTime> observeCreationTime() {
        return creationTime.expose;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<ZonedDateTime> observeCreationTimeNow() {
        return observeCreationTime().startWith(creationTime());
    }

    /**
     * The termiated time of this order.
     * 
     * @return
     */
    @Icy.Property
    public ZonedDateTime terminationTime() {
        return null;
    }

    /** The value stream. */
    private final Signaling<ZonedDateTime> terminationTime = new Signaling();

    @Icy.Intercept("terminationTime")
    private ZonedDateTime terminationTime(ZonedDateTime date) {
        terminationTime.accept(date);
        return date;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<ZonedDateTime> observeTerminationTime() {
        return creationTime.expose;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<ZonedDateTime> observeTerminationTimeNow() {
        return observeTerminationTime().startWith(terminationTime());
    }

    /**
     * The termiated time of this order.
     * 
     * @return
     */
    @Icy.Property
    public OrderState state() {
        return OrderState.INIT;
    }

    /** The value stream. */
    private final Signaling<OrderState> state = new Signaling();

    @Icy.Intercept("state")
    private OrderState state(OrderState v) {
        state.accept(v);
        return v;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<OrderState> observeState() {
        return state.expose;
    }

    /**
     * Observe value modification.
     * 
     * @return
     */
    public final Signal<OrderState> observeStateNow() {
        return observeState().startWith(state());
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isExpired() {
        return state() == OrderState.EXPIRED;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isNotExpired() {
        return isExpired() == false;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isCanceled() {
        return state() == OrderState.CANCELED;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isNotCanceled() {
        return isCanceled() == false;
    }

    /**
     * Check the order {@link OrderState}.
     * 
     * @return The result.
     */
    public final boolean isCompleted() {
        return state() == OrderState.COMPLETED;
    }

    /**
     * Check the order {@link OrderState}.
     *
     * @return The result.
     */
    public final boolean isNotCompleted() {
        return isCompleted() == false;
    }

    /**
     * Retrieve the relation by type.
     * 
     * @param type A relation type.
     */
    public final <T> T relation(Class<T> type) {
        if (relations == null) {
            relations = new ConcurrentHashMap();
        }
        return (T) relations.computeIfAbsent(type, key -> I.make(type));
    }

    /**
     * Write log.
     * 
     * @param comment
     * @return
     */
    public final OrderModel log(String comment) {
        if (comment != null && !comment.isEmpty()) {
            relation(Log.class).items.add(comment);
        }
        return this;
    }

    /**
     * Write log.
     * 
     * @param comment
     * @param params
     */
    public final void log(String comment, Object... params) {
        log(String.format(comment, params));
    }

    /**
     * Observe when this {@link OldOrder} will be canceled or completed.
     * 
     * @return A event {@link Signal}.
     */
    public final Signal<Order> observeTerminating() {
        return observeState().take(OrderState.CANCELED, OrderState.COMPLETED).take(1).mapTo((Order) this);
    }

    /**
     * Register entry or exit execution.
     */
    final void execute(Execution execution) {
        entries.add(execution);
        cost.set(v -> v.plus(execution.size.multiply(execution.price)));
    }

    /**
     * Retrieve first {@link Execution}.
     * 
     * @return
     */
    @Deprecated
    public Variable<Execution> first() {
        return Variable.of(entries.peekFirst());
    }

    /**
     * Retrieve last {@link Execution}.
     * 
     * @return
     */
    @Deprecated
    public Variable<Execution> last() {
        return Variable.of(entries.peekLast());
    }

    /**
     * Retrieve all {@link Execution}s.
     * 
     * @return
     */
    @Deprecated
    public Signal<Execution> all() {
        return I.signal(entries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Order ? Objects.equals(id(), ((Order) obj).id()) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return direction()
                .mark() + size() + "@" + price() + " 残" + remainingSize() + " 済" + executedSize + " " + creationTime() + " " + state;
    }

    /**
     * Log for {@link OldOrder}.
     */
    private static class Log {

        /** The actual log. */
        private final LinkedList<String> items = new LinkedList();
    }
}
