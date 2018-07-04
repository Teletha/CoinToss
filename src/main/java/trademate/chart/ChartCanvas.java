/*
 * Copyright (C) 2018 CoinToss Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.chart;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

import cointoss.Side;
import cointoss.order.Order.State;
import cointoss.ticker.Tick;
import cointoss.util.Chrono;
import cointoss.util.Num;
import kiss.I;
import trademate.Notificator;
import trademate.chart.Axis.TickLable;
import viewtify.Viewtify;
import viewtify.ui.helper.LayoutAssistant;
import viewtify.ui.helper.StyleHelper;

/**
 * @version 2018/06/26 14:41:42
 */
public class ChartCanvas extends Region {

    /** @FIXME Read from css file. */
    private static final Color Buy = Color.rgb(32, 151, 77);

    /** @FIXME Read from css file. */
    private static final Color Sell = Color.rgb(247, 105, 77);

    /** The candle width. */
    private static final int BarWidth = 3;

    /** The chart node. */
    private final ChartView chart;

    /** The horizontal axis. (shortcut) */
    private final Axis axisX;

    /** The vertical axis. (shortcut) */
    private final Axis axisY;

    /** The notificator. */
    private final Notificator notificator = I.make(Notificator.class);

    /** Chart UI */
    private final LineMark backGridVertical;

    /** Chart UI */
    private final LineMark backGridHorizontal;

    /** Chart UI */
    private final LineMark mouseTrackVertical;

    /** Chart UI */
    private final LineMark mouseTrackHorizontal;

    /** Chart UI */
    private final LineMark notifyPrice;

    /** Chart UI */
    private final LineMark latestPrice;

    /** Chart UI */
    private final LineMark orderBuyPrice;

    /** Chart UI */
    private final LineMark orderSellPrice;

    /** Chart UI */
    private final LineChart chartBottom = new LineChart();

    /** Chart UI */
    private final Canvas candles = new Canvas();

    /** Chart UI */
    private final Canvas candleLatest = new Canvas();

    /** Flag whether candle chart shoud layout on the next rendering phase or not. */
    public final LayoutAssistant layoutCandle = new LayoutAssistant(this);

    /** Flag whether candle chart shoud layout on the next rendering phase or not. */
    public final LayoutAssistant layoutCandleLatest = new LayoutAssistant(this);

    /**
     * Chart canvas.
     * 
     * @param chart
     * @param axisX
     * @param axisY
     */
    public ChartCanvas(ChartView chart, Axis axisX, Axis axisY) {
        this.chart = chart;
        this.axisX = axisX;
        this.axisY = axisY;
        this.backGridVertical = new LineMark(axisX.forGrid, axisX, ChartClass.BackGrid);
        this.backGridHorizontal = new LineMark(axisY.forGrid, axisY, ChartClass.BackGrid);
        this.mouseTrackVertical = new LineMark(axisX, ChartClass.MouseTrack);
        this.mouseTrackHorizontal = new LineMark(axisY, ChartClass.MouseTrack);
        this.notifyPrice = new LineMark(axisY, ChartClass.PriceSignal);
        this.latestPrice = new LineMark(axisY, ChartClass.PriceLatest);
        this.orderBuyPrice = new LineMark(axisY, ChartClass.OrderSupport, Side.BUY);
        this.orderSellPrice = new LineMark(axisY, ChartClass.OrderSupport, Side.SELL);
        this.candles.widthProperty().bind(widthProperty());
        this.candles.heightProperty().bind(heightProperty());
        this.candleLatest.widthProperty().bind(widthProperty());
        this.candleLatest.heightProperty().bind(heightProperty());

        this.chartBottom.create(tick -> tick.longVolume().toDouble() * 2, Buy);
        this.chartBottom.create(tick -> tick.shortVolume().toDouble() * 2, Sell);

        Viewtify.clip(this);

        layoutCandle.layoutBy(widthProperty(), heightProperty())
                .layoutBy(axisX.scroll.valueProperty(), axisX.scroll.visibleAmountProperty())
                .layoutBy(axisY.scroll.valueProperty(), axisY.scroll.visibleAmountProperty())
                .layoutBy(chart.ticker.observe().switchMap(ticker -> ticker.add.startWithNull()));
        layoutCandleLatest.layoutBy(widthProperty(), heightProperty())
                .layoutBy(axisX.scroll.valueProperty(), axisX.scroll.visibleAmountProperty())
                .layoutBy(axisY.scroll.valueProperty(), axisY.scroll.visibleAmountProperty())
                .layoutBy(chart.ticker.observe().switchMap(ticker -> ticker.update.startWithNull()));

        visualizeNotifyPrice();
        visualizeOrderPrice();
        visualizeLatestPrice();
        visualizeMouseTrack();

        getChildren()
                .addAll(backGridVertical, backGridHorizontal, notifyPrice, orderBuyPrice, orderSellPrice, latestPrice, candles, candleLatest, mouseTrackHorizontal, mouseTrackVertical, chartBottom);
    }

    /**
     * Visualize mouse tracker in chart.
     */
    private void visualizeMouseTrack() {
        TickLable labelX = mouseTrackVertical.createLabel();
        TickLable labelY = mouseTrackHorizontal.createLabel();

        // track on move
        setOnMouseMoved(e -> {
            double x = axisX.getValueForPosition(e.getX());
            labelX.value.set(x);
            labelY.value.set(axisY.getValueForPosition(e.getY()));

            mouseTrackVertical.layoutLine.requestLayout();
            mouseTrackHorizontal.layoutLine.requestLayout();

            // upper info
            chart.ticker.v.findByEpochSecond((long) x).isPresent(tick -> {
                chart.selectDate.text(Chrono.system(tick.start).format(Chrono.DateTime));
                chart.selectHigh.text("H " + tick.highPrice().scale(0));
                chart.selectLow.text("L " + tick.lowPrice().scale(0));
            });
        });

        // remove on exit
        setOnMouseExited(e -> {
            labelX.value.set(-1);
            labelY.value.set(-1);

            mouseTrackVertical.layoutLine.requestLayout();
            mouseTrackHorizontal.layoutLine.requestLayout();

            // upper info
            chart.selectDate.text("");
            chart.selectHigh.text("");
            chart.selectLow.text("");
        });
    }

    /**
     * Visualize notifiable price in chart.
     */
    private void visualizeNotifyPrice() {
        addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            double clickedPosition = e.getY();

            // check price range to add or remove
            for (TickLable mark : notifyPrice.labels) {
                double markedPosition = axisY.getPositionForValue(mark.value.get());

                if (Math.abs(markedPosition - clickedPosition) < 5) {
                    notifyPrice.remove(mark);
                    return;
                }
            }

            Num price = Num.of(Math.floor(axisY.getValueForPosition(clickedPosition)));
            TickLable label = notifyPrice.createLabel(price);

            label.add(chart.market.v.signalByPrice(price).on(Viewtify.UIThread).to(exe -> {
                notificator.priceSignal.notify("Rearch to " + price);
                notifyPrice.remove(label);
            }));
        });
    }

    /**
     * Visualize order price in chart.
     */
    private void visualizeOrderPrice() {
        chart.market.observe().to(market -> {
            market.orders.added.on(Viewtify.UIThread).to(o -> {
                LineMark mark = o.isBuy() ? orderBuyPrice : orderSellPrice;
                TickLable label = mark.createLabel(o.price);

                o.state.observe().take(State.CANCELED, State.COMPLETED).take(1).on(Viewtify.UIThread).to(() -> {
                    mark.remove(label);
                });
            });
        });
    }

    /**
     * Visualize latest price in chart.
     */
    private void visualizeLatestPrice() {
        TickLable latest = latestPrice.createLabel();

        chart.market.observe().to(market -> {
            market.timeline.map(e -> e.price).diff().on(Viewtify.UIThread).to(price -> {
                latest.value.set(price.toDouble());
                latestPrice.layoutLine.requestLayout();
            });
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layoutChildren() {
        // draw back lines
        backGridVertical.draw();
        backGridHorizontal.draw();
        mouseTrackVertical.draw();
        mouseTrackHorizontal.draw();
        notifyPrice.draw();
        latestPrice.draw();
        orderBuyPrice.draw();
        orderSellPrice.draw();

        drawCandle();
    }

    /**
     * Draw candle chart.
     */
    private void drawCandle() {
        layoutCandle.layout(() -> {
            // estimate visible range
            long start = (long) axisX.computeVisibleMinValue();
            long end = Math.min((long) axisX.computeVisibleMaxValue(), chart.ticker.v.last().start.toEpochSecond());
            long span = chart.ticker.v.span.duration.getSeconds();
            int visibleSize = (int) ((end - start) / span) + 1;
            int visibleStartIndex = (int) ((start - chart.ticker.v.first().start.toEpochSecond()) / span);

            // Redraw all candles.
            GraphicsContext gc = candles.getGraphicsContext2D();
            gc.clearRect(0, 0, candles.getWidth(), candles.getHeight());

            // draw chart in visible range
            chartBottom.initialize(visibleSize);
            chart.ticker.v.each(visibleStartIndex, visibleSize, tick -> {
                double x = axisX.getPositionForValue(tick.start.toEpochSecond());
                double open = axisY.getPositionForValue(tick.openPrice.toDouble());
                double close = axisY.getPositionForValue(tick.closePrice().toDouble());
                double high = axisY.getPositionForValue(tick.highPrice().toDouble());
                double low = axisY.getPositionForValue(tick.lowPrice().toDouble());

                gc.setStroke(open < close ? Sell : Buy);
                gc.setLineWidth(1);
                gc.strokeLine(x, high, x, low);
                gc.setLineWidth(BarWidth);
                gc.strokeLine(x, open, x, close);

                chartBottom.calculate(x, tick);
            });
            chartBottom.draw();
        });

        layoutCandleLatest.layout(() -> {
            GraphicsContext gc = candleLatest.getGraphicsContext2D();
            gc.clearRect(0, 0, candleLatest.getWidth(), candleLatest.getHeight());

            Tick tick = chart.ticker.v.last();

            double x = axisX.getPositionForValue(tick.start.toEpochSecond());
            double open = axisY.getPositionForValue(tick.openPrice.toDouble());
            double close = axisY.getPositionForValue(tick.closePrice().toDouble());
            double high = axisY.getPositionForValue(tick.highPrice().toDouble());
            double low = axisY.getPositionForValue(tick.lowPrice().toDouble());

            gc.setStroke(open < close ? Sell : Buy);
            gc.setLineWidth(1);
            gc.strokeLine(x, high, x, low);
            gc.setLineWidth(BarWidth);
            gc.strokeLine(x, open, x, close);
        });
    }

    /**
     * @version 2018/01/15 20:52:31
     */
    private class LineChart extends Group {

        /** The poly line. */
        private final List<Line> lines = new CopyOnWriteArrayList();

        /** The current x-position. */
        private int index = 0;

        /** The x-point of values. */
        private double[] valueX = new double[0];

        /**
         * 
         */
        private LineChart() {
        }

        /**
         * Create new line chart.
         * 
         * @param converter
         * @param color
         */
        private void create(ToDoubleFunction<Tick> converter, Color color) {
            lines.add(new Line(converter, color));
        }

        /**
         * Initialize.
         * 
         * @param size
         */
        private void initialize(int size) {
            index = 0;

            for (Line line : lines) {
                line.valueMax = 0;
            }

            // ensure size
            if (valueX.length < size) {
                valueX = new double[size * 2];

                for (Line line : lines) {
                    line.valueY = new double[size * 2];
                }
            }
        }

        /**
         * Pre-calculate each values.
         * 
         * @param x
         * @param tick
         */
        private void calculate(double x, Tick tick) {
            for (Line line : lines) {
                line.calculate(x, tick);
            }
            valueX[index++] = x;
        }

        /**
         * Finish drawing chart line.
         */
        private void draw() {
            double height = getHeight();
            double scale = lines.stream().map(Line::scale).min(Comparator.naturalOrder()).get();
            GraphicsContext gc = candles.getGraphicsContext2D();

            for (Line line : lines) {
                for (int i = 0; i < line.valueY.length; i++) {
                    line.valueY[i] = height - line.valueY[i] * scale;
                }

                gc.setLineWidth(1);
                gc.setStroke(line.color);
                gc.strokePolyline(valueX, line.valueY, index);
            }
        }

        /**
         * @version 2018/01/15 20:38:38
         */
        private class Line {

            /** The maximum height. */
            private final double heightMax = 40;

            /** The value converter. */
            private final ToDoubleFunction<Tick> converter;

            /** The color of line. */
            private final Color color;

            /** The y-point of values. */
            private double[] valueY = new double[0];

            /** The max value. */
            private double valueMax = 0;

            /**
             * @param converter
             * @param color
             */
            private Line(ToDoubleFunction<Tick> converter, Color color) {
                this.converter = converter;
                this.color = color;
            }

            /**
             * Calculate value.
             * 
             * @param tick
             * @return
             */
            private void calculate(double x, Tick tick) {
                double calculated = converter.applyAsDouble(tick);

                valueMax = Math.max(valueMax, calculated);
                this.valueY[index] = calculated;
            }

            /**
             * Calculate scale.
             * 
             * @return
             */
            private double scale() {
                return heightMax < valueMax ? heightMax / valueMax : 1;
            }
        }
    }

    /**
     * @version 2018/01/12 21:54:07
     */
    private class TopMark extends Path {

    }

    /**
     * @version 2018/01/09 0:19:26
     */
    private class LineMark extends Path {

        /** The class name. */
        private final Enum[] classNames;

        /** The model. */
        private final List<TickLable> labels;

        /** The associated axis. */
        private final Axis axis;

        /** The layout manager. */
        private final LayoutAssistant layoutLine = layoutCandle.sub();

        /**
         * @param classNames
         */
        private LineMark(Axis axis, Enum... classNames) {
            this(new CopyOnWriteArrayList(), axis, classNames);
        }

        /**
         * @param className
         * @param labels
         */
        private LineMark(List<TickLable> labels, Axis axis, Enum... classNames) {
            this.classNames = classNames;
            this.labels = labels;
            this.axis = axis;

            Viewtify.clip(this);
            StyleHelper.of(this).style(ChartClass.Line).style(classNames);
        }

        /**
         * Create new mark.
         * 
         * @return
         */
        private TickLable createLabel() {
            return createLabel(null);
        }

        /**
         * Create new mark.
         * 
         * @return
         */
        private TickLable createLabel(Num price) {
            TickLable label = axis.createLabel(classNames);
            if (price != null) label.value.set(price.toDouble());
            labels.add(label);

            layoutLine.requestLayout();

            return label;
        }

        /**
         * Dispose mark.
         * 
         * @param mark
         */
        private void remove(TickLable mark) {
            labels.remove(mark);
            mark.dispose();
            layoutLine.requestLayout();
        }

        /**
         * Draw mark.
         */
        private void draw() {
            layoutLine.layout(() -> {
                ObservableList<PathElement> paths = getElements();
                int pathSize = paths.size();
                int labelSize = labels.size();

                if (pathSize > labelSize * 2) {
                    paths.remove(labelSize * 2, pathSize);
                    pathSize = labelSize * 2;
                }

                for (int i = 0; i < labelSize; i++) {
                    MoveTo move;
                    LineTo line;

                    if (i * 2 < pathSize) {
                        move = (MoveTo) paths.get(i * 2);
                        line = (LineTo) paths.get(i * 2 + 1);
                    } else {
                        move = new MoveTo();
                        line = new LineTo();
                        paths.addAll(move, line);
                    }

                    double value = labels.get(i).position();

                    if (axis.isHorizontal()) {
                        move.setX(value);
                        move.setY(0);
                        line.setX(value);
                        line.setY(getHeight());
                    } else {
                        move.setX(0);
                        move.setY(value);
                        line.setX(getWidth());
                        line.setY(value);
                    }
                }
            });
        }
    }
}