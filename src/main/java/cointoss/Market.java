/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss;

import static cointoss.order.Order.State.*;
import static java.util.concurrent.TimeUnit.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import cointoss.order.Order;
import cointoss.order.Order.State;
import cointoss.order.OrderBook;
import cointoss.order.OrderManager;
import cointoss.ticker.ExecutionFlow;
import cointoss.ticker.TickSpan;
import cointoss.ticker.Ticker;
import cointoss.ticker.TickerManager;
import cointoss.util.Num;
import kiss.Disposable;
import kiss.I;
import kiss.Signal;
import kiss.Signaling;
import kiss.Variable;

/**
 * @version 2018/04/29 20:17:06
 */
public class Market implements Disposable {

    /** The initial execution. */
    private static final Execution SEED = new Execution();

    static {
        SEED.side = Side.BUY;
    }

    private final AtomicReference<Execution> switcher = new AtomicReference<>(SEED);

    /** The market handler. */
    public final MarketService service;

    public final ExecutionFlow flow = new ExecutionFlow(100);

    public final ExecutionFlow flow75 = new ExecutionFlow(200);

    public final ExecutionFlow flow100 = new ExecutionFlow(400);

    public final ExecutionFlow flow200 = new ExecutionFlow(800);

    public final ExecutionFlow flow300 = new ExecutionFlow(1600);

    /** The execution observers. */
    protected final Signaling<Execution> timelineObservers = new Signaling();

    /** The execution time line. */
    public final Signal<Execution> timeline = timelineObservers.expose;

    /** The execution time line by taker. */
    public final Signal<Execution> timelineByTaker = timeline.map(e -> {
        Execution previous = switcher.getAndSet(e);

        if (e.consecutive == Execution.ConsecutiveSameBuyer || e.consecutive == Execution.ConsecutiveSameSeller) {
            // same taker
            e.cumulativeSize = previous.cumulativeSize.plus(e.size);
            return null;
        }
        return previous;
    }).skip(e -> e == null || e == SEED);

    /** The order book. */
    public final OrderBook orderBook = new OrderBook();

    /** The order manager. */
    public final OrderManager orders = new OrderManager();

    /** The ticker manager. */
    public final TickerManager tickers = new TickerManager();

    /** The position manager. */
    public final PositionManager positions = new PositionManager(tickers.latest);

    /** The amount of base currency. */
    public final Variable<Num> base = Variable.empty();

    /** The initial amount of base currency. */
    public final Variable<Num> baseInit = Variable.empty();

    /** The amount of target currency. */
    public final Variable<Num> target = Variable.empty();

    /** The initial amount of target currency. */
    public final Variable<Num> targetInit = Variable.empty();

    /** The tarader manager. */
    private final List<Trader> traders = new CopyOnWriteArrayList<>();

    /** The order manager. */
    private final List<Order> orderItems = new CopyOnWriteArrayList();

    /**
     * Build {@link Market} with the specified {@link MarketProvider}.
     * 
     * @param provider A market provider.
     */
    public Market(MarketService service) {
        this.service = Objects.requireNonNull(service, "Market is not found.");

        // build tickers for each span
        timeline.to(tickers::update);

        // initialize currency data
        service.baseCurrency().to(v -> {
            this.base.set(v);
            this.baseInit.let(v);
        });
        service.targetCurrency().to(v -> {
            this.target.set(v);
            this.targetInit.let(v);
        });

        // orderbook management
        service.add(service.orderBook().to(board -> {
            orderBook.shorts.update(board.asks);
            orderBook.longs.update(board.bids);
        }));
        service.add(timeline.throttle(2, TimeUnit.SECONDS).to(e -> {
            // fix error board
            orderBook.shorts.fix(e.price);
            orderBook.longs.fix(e.price);
        }));

        service.add(service.positions().to(e -> {
            for (Order order : orderItems) {
                if (order.id().equals(e.yourOrder)) {
                    update(order, e);

                    order.listeners.accept(e);

                    Position position = new Position();
                    position.side = order.side;
                    position.price = e.price;
                    position.size.set(e.size);
                    position.date = e.exec_date;
                    positions.add(position);
                }
            }
        }));
    }

    /**
     * Add market trader to this market.
     * 
     * @param trader A trader to add.
     */
    public final void addTrader(Trader trader) {
        if (trader != null) {
            traders.add(trader);
            trader.market = this;
            trader.initialize();
        }
    }

    /**
     * Remove market trader to this market.
     * 
     * @param trader A trader to remove.
     */
    public final void removeTrader(Trader trader) {
        if (trader != null) {
            traders.remove(trader);
            trader.dispose();
            trader.market = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void vandalize() {
        service.dispose();
    }

    /**
     * Request order actually.
     * 
     * @param order A order to request.
     * @return A requested order.
     */
    public final Signal<Order> request(Order order) {
        order.state.set(REQUESTING);

        return service.request(order).retryWhen(fail -> fail.effect(e -> {
            System.out.println("Fail " + order + "  retry ");
            e.printStackTrace();
        }).take(40).delay(100, MILLISECONDS)).map(id -> {
            order.id = id;
            order.created.set(ZonedDateTime.now());
            order.averagePrice.set(order.price);
            order.remaining.set(order.size);
            order.state.set(ACTIVE);

            orderItems.add(order);
            orders.add(order);

            return order;
        }).effectOnError(e -> {
            order.state.set(State.CANCELED);
        });
    }

    /**
     * Request order canceling.
     * 
     * @param order A order to cancel.
     * @return A canceled order.
     */
    public final Signal<Order> cancel(Order order) {
        if (order.state.is(ACTIVE) || order.state.is(State.REQUESTING)) {
            State previous = order.state.set(REQUESTING);

            return service.cancel(order).effect(o -> {
                orderItems.remove(o);
                o.state.set(CANCELED);
            }).effectOnError(e -> {
                order.state.set(previous);
            });
        } else {
            return I.signal(order);
        }
    }

    /**
     * List up all orders.
     * 
     * @return A list of all orders.
     */
    public final List<Order> orders() {
        return service.orders().toList();
    }

    /**
     * Create new price signal.
     * 
     * @param price
     * @return
     */
    public final Signal<Execution> signalByPrice(Num price) {
        if (tickers.latest.v.price.isLessThan(price)) {
            return timeline.take(e -> e.price.isGreaterThanOrEqual(price)).take(1);
        } else {
            return timeline.take(e -> e.price.isLessThanOrEqual(price)).take(1);
        }
    }

    /**
     * Get {@link Ticker} by span.
     * 
     * @param span
     * @return
     */
    public final Ticker tickerBy(TickSpan span) {
        return tickers.tickerBy(span);
    }

    /**
     * Read {@link Execution} log.
     * 
     * @param log
     * @return
     */
    public final Market readLog(Function<MarketLog, Signal<Execution>> log) {
        service.add(log.apply(service.log).to(timelineObservers));

        return this;
    }

    /**
     * Calculate profit and loss.
     * 
     * @return
     */
    public Num calculateProfit() {
        Num baseProfit = base.v.minus(baseInit);
        Num targetProfit = target.v.multiply(tickers.latest.v.price)
                .minus(targetInit.v.multiply(tickers.initial.v.price));
        return baseProfit.plus(targetProfit);
    }

    /**
     * Update local managed {@link Order}.
     * 
     * @param order
     * @param exe
     */
    private void update(Order order, Execution exe) {
        // update assets
        if (order.side().isBuy()) {
            base.set(v -> v.minus(exe.size.multiply(exe.price)));
            target.set(v -> v.plus(exe.size));
        } else {
            base.set(v -> v.plus(exe.size.multiply(exe.price)));
            target.set(v -> v.minus(exe.size));
        }

        // for order state
        Num executed = Num.min(order.remaining, exe.size);

        if (order.child_order_type.isMarket() && executed.isNot(0)) {
            order.averagePrice.set(v -> v.multiply(order.executed_size)
                    .plus(exe.price.multiply(executed))
                    .divide(executed.plus(order.executed_size)));
        }

        order.executed_size.set(v -> v.plus(executed));
        order.remaining.set(v -> v.minus(executed));

        if (order.remaining.is(Num.ZERO)) {
            order.state.set(State.COMPLETED);
            orderItems.remove(order); // complete order
        }

        // pairing order and execution
        order.executions.add(exe);
    }
}
