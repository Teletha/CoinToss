/*
 * Copyright (C) 2021 cointoss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.console;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import stylist.Style;
import stylist.StyleDSL;
import trademate.TradingView;
import viewtify.Viewtify;
import viewtify.ui.UIListView;
import viewtify.ui.View;
import viewtify.ui.ViewDSL;

public class Console extends View {

    /** The maximum line size. */
    private static final int MAX = 3000;

    /** The background logger. */
    private Logger logger;

    private TradingView view;

    private UIListView<String> console;

    final ObservableList<String> messages = FXCollections.observableList(new LinkedList());

    class view extends ViewDSL {
        {
            $(console, style.consoleView);
        }
    }

    interface style extends StyleDSL {

        Style consoleView = () -> {
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        // create logger
        String name = view.service.id;
        logger = LogManager.getLogger(name);
        ConsoleAppender.consoles.put(name, this);

        console.items(messages);
    }

    /**
     * Write message to console.
     * 
     * @param message
     */
    void write(String message) {
        Viewtify.inUI(() -> {
            messages.add(0, message);

            if (MAX < messages.size()) {
                messages.remove(messages.size() - 1);
            }
        });
    }

    /**
     * Write message to console.
     * 
     * @param message
     */
    public void info(String message, Object... params) {
        logger.info(message, params);
    }
}