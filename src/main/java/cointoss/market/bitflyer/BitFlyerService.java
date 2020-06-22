/*
 * Copyright (C) 2019 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package cointoss.market.bitflyer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.TextInputDialog;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cointoss.Direction;
import cointoss.MarketService;
import cointoss.MarketSetting;
import cointoss.execution.Execution;
import cointoss.order.Order;
import cointoss.order.OrderBookPage;
import cointoss.order.OrderBookPageChanges;
import cointoss.order.OrderState;
import cointoss.order.OrderType;
import cointoss.util.APILimiter;
import cointoss.util.Chrono;
import cointoss.util.Num;
import kiss.Disposable;
import kiss.I;
import kiss.Signal;
import kiss.Signaling;
import necromancy.Necromancy;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import viewtify.Viewtify;

/**
 * <p>
 * Since the order API and the real-time execution API are completely separate systems, execution
 * data may arrive before the order response, or execution data may arrive after the cancellation
 * response.
 * </p>
 */
class BitFlyerService extends MarketService {

    /** REUSE */
    private static final MediaType mime = MediaType.parse("application/json; charset=utf-8");

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-");

    /** The bitflyer ID date fromat. */
    private static final DateTimeFormatter IdFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** The bitflyer API limit. */
    private static final APILimiter Limit = APILimiter.with.limit(500).refresh(Duration.ofMinutes(5));

    /** The realtime data format */
    static final DateTimeFormatter RealTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /** The realtime data format */
    static final DateTimeFormatter RealTimeFormatUntilSecond = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** The realtime data format */
    static final DateTimeFormatter RealTimeFormatUntilMinute = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /** The realtime data format */
    static final DateTimeFormatter RealTimeFormatUntilHour = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    /** The api url. */
    static final String api = "https://api.bitflyer.com";

    /** The internal order manager. */
    private final Map<String, Order> orders = new ConcurrentHashMap();

    /** Flag for test. */
    private final boolean forTest;

    /** The account setting. */
    private final BitFlyerAccount account = I.make(BitFlyerAccount.class);

    /** The shared order list. */
    private final Signal<List<Order>> intervalOrderCheck;

    /** The event stream of real-time order update. */
    private final Signaling<Order> orderUpdateRealtimely = new Signaling();

    /** The session key. */
    private final String sessionKey = "api_session_v2";

    /** The session maintainer. */
    private final SessionMaintainer sessionMaintainer = new SessionMaintainer();

    /** The latest realtime execution id. */
    private long latestId;

    /**
     * @param type
     */
    BitFlyerService(String type, MarketSetting setting) {
        this(type, false, setting);
    }

    /**
     * @param type
     */
    BitFlyerService(String type, boolean forTest, MarketSetting setting) {
        super("BitFlyer", type, setting);

        this.forTest = forTest;
        this.intervalOrderCheck = I.signal(0, 1, TimeUnit.SECONDS, scheduler()).map(v -> orders().toList()).share();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> request(Order order) {
        Signal<String> call;
        String id = "JRF" + Chrono.utcNow().format(format) + RandomStringUtils.randomNumeric(6);

        if (forTest || sessionMaintainer.session() == null) {
            ChildOrderRequest request = new ChildOrderRequest();
            request.child_order_type = order.type == OrderType.Maker ? "LIMIT" : "MARKET";
            request.minute_to_expire = 60 * 24;
            request.price = order.price.intValue();
            request.product_code = marketName;
            request.side = order.direction().name();
            request.size = order.size.doubleValue();
            request.time_in_force = order.quantityCondition.abbreviation;

            call = call("POST", "/v1/me/sendchildorder", request, "child_order_acceptance_id", String.class);
        } else {
            ChildOrderRequestWebAPI request = new ChildOrderRequestWebAPI();
            request.account_id = account.accountId.v;
            request.ord_type = order.type == OrderType.Maker ? "LIMIT" : "MARKET";
            request.minute_to_expire = 60 * 24;
            request.order_ref_id = id;
            request.price = order.price.intValue();
            request.product_code = marketName;
            request.side = order.direction().name();
            request.size = order.size.doubleValue();
            request.time_in_force = order.quantityCondition.abbreviation;

            call = call("POST", "https://lightning.bitflyer.jp/api/trade/sendorder", request, "", WebResponse.class)
                    .map(e -> e.data.get("order_ref_id"));
        }

        Complementer complementer = new Complementer(order);

        return call //
                .effectOnObserve(complementer::start)
                .effect(complementer::complement)
                .effectOnTerminate(complementer::stop);
    }

    /**
     * <p>
     * Comlement executions while order request and response.
     * </p>
     * <p>
     * If the execution data comes to the real-time API before the oreder's response, the order
     * cannot be identified from the real-time API.
     * </p>
     * <p>
     * Record all execution data from request to response, and check if there is already an
     * execution data at response.
     * </p>
     */
    private class Complementer {

        /** The associated order. */
        private final Order order;

        /** The realtime execution manager. */
        private final LinkedList<Execution> executions = new LinkedList();

        /** The disposer for realtime execution stream. */
        private Disposable disposer;

        /**
         * 
         */
        private Complementer(Order order) {
            this.order = order;
        }

        /**
         * Start complementing.
         */
        private void start() {
            disposer = executionsRealtimely().to(executions::add);
        }

        /**
         * Stop complementing.
         */
        private void stop() {
            disposer.dispose();
        }

        /**
         * Because ID registration has been completed, it is possible to detect contracts from the
         * real-time API. Check if there is an order in the execution data recorded after placing
         * the order.
         */
        private void complement(String orderId) {
            // stop recording realtime executions and register order id atomically
            disposer.dispose();
            orders.put(orderId, order);

            // check order executions while request and response
            executions.forEach(e -> {
                if (e.buyer.equals(orderId) || e.seller.equals(orderId)) {
                    updateOrder(order, e);
                }
            });

            // order termination will unregister
            order.observeTerminating().to(() -> orders.remove(orderId));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> cancel(Order order) {
        CancelRequest cancel = new CancelRequest();
        cancel.product_code = marketName;
        cancel.account_id = account.accountId.v;
        cancel.order_id = order.relation(Internals.class).id;
        cancel.child_order_acceptance_id = order.id;

        Signal<?> call = forTest || sessionMaintainer.session() == null || cancel.order_id == null
                ? call("POST", "/v1/me/cancelchildorder", cancel, null, null)
                : call("POST", "https://lightning.bitflyer.jp/api/trade/cancelorder", cancel, null, WebResponse.class);

        Signal<Order> isCancelled = intervalOrderCheck.map(orders -> {
            for (Order listed : orders) {
                if (order.id.equals(listed.id)) {
                    if (listed.isTerminated()) {
                        return listed;
                    } else {
                        return null;
                    }
                }
            }

            return Order.with.direction(order.direction, order.size)
                    .id(order.id)
                    .state(OrderState.CANCELED)
                    .remainingSize(order.remainingSize)
                    .executedSize(order.executedSize);
        }).skipNull();

        return call.combine(isCancelled).take(1).map(v -> v.ⅱ).effect(orderUpdateRealtimely::accept);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Integer> delay() {
        return Signal.never();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Execution> connectExecutionRealtimely() {
        String[] previous = new String[] {"", ""};

        return network.jsonRPC("wss://ws.lightstream.bitflyer.com/json-rpc", "lightning_executions_" + marketName)
                .flatIterable(JsonElement::getAsJsonArray)
                .map(JsonElement::getAsJsonObject)
                .map(e -> {
                    long id = e.get("id").getAsLong();

                    if (id == 0 && latestId == 0) {
                        return null; // skip
                    }

                    id = latestId = id != 0 ? id : ++latestId;
                    Direction direction = Direction.parse(e.get("side").getAsString());
                    Num size = Num.of(e.get("size").getAsString());
                    Num price = Num.of(e.get("price").getAsString());
                    ZonedDateTime date = parse(e.get("exec_date").getAsString()).atZone(Chrono.UTC);
                    String buyer = e.get("buy_child_order_acceptance_id").getAsString();
                    String seller = e.get("sell_child_order_acceptance_id").getAsString();
                    String taker = direction.isBuy() ? buyer : seller;
                    int consecutiveType = estimateConsecutiveType(previous[0], previous[1], buyer, seller);
                    int delay = estimateDelay(taker, date);

                    Execution exe = Execution.with.direction(direction, size)
                            .id(id)
                            .price(price)
                            .date(date)
                            .consecutive(consecutiveType)
                            .delay(delay)
                            .buyer(buyer)
                            .seller(seller);

                    Order o = orders.get(buyer);

                    if (o != null) {
                        updateOrder(o, exe);
                    } else {
                        o = orders.get(seller);

                        if (o != null) {
                            updateOrder(o, exe);
                        }
                    }

                    previous[0] = buyer;
                    previous[1] = seller;

                    return exe;
                })
                .skipNull();
    }

    /**
     * Normalize time format.
     * 
     * @param time
     * @return
     */
    static LocalDateTime parse(String time) {
        int size = time.length();

        // remove tail Z
        time = time.substring(0, size - 1);

        switch (size) {
        case 14:
            return LocalDateTime.parse(time, RealTimeFormatUntilHour);

        case 17:
            return LocalDateTime.parse(time, RealTimeFormatUntilMinute);

        case 20:
            return LocalDateTime.parse(time, RealTimeFormatUntilSecond);

        default:
            // padding 0
            if (size < 24) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 24 - size; i++) {
                    builder.append("0");
                }
                time = time + builder;
            }
            return LocalDateTime.parse(time.substring(0, 23), RealTimeFormat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executions(long start, long end) {
        String[] previous = new String[] {"", ""};

        return call("GET", "/v1/executions?product_code=" + marketName + "&count=" + setting
                .acquirableExecutionSize() + "&before=" + end + "&after=" + start, "").flatIterable(JsonElement::getAsJsonArray)
                        .reverse()
                        .map(e -> convert(e, previous));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> executionLatest() {
        String[] previous = new String[] {"", ""};

        return call("GET", "/v1/executions?product_code=" + marketName + "&count=1", "").flatIterable(JsonElement::getAsJsonArray)
                .map(e -> convert(e, previous));
    }

    /**
     * Convert to {@link Execution}.
     * 
     * @param json
     * @param previous
     * @return
     */
    private Execution convert(JsonElement json, String[] previous) {
        JsonObject e = json.getAsJsonObject();

        long id = e.get("id").getAsLong();
        Direction direction = Direction.parse(e.get("side").getAsString());
        Num size = Num.of(e.get("size").getAsString());
        Num price = Num.of(e.get("price").getAsString());
        ZonedDateTime date = LocalDateTime.parse(e.get("exec_date").getAsString()).atZone(Chrono.UTC);
        String buyer = e.get("buy_child_order_acceptance_id").getAsString();
        String seller = e.get("sell_child_order_acceptance_id").getAsString();
        String taker = direction.isBuy() ? buyer : seller;
        int consecutiveType = estimateConsecutiveType(previous[0], previous[1], buyer, seller);
        int delay = estimateDelay(taker, date);

        Execution exe = Execution.with.direction(direction, size).id(id).price(price).date(date).consecutive(consecutiveType).delay(delay);

        previous[0] = buyer;
        previous[1] = seller;

        return exe;
    }

    /**
     * Estimate consecutive type.
     * 
     * @param previous
     */
    private int estimateConsecutiveType(String prevBuyer, String prevSeller, String buyer, String seller) {
        if (buyer.equals(prevBuyer)) {
            if (seller.equals(prevSeller)) {
                return Execution.ConsecutiveSameBoth;
            } else {
                return Execution.ConsecutiveSameBuyer;
            }
        } else if (seller.equals(prevSeller)) {
            return Execution.ConsecutiveSameSeller;
        } else {
            return Execution.ConsecutiveDifference;
        }
    }

    /**
     * <p>
     * Analyze Taker's order ID and obtain approximate order time (Since there is a bot which
     * specifies non-standard id format, ignore it in that case).
     * </p>
     * <ol>
     * <li>Execution Date : UTC</li>
     * <li>Server Order ID Date : UTC (i.e. stop-limit or IFD order)</li>
     * <li>User Order ID Date : JST+9:00</li>
     * </ol>
     *
     * @param exe
     * @return
     */
    private int estimateDelay(String taker, ZonedDateTime date) {
        try {
            // order format is like the following [JRF20180427-123407-869661]
            // exclude illegal format
            if (taker == null || taker.length() != 25 || !taker.startsWith("JRF")) {
                return Execution.DelayInestimable;
            }

            // remove tail random numbers
            taker = taker.substring(3, 18);

            // parse as datetime
            long orderTime = LocalDateTime.parse(taker, IdFormat).toEpochSecond(ZoneOffset.UTC);
            long executedTime = date.toEpochSecond() + 1;
            int diff = (int) (executedTime - orderTime);

            // estimate server order (over 9*60*60)
            if (diff < -32000) {
                diff += 32400;
            }

            if (diff < 0) {
                return Execution.DelayInestimable;
            } else if (180 < diff) {
                return Execution.DelayHuge;
            } else {
                return diff;
            }
        } catch (DateTimeParseException e) {
            return Execution.DelayInestimable;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders() {
        return call("GET", "/v1/me/getchildorders?product_code=" + marketName, "", "*", ChildOrderResponse.class)
                .map(ChildOrderResponse::toOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders(OrderState state) {
        return call("GET", "/v1/me/getchildorders?child_order_state=" + state + "&product_code=" + marketName, "", "*", ChildOrderResponse.class)
                .map(ChildOrderResponse::toOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<Order> connectOrdersRealtimely() {
        return orderUpdateRealtimely.expose;
    }

    /**
     * Update order by execution.
     * 
     * @param o
     * @param e
     */
    private void updateOrder(Order o, Execution e) {
        Num remaining = o.remainingSize.minus(e.size);

        orderUpdateRealtimely.accept(Order.with.direction(o.direction, o.size)
                .id(o.id)
                .state(remaining.isZero() ? OrderState.COMPLETED : OrderState.ACTIVE)
                .remainingSize(remaining)
                .executedSize(o.executedSize.plus(e.size)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> baseCurrency() {
        return call("GET", "/v1/me/getbalance", "", "*", CurrencyState.class).take(unit -> unit.currency_code.equals("JPY"))
                .map(c -> c.available);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> targetCurrency() {
        return call("GET", "/v1/me/getbalance", "", "*", CurrencyState.class).take(unit -> unit.currency_code.equals("BTC"))
                .map(c -> c.available);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<OrderBookPageChanges> orderBook() {
        return call("PUBLIC", "/v1/board?product_code=" + marketName, "", "", OrderBookPageChanges.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Signal<OrderBookPageChanges> connectOrderBookRealtimely() {
        return network.jsonRPC("wss://ws.lightstream.bitflyer.com/json-rpc", "lightning_board_" + marketName)
                .map(JsonElement::getAsJsonObject)
                .map(e -> {
                    OrderBookPageChanges change = new OrderBookPageChanges();
                    JsonArray asks = e.get("asks").getAsJsonArray();

                    for (int i = 0; i < asks.size(); i++) {
                        JsonObject ask = asks.get(i).getAsJsonObject();
                        change.asks.add(new OrderBookPage(Num.of(ask.get("price").getAsString()), ask.get("size").getAsDouble()));
                    }

                    JsonArray bids = e.get("bids").getAsJsonArray();

                    for (int i = 0; i < bids.size(); i++) {
                        JsonObject bid = bids.get(i).getAsJsonObject();
                        change.bids.add(new OrderBookPage(Num.of(bid.get("price").getAsString()), bid.get("size").getAsDouble()));
                    }
                    return change;
                });
    }

    /**
     * Call private API.
     */
    private <M> Signal<M> call(String method, String path, Object body, String selector, Class<M> type) {
        StringBuilder builder = new StringBuilder();
        I.write(body, builder);

        return call(method, path, builder.toString(), selector, type);
    }

    /**
     * Call private API.
     */
    protected <M> Signal<M> call(String method, String path, String body, String selector, Class<M> type) {
        String timestamp = String.valueOf(Chrono.utcNow().toEpochSecond());
        String sign = Hashing.hmacSha256(account.apiSecret.v.getBytes())
                .hashString(timestamp + method + path + body, StandardCharsets.UTF_8)
                .toString();

        Request request;

        if (method.equals("PUBLIC")) {
            request = new Request.Builder().url(api + path).build();
        } else if (method.equals("GET")) {
            request = new Request.Builder().url(api + path)
                    .addHeader("ACCESS-KEY", account.apiKey.v)
                    .addHeader("ACCESS-TIMESTAMP", timestamp)
                    .addHeader("ACCESS-SIGN", sign)
                    .build();
        } else if (method.equals("POST") && !path.startsWith("http")) {
            request = new Request.Builder().url(api + path)
                    .addHeader("ACCESS-KEY", account.apiKey.v)
                    .addHeader("ACCESS-TIMESTAMP", timestamp)
                    .addHeader("ACCESS-SIGN", sign)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(mime, body))
                    .build();
        } else {
            Builder builder = new Request.Builder().url(path)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Cookie", sessionKey + "=" + sessionMaintainer.session)
                    .addHeader("X-Requested-With", "XMLHttpRequest");
            if (body != null && body.length() != 0) builder = builder.post(RequestBody.create(mime, body));
            request = builder.build();
        }
        return network.rest(request, selector, type, Limit);
    }

    /**
     * Call private API.
     */
    protected Signal<JsonElement> call(String method, String path, String body) {
        String timestamp = String.valueOf(Chrono.utcNow().toEpochSecond());
        String sign = Hashing.hmacSha256(account.apiSecret.v.getBytes())
                .hashString(timestamp + method + path + body, StandardCharsets.UTF_8)
                .toString();

        Request request;

        if (method.equals("PUBLIC")) {
            request = new Request.Builder().url(api + path).build();
        } else if (method.equals("GET")) {
            request = new Request.Builder().url(api + path)
                    .addHeader("ACCESS-KEY", account.apiKey.v)
                    .addHeader("ACCESS-TIMESTAMP", timestamp)
                    .addHeader("ACCESS-SIGN", sign)
                    .build();
        } else if (method.equals("POST") && !path.startsWith("http")) {
            request = new Request.Builder().url(api + path)
                    .addHeader("ACCESS-KEY", account.apiKey.v)
                    .addHeader("ACCESS-TIMESTAMP", timestamp)
                    .addHeader("ACCESS-SIGN", sign)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(mime, body))
                    .build();
        } else {
            Builder builder = new Request.Builder().url(path)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Cookie", sessionKey + "=" + sessionMaintainer.session)
                    .addHeader("X-Requested-With", "XMLHttpRequest");
            if (body != null && body.length() != 0) builder = builder.post(RequestBody.create(mime, body));
            request = builder.build();
        }
        return network.rest(request, Limit);
    }

    /**
     * @version 2018/02/15 9:27:14
     */
    private class SessionMaintainer {

        /** The session id. */
        private String session;

        private boolean started = false;

        private Disposable ping;

        /**
         * Retrieve the session id.
         * 
         * @return
         */
        private String session() {
            if (session == null) {
                I.schedule(this::connect);
            }
            return session;
        }

        /**
         * Connect to server and get session id.
         */
        private synchronized void connect() {
            if (started == false) {
                started = true;

                Necromancy browser = Necromancy.with.profile(".log/bitflyer/chrome");

                browser.load("https://lightning.bitflyer.jp") //
                        .input("#LoginId", account.loginId)
                        .input("#Password", account.loginPassword)
                        .click("#login_btn");

                if (browser.uri().equals("https://lightning.bitflyer.jp/Home/TwoFactorAuth")) {
                    Viewtify.inUI(() -> {
                        String code = new TextInputDialog().showAndWait()
                                .orElseThrow(() -> new IllegalArgumentException("二段階認証の確認コードが間違っています"))
                                .trim();

                        Viewtify.inWorker(() -> {
                            browser.click("form > label").input("#ConfirmationCode", code).click("form > button");
                            session = browser.cookie(sessionKey);
                            browser.reload();
                            browser.dispose();
                            maintain();
                        });
                    });
                } else {
                    session = browser.cookie(sessionKey);
                    browser.reload();
                    browser.dispose();
                    maintain();
                }
            }
        }

        /**
         * Maintain the session.
         */
        private synchronized void maintain() {
            if (ping == null) {
                I.signal(10, 10, TimeUnit.MINUTES);
                disposer.add(scheduler().scheduleAtFixedRate(() -> {
                    call("", "https://lightning.bitflyer.com/api/trade/getMyBoardOrders", "{\"product_code\":\"" + marketName + "\",\"account_id\":\"" + account.accountId + "\",\"lang\":\"ja\"}")
                            .to(I.NoOP);
                }, 1, 1, TimeUnit.MINUTES));
            }
        }
    }

    /**
     * @version 2017/11/13 13:09:00
     */
    @SuppressWarnings("unused")
    private static class ChildOrderRequest {

        public String product_code;

        public String child_order_type;

        public String side;

        public int price;

        public double size;

        public int minute_to_expire;

        public String time_in_force;

    }

    /**
     * @version 2018/02/14 13:36:32
     */
    static class ChildOrderResponse {

        public Direction side;

        public String child_order_id;

        public String child_order_acceptance_id;

        public Num size;

        public Num price;

        public Num average_price;

        public Num outstanding_size;

        public Num executed_size;

        public Num canceled_size;

        public String child_order_date;

        public OrderState child_order_state;

        public Order toOrder() {
            Order o = Order.with.direction(side, size)
                    .price(price)
                    .remainingSize(outstanding_size)
                    .executedSize(executed_size)
                    .state(child_order_state)
                    .id(child_order_acceptance_id)
                    .type(OrderType.Maker)
                    .creationTime(LocalDateTime.parse(child_order_date, Chrono.DateTimeWithT).atZone(Chrono.UTC));
            o.relation(Internals.class).id = child_order_id;

            return o;
        }
    }

    /**
     * @version 2018/01/29 1:23:05
     */
    @SuppressWarnings("unused")
    private static class ChildOrderRequestWebAPI {

        public String product_code;

        public String order_ref_id;

        public String ord_type;

        public String side;

        public int price;

        public double size;

        public double minute_to_expire;

        public String time_in_force;

        public String lang = "ja";

        public String account_id;

    }

    /**
     * @version 2018/02/23 16:41:56
     */
    @SuppressWarnings("unused")
    private static class CancelRequest {

        /** For REST API. */
        public String product_code;

        /** For REST API. */
        public String child_order_acceptance_id;

        /** For internal API. */
        public String lang = "ja";

        /** For internal API. */
        public String account_id;

        /** For internal API. */
        public String order_id;
    }

    /**
     * @version 2017/11/28 9:28:38
     */
    @SuppressWarnings("unused")
    private static class CurrencyState {

        /** The currency code. */
        public String currency_code;

        /** The total currency amount. */
        public Num amount;

        /** The available currency amount. */
        public Num available;
    }

    /**
     * @version 2018/02/09 11:42:27
     */
    @SuppressWarnings("unused")
    private class WebRequest {

        /** Generic parameter */
        public String account_id = account.accountId.v;

        /** Generic parameter */
        public String product_code = marketName;

        /** Generic parameter */
        public String lang = "ja";
    }

    /**
     * @version 2018/02/09 11:42:24
     */
    private static class WebResponse {

        /** Generic parameter */
        public int status;

        /** Generic parameter */
        public String error_message;

        /** Generic parameter */
        public Map<String, String> data = new HashMap();

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "WebResponse [status=" + status + ", error_message=" + error_message + ", data=" + data + "]";
        }
    }

    /**
     * 
     */
    private static class CircuitBreakerInfo {
        public Num upper_limit;

        public Num lower_limit;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "CircuitBreakerInfo [upper_limit=" + upper_limit + ", lower_limit=" + lower_limit + "]";
        }
    }

    /**
     * @version 2018/07/08 11:32:36
     */
    private static class Internals {

        private String id;
    }
}
