/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.order;

import static cointoss.order.OrderState.*;
import static trademate.TradeMateStyle.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javafx.scene.control.TreeTableRow;

import cointoss.Side;
import cointoss.order.Order;
import cointoss.order.OrderState;
import cointoss.util.Num;
import kiss.Extensible;
import kiss.Variable;
import stylist.StyleDSL;
import stylist.ValueStyle;
import trademate.TradingView;
import trademate.order.OrderCatalog.Lang;
import viewtify.View;
import viewtify.Viewtify;
import viewtify.bind.Calculation;
import viewtify.dsl.Style;
import viewtify.dsl.UIDefinition;
import viewtify.ui.UITreeItem;
import viewtify.ui.UITreeTableColumn;
import viewtify.ui.UITreeTableView;
import viewtify.ui.UserInterface;

/**
 * @version 2018/08/30 16:54:37
 */
public class OrderCatalog extends View<Lang> {

    /** The date formatter. */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss");

    /** UI */
    private UITreeTableView<Object> table;

    /** UI */
    private UITreeTableColumn<Object, ZonedDateTime> date;

    /** UI */
    private UITreeTableColumn<Object, Side> side;

    /** UI */
    private UITreeTableColumn<Object, Num> amount;

    /** UI */
    private UITreeTableColumn<Object, Num> price;

    /** Parent View */
    private TradingView view;

    /**
     * {@inheritDoc}
     */
    @Override
    protected UIDefinition declareUI() {
        return new UIDefinition() {
            {
                $(table, S.Root, () -> {
                    $(date, S.Wide);
                    $(side, S.Narrow);
                    $(price, S.Wide);
                    $(amount, S.Narrow);
                });
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        table.selectMultipleRows().render(table -> new CatalogRow()).context($ -> {
            Calculation<Boolean> ordersArePassive = table.selection().flatVariable(this::state).isNot(ACTIVE);

            $.menu("Cancel").disableWhen(ordersArePassive).whenUserClick(e -> act(this::cancel));
        });

        date.header($.date())
                .modelByProperty(OrderSet.class, o -> o.date)
                .modelByVar(Order.class, o -> o.created)
                .render((ui, item) -> ui.text(formatter.format(item)));
        side.header($.side())
                .modelByProperty(OrderSet.class, o -> o.side)
                .model(Order.class, Order::side)
                .render((ui, side) -> ui.text(side).styleOnly(Side.of(side)));
        amount.header($.amount()).modelByProperty(OrderSet.class, o -> o.amount).model(Order.class, o -> o.remainingSize);
        price.header($.price()).modelByProperty(OrderSet.class, o -> o.averagePrice).model(Order.class, o -> o.price.v);
    }

    /**
     * Compute order state.
     * 
     * @param item
     * @return
     */
    private Variable<OrderState> state(Object item) {
        return item instanceof Order ? ((Order) item).state : Variable.of(ACTIVE);
    }

    /**
     * Create tree item for {@link OrderSet}.
     * 
     * @param set
     */
    public void createOrderItem(OrderSet set) {
        UITreeItem item = table.root.createItem(set).expand(set.sub.size() != 1).removeWhenEmpty();

        for (Order order : set.sub) {
            createOrderItem(item, order);
        }
    }

    /**
     * Create tree item for {@link Order}.
     */
    private void createOrderItem(UITreeItem item, Order order) {
        item.createItem(order).removeWhen(order.observeTerminating());
    }

    /**
     * Cancel {@link OrderSet} or {@link Order}.
     * 
     * @param order
     */
    private void act(Consumer<Order> forOrder) {
        for (Object order : table.selection()) {
            if (order instanceof Order) {
                forOrder.accept((Order) order);
            } else {
                for (Order child : ((OrderSet) order).sub) {
                    forOrder.accept(child);
                }
            }
        }
    }

    /**
     * Cancel {@link Order}.
     * 
     * @param order
     */
    private void cancel(Order order) {
        Viewtify.inWorker(() -> {
            view.market().cancel(order).to(o -> {
            });
        });
    }

    /**
     * @version 2017/12/04 14:32:07
     */
    private class CatalogRow extends TreeTableRow<Object> {

        private final Calculation<stylist.Style> orderState = Viewtify.calculate(itemProperty()).flatVariable(o -> {
            if (o instanceof Order) {
                return ((Order) o).state;
            } else {
                return Variable.of(OrderState.ACTIVE);
            }
        }).map(S.State::of);

        /** The enhanced ui. */
        private final UserInterface ui = Viewtify.wrap(this, OrderCatalog.this);

        /**
         * 
         */
        private CatalogRow() {
            ui.styleOnly(orderState);
        }
    }

    /**
     * @version 2018/09/07 14:14:11
     */
    private interface S extends StyleDSL {

        Style Root = () -> {
            display.width(400, px);
            text.unselectable();
        };

        ValueStyle<OrderState> State = state -> {
            switch (state) {
            case REQUESTING:
                $.descendant(() -> {
                    font.color($.rgb(80, 80, 80));
                });
                break;
            }
        };

        Style Wide = () -> {
            display.width(120, px);
        };

        Style Narrow = () -> {
            display.width(65, px);
        };
    }

    /**
     * @version 2018/09/07 10:29:37
     */
    static class Lang implements Extensible {

        String date() {
            return "Date";
        }

        String side() {
            return "Side";
        }

        String amount() {
            return "Amount";
        }

        String price() {
            return "Price";
        }

        /**
         * @version 2018/09/07 10:44:14
         */
        private static class Lang_ja extends Lang {

            /**
             * {@inheritDoc}
             */
            @Override
            String date() {
                return "日付";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            String side() {
                return "売買";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            String amount() {
                return "数量";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            String price() {
                return "値段";
            }
        }
    }
}
