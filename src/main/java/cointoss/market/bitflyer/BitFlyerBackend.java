/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.market.bitflyer;

import static java.util.concurrent.TimeUnit.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javafx.scene.control.TextInputDialog;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cointoss.Execution;
import cointoss.Market;
import cointoss.MarketBackend;
import cointoss.Position;
import cointoss.Side;
import cointoss.order.Order;
import cointoss.order.Order.State;
import cointoss.order.OrderBookChange;
import cointoss.order.OrderUnit;
import cointoss.util.Chrono;
import cointoss.util.Network;
import cointoss.util.Num;
import filer.Filer;
import kiss.Disposable;
import kiss.I;
import kiss.JSON;
import kiss.Signal;
import marionette.Browser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import viewtify.Viewtify;

/**
 * @version 2018/02/27 10:20:56
 */
class BitFlyerBackend extends MarketBackend {

    private static final MediaType mime = MediaType.parse("application/json; charset=utf-8");

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-");

    /** The api url. */
    static final String api = "https://api.bitflyer.jp";

    /** UTC */
    static final ZoneId zone = ZoneId.of("UTC");

    /** The market type. */
    private BitFlyer type;

    /** The key. */
    private final String accessKey;

    /** The token. */
    private final String accessToken;

    /** The key. */
    private final String name;

    /** The token. */
    private final String password;

    /** The token. */
    private final String accountId;

    private Disposable disposer = Disposable.empty();

    /** The real time order. */
    private final Signal<List<Order>> orders;

    /**
     * @param type
     */
    BitFlyerBackend(BitFlyer type) {
        this.type = type;

        List<String> lines = Filer.read(".log/bitflyer/key.txt").toList();
        this.accessKey = lines.get(0);
        this.accessToken = lines.get(1);
        this.name = lines.get(2);
        this.password = lines.get(3);
        this.accountId = lines.get(4);
        this.orders = new Signal<List<Order>>((observer, disposer) -> {
            Future<?> future = I.schedule(() -> {
                observer.accept(orders().toList());
            }, 1, SECONDS);
            return disposer.add(() -> {
                future.cancel(true);
            });
        }).share();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return type.fullName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Market market, Signal<Execution> log) {
        disposer.add(log.to(market::tick));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void vandalize() {
        disposer.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> request(Order order) {
        String id = "JRF" + Chrono.utcNow().format(format) + RandomStringUtils.randomNumeric(6);

        if (maintainer.session() == null) {
            ChildOrderRequest request = new ChildOrderRequest();
            request.child_order_type = order.isLimit() ? "LIMIT" : "MARKET";
            request.minute_to_expire = 60 * 24;
            request.price = order.price.toInt();
            request.product_code = type.name();
            request.side = order.side().name();
            request.size = order.size.toDouble();
            request.time_in_force = order.quantity().abbreviation;

            return call("POST", "/v1/me/sendchildorder", request, "child_order_acceptance_id", String.class);
        } else {
            ChildOrderRequestWebAPI request = new ChildOrderRequestWebAPI();
            request.account_id = accountId;
            request.lang = "ja";
            request.minute_to_expire = 60 * 24;
            request.ord_type = order.isLimit() ? "LIMIT" : "MARKET";
            request.order_ref_id = id;
            request.price = order.price.toInt();
            request.product_code = type.name();
            request.side = order.side().name();
            request.size = order.size.toDouble();
            request.time_in_force = order.quantity().abbreviation;

            return call("POST", "https://lightning.bitflyer.jp/api/trade/sendorder", request, "", WebResponse.class)
                    .map(e -> e.data.get("order_ref_id"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> cancel(Order order) {
        CancelRequest cancel = new CancelRequest();
        cancel.product_code = type.name();
        cancel.account_id = accountId;
        cancel.order_id = order.internlId;
        cancel.child_order_acceptance_id = order.id;

        Signal requestCancel = maintainer.session() == null || cancel.order_id == null
                ? call("POST", "/v1/me/cancelchildorder", cancel, null, null)
                : call("POST", "https://lightning.bitflyer.jp/api/trade/cancelorder", cancel, null, WebResponse.class);
        Signal<List<Order>> isCanceled = orders.take(orders -> !orders.contains(order));

        return requestCancel.combine(isCanceled).take(1).mapTo(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Order> orders() {
        return call("GET", "/v1/me/getchildorders?child_order_state=ACTIVE&product_code=" + type.name(), "", "*", ChildOrderResponse.class)
                .map(ChildOrderResponse::toOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Position> getPositions() {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Execution> getExecutions() {
        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> getBaseCurrency() {
        return call("GET", "/v1/me/getbalance", "", "*", CurrencyState.class).take(unit -> unit.currency_code.equals("JPY"))
                .map(c -> c.available);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Num> getTargetCurrency() {
        return call("GET", "/v1/me/getbalance", "", "*", CurrencyState.class).take(unit -> unit.currency_code.equals("BTC"))
                .map(c -> c.available);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<OrderBookChange> getOrderBook() {
        return snapshotOrderBook().merge(realtimeOrderBook());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Health> getHealth() {
        return health;
    }

    private final Signal<Health> health = I.signal(0, 5, SECONDS)
            .flatMap(v -> call("GET", "/v1/gethealth?product_code=" + type, "", "", ServerHealth.class))
            .map(health -> health.status)
            .share()
            .diff();

    /**
     * Snapshot order book info.
     * 
     * @return
     */
    private Signal<OrderBookChange> snapshotOrderBook() {
        return call("GET", "/v1/board?product_code=" + type, "", "", OrderBookChange.class);
    }

    /**
     * Realtime order book info.
     * 
     * @return
     */
    private Signal<OrderBookChange> realtimeOrderBook() {
        return Network.pubnub("lightning_board_" + type, "sub-c-52a9ab50-291b-11e5-baaa-0619f8945a4f")
                .map(JsonElement::getAsJsonObject)
                .map(e -> {
                    OrderBookChange board = new OrderBookChange();
                    board.mid_price = Num.of(e.get("mid_price").getAsLong());

                    JsonArray asks = e.get("asks").getAsJsonArray();

                    for (int i = 0; i < asks.size(); i++) {
                        JsonObject ask = asks.get(i).getAsJsonObject();
                        board.asks.add(new OrderUnit(Num.of(ask.get("price").getAsString()), Num.of(ask.get("size").getAsString())));
                    }

                    JsonArray bids = e.get("bids").getAsJsonArray();

                    for (int i = 0; i < bids.size(); i++) {
                        JsonObject bid = bids.get(i).getAsJsonObject();
                        board.bids.add(new OrderUnit(Num.of(bid.get("price").getAsString()), Num.of(bid.get("size").getAsString())));
                    }
                    return board;
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
    private <M> Signal<M> call(String method, String path, String body, String selector, Class<M> type) {
        return new Signal<>((observer, disposer) -> {
            String timestamp = String.valueOf(ZonedDateTime.now(zone).toEpochSecond());
            String sign = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, accessToken).hmacHex(timestamp + method + path + body);

            Request request;

            if (method.equals("GET")) {
                request = new Request.Builder().url(api + path)
                        .addHeader("ACCESS-KEY", accessKey)
                        .addHeader("ACCESS-TIMESTAMP", timestamp)
                        .addHeader("ACCESS-SIGN", sign)
                        .build();
            } else if (method.equals("POST") && !path.startsWith("http")) {
                request = new Request.Builder().url(api + path)
                        .addHeader("ACCESS-KEY", accessKey)
                        .addHeader("ACCESS-TIMESTAMP", timestamp)
                        .addHeader("ACCESS-SIGN", sign)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(mime, body))
                        .build();
            } else {
                request = new Request.Builder().url(api + path)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Cookie", "api_session=" + maintainer.session())
                        .addHeader("ACCESS-SIGN", sign)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .post(RequestBody.create(mime, body))
                        .build();
            }

            try (Response response = new OkHttpClient().newCall(request).execute()) {
                int code = response.code();
                String value = response.body().string();

                if (code == 200) {
                    if (type == null) {
                        observer.accept(null);
                    } else {
                        JSON json = I.json(value);

                        if (selector == null || selector.isEmpty()) {
                            M m = json.to(type);

                            if (m instanceof WebResponse) {
                                WebResponse res = (WebResponse) m;

                                if (res.error_message != null && !res.error_message.isEmpty()) {
                                    throw new Error(res.error_message);
                                }
                            }
                            observer.accept(m);
                        } else {
                            json.find(selector, type).to(observer);
                        }
                    }
                } else {
                    observer.error(new Error("HTTP Status " + code + " " + value));
                }
            } catch (Throwable e) {
                observer.error(e);
            }
            observer.complete();

            return disposer;
        });
    }

    private SessionMaintainer maintainer = new SessionMaintainer();

    /**
     * @version 2018/02/15 9:27:14
     */
    private class SessionMaintainer implements Disposable {

        /** The session id. */
        private String session;

        private boolean started = false;

        /**
         * Retrieve the session id.
         * 
         * @return
         */
        private String session() {
            if (session == null) {
                // I.schedule(this::connect);
            }
            return session;
        }

        /**
         * Connect to server and get session id.
         */
        private synchronized void connect() {
            if (started == false) {
                started = true;

                Browser browser = new Browser().configHeadless(false).configProfile(".log/bitflyer/chrome");
                browser.load("https://lightning.bitflyer.jp") //
                        .input("#LoginId", name)
                        .input("#Password", password)
                        .click("#login_btn");

                if (browser.uri().equals("https://lightning.bitflyer.jp/Home/TwoFactorAuth")) {
                    Viewtify.inUI(() -> {
                        String code = new TextInputDialog().showAndWait()
                                .orElseThrow(() -> new IllegalArgumentException("二段階認証の確認コードが間違っています"))
                                .trim();

                        Viewtify.inWorker(() -> {
                            browser.click("form > label").input("#ConfirmationCode", code).click("form > button");
                            session = browser.cookie("api_session");
                            System.out.println("SESSION OK");
                            browser.reload();
                            browser.dispose();
                        });
                    });
                } else {
                    session = browser.cookie("api_session");
                    System.out.println("SESSION OK2");
                    browser.dispose();
                }

            }
        }

        /**
         * Maintain the session.
         */
        private void maintain() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void vandalize() {
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
    private static class ChildOrderResponse {

        public Side side;

        public String child_order_id;

        public String child_order_acceptance_id;

        public Num size;

        public Num price;

        public Num average_price;

        public Num outstanding_size;

        public Num executed_size;

        public String child_order_date;

        public State child_order_state;

        public Order toOrder() {
            Order o = Order.limit(side, size, price);
            o.internlId = child_order_id;
            o.id = child_order_acceptance_id;
            o.averagePrice.set(average_price);
            o.remainingSize.set(outstanding_size);
            o.executed_size.set(executed_size);
            o.created.set(LocalDateTime.parse(child_order_date, Chrono.DateTimeWithT).atZone(Chrono.UTC));
            o.state.set(child_order_state);

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

        public String lang;

        public String account_id;
    }

    /**
     * @version 2018/01/29 1:28:03
     */
    @SuppressWarnings("unused")
    private static class ChildOrderResponseWebAPI {

        public int status;

        public String error_message;

        public Map<String, String> data = new HashMap();

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "ChildOrderResponseWebAPI [status=" + status + ", error_message=" + error_message + ", data=" + data + "]";
        }
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
     * @version 2017/12/14 16:24:28
     */
    private static class ServerHealth {

        /** The server status. */
        public Health status;
    }

    /**
     * @version 2018/02/09 11:42:27
     */
    @SuppressWarnings("unused")
    private class WebRequest {

        /** Generic parameter */
        public String account_id = accountId;

        /** Generic parameter */
        public String product_code = type.name();

        /** Generic parameter */
        public String lang = "ja";
    }

    /**
     * @version 2018/02/09 11:42:24
     */
    @SuppressWarnings("unused")
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
}
