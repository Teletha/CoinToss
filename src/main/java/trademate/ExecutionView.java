/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package trademate;

import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import javafx.scene.control.ListCell;

import cointoss.Execution;
import cointoss.util.Num;
import viewtify.UI;
import viewtify.View;
import viewtify.Viewtify;
import viewtify.ui.UIListView;
import viewtify.ui.UISpinner;
import viewtify.ui.UserInterface;

/**
 * @version 2018/01/12 21:29:04
 */
public class ExecutionView extends View {

    /** The time format. */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** The execution list. */
    private @UI UIListView<Execution> executionList;

    /** The execution list. */
    private @UI UIListView<Execution> executionCumulativeList;

    /** UI for interval configuration. */
    private @UI UISpinner<Integer> takerSize;

    /** Parent View */
    private @UI TradingView view;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        // configure UI
        executionList.cell(v -> new Cell());
        executionCumulativeList.cell(v -> new Cell())
                .filter(takerSize.property(), (e, size) -> e.cumulativeSize.isGreaterThanOrEqual(size));
        takerSize.values(IntStream.range(1, 51).boxed()).initial(10);

        // load execution log
        Viewtify.inWorker(() -> {
            return view.market().timeline.on(Viewtify.UIThread).to(e -> {
                executionList.add(0, e);

                if (100 < executionList.size()) {
                    executionList.removeLast();
                }
            });
        });

        // load big taker log
        Viewtify.inWorker(() -> {
            return view.market().timelineByTaker.on(Viewtify.UIThread).to(e -> {
                if (e.cumulativeSize.isGreaterThanOrEqual(Num.ONE)) {
                    executionCumulativeList.add(0, e);

                    if (2000 < executionCumulativeList.size()) {
                        executionCumulativeList.removeLast();
                    }
                }
            });
        });
    }

    /**
     * @version 2017/11/13 21:35:32
     */
    private class Cell extends ListCell<Execution> {

        /** The enhanced ui. */
        private final UserInterface ui = Viewtify.wrap(this, ExecutionView.this);

        /**
         * {@inheritDoc}
         */
        @Override
        protected void updateItem(Execution e, boolean empty) {
            super.updateItem(e, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(formatter.format(e.exec_date.plusHours(9)) + "  " + e.price + "円  " + e.cumulativeSize.scale(6));
                ui.styleOnly(e.side);
            }
        }
    }
}
