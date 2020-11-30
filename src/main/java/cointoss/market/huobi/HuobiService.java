/*
 * Copyright (C) 2020 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.market.huobi;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomStringUtils;

import cointoss.Direction;
import cointoss.MarketService;
import cointoss.MarketSetting;
import cointoss.execution.Execution;
import cointoss.market.Exchange;
import cointoss.order.Order;
import cointoss.order.OrderBookPage;
import cointoss.order.OrderBookPageChanges;
import cointoss.order.OrderState;
import cointoss.util.APILimiter;
import cointoss.util.Chrono;
import cointoss.util.EfficientWebSocket;
import cointoss.util.EfficientWebSocketModel.IdentifiableTopic;
import cointoss.util.Network;
import cointoss.util.arithmetic.Num;
import kiss.I;
import kiss.JSON;
import kiss.Signal;

public class HuobiService extends MarketService {

    /** The right padding for id. */
    private static final long PaddingForID = 100000;

    /** The realtime data format */
    private static final DateTimeFormatter RealTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    /** The bitflyer API limit. */
    private static final APILimiter Limit = APILimiter.with.limit(10).refresh(Duration.ofSeconds(1));

    /** The realtime communicator. */
    private static final EfficientWebSocket Realtime = EfficientWebSocket.with.address("wss://api-aws.huobi.pro/ws")
            .extractId(json -> json.text("ch"))
            .pongIf(json -> {
                String id = json.text("ping");
                return id == null ? null : "{'pong':" + id + "}";
            });

    /** The market id. */
    private final int marketId;

    /** The instrument tick size. */
    private final Num instrumentTickSize;

    /**
     * @param marketName
     * @param setting
     */
    protected HuobiService(int id, String marketName, MarketSetting setting) {
        super(Exchange.GMO, marketName, setting);

        this.marketId = id;
        this.instrumentTickSize = marketName.equals("XBTUSD") ? Num.of("0.01") : setting.base.minimumSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EfficientWebSocket clientRealtimely() {
        return Realtime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Integer> delay() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> request(Order order) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> cancel(Order order) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executions(long startId, long endId) {
        startId++;
        long startingPoint = startId % PaddingForID;
        AtomicLong increment = new AtomicLong(startingPoint - 1);
        Object[] previous = new Object[] {null, encodeId(startId)};

        return call("GET", "trade?symbol=" + marketName + "&count=1000" + "&startTime=" + formatEncodedId(startId) + "&start=" + startingPoint)
                .flatIterable(e -> e.find("*"))
                .map(json -> {
                    return convert(json, increment, previous);
                });
    }

    private ZonedDateTime encodeId(long id) {
        return Chrono.utcByMills(id / PaddingForID);
    }

    private String formatEncodedId(long id) {
        return RealTimeFormat.format(encodeId(id));
    }

    private long decodeId(ZonedDateTime time) {
        return time.toInstant().toEpochMilli() * PaddingForID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Execution> connectExecutionRealtimely() {
        AtomicLong increment = new AtomicLong();
        Object[] previous = new Object[2];

        return clientRealtimely().subscribe(new Topic("trade.detail", marketName))
                .flatIterable(json -> json.find("tick", "data", "*"))
                .map(json -> convert(json, increment, previous));
    }

    /**
     * Convert to {@link Execution}.
     * 
     * @param json
     * @param previous
     * @return
     */
    private Execution convert(JSON e, AtomicLong increment, Object[] previous) {
        Direction direction = e.get(Direction.class, "direction");
        Num size = e.get(Num.class, "amount");
        Num price = e.get(Num.class, "price");
        ZonedDateTime date = Chrono.utcByMills(e.get(long.class, "ts"));
        long id = e.get(long.class, "tradeId");

        return Execution.with.direction(direction, size).id(id).price(price).date(date);
    }

    /**
     * Convert to {@link Execution}.
     * 
     * @param json
     * @param previous
     * @return
     */
    private Execution convert2(JSON e, AtomicLong increment, Object[] previous) {
        Direction direction = e.get(Direction.class, "direction");
        Num size = e.get(Num.class, "amount");
        Num price = e.get(Num.class, "price");
        ZonedDateTime date = Chrono.utcByMills(e.get(long.class, "ts"));
        long id = e.get(long.class, "trade-id");

        return Execution.with.direction(direction, size).id(id).price(price).date(date);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatest() {
        return call("GET", "market/trade?symbol=" + marketName).flatIterable(e -> e.find("tick", "data", "*"))
                .map(json -> convert2(json, new AtomicLong(), new Object[2]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatestAt(long id) {
        return call("GET", "market/history/trade?symbol=" + marketName + "&size=2000&page=20")
                .flatIterable(e -> e.find("data", "*", "data", "*"))
                .map(json -> convert2(json, new AtomicLong(), new Object[2]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long estimateInitialExecutionId() {
        return decodeId(Chrono.utc(2020, 1, 1).minusMinutes(3));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders() {
        return I.signal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders(OrderState state) {
        return I.signal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Order> connectOrdersRealtimely() {
        return I.signal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<OrderBookPageChanges> orderBook() {
        return call("GET", "orderBook/L2?depth=1200&symbol=" + marketName).map(e -> convertOrderBook(e.find("*")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<OrderBookPageChanges> connectOrderBookRealtimely() {
        return clientRealtimely().subscribe(new Topic("orderBookL2", marketName))
                .map(json -> json.find("data", "*"))
                .map(this::convertOrderBook);
    }

    /**
     * Convert json to {@link OrderBookPageChanges}.
     * 
     * @param pages
     * @return
     */
    private OrderBookPageChanges convertOrderBook(List<JSON> pages) {
        OrderBookPageChanges change = new OrderBookPageChanges();
        for (JSON page : pages) {
            long id = Long.parseLong(page.text("id"));
            Num price = instrumentTickSize.multiply((100000000L * marketId) - id);
            JSON sizeElement = page.get("size");
            double size = sizeElement == null ? 0 : sizeElement.as(Double.class) / price.doubleValue();

            if (page.text("side").charAt(0) == 'B') {
                change.bids.add(new OrderBookPage(price, size));
            } else {
                change.asks.add(new OrderBookPage(price, size));
            }
        }
        return change;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> baseCurrency() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> targetCurrency() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkEquality(Execution one, Execution other) {
        return one.buyer.equals(other.buyer);
    }

    /**
     * Call rest API.
     * 
     * @param method
     * @param path
     * @return
     */
    private Signal<JSON> call(String method, String path) {
        Builder builder = HttpRequest.newBuilder(URI.create("https://api.huobi.pro/" + path));

        return Network.rest(builder, Limit, client()).retryWhen(retryPolicy(10, "Huobi RESTCall"));
    }

    /**
     * Subscription topic for websocket communication.
     */
    static class Topic extends IdentifiableTopic<Topic> {

        public String sub;

        public String unsub;

        public String id = RandomStringUtils.randomAlphabetic(6);

        private Topic(String channel, String market) {
            super("market." + market + "." + channel, topic -> {
                topic.unsub = topic.sub;
                topic.sub = null;
            });
            this.sub = "market." + market + "." + channel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean verifySubscribedReply(JSON reply) {
            return id.equals(reply.text("id")) && "ok".equals(reply.text("status"));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Huobi.BTC_USDT.executionLatestAt(102220063887L).to(e -> {
            System.out.println(e);
        });

        Thread.sleep(1000 * 10);
    }
}