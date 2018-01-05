/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package trademate.chart;

import static java.lang.Math.*;

import java.util.ArrayList;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;

import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;

/**
 * @version 2017/09/27 9:11:27
 */
public class LinearAxis extends Axis {

    /**
     * We use these for auto ranging to pick a user friendly tick unit. We handle tick units in the
     * range of 1e-10 to 1e+12
     */
    private static final double[] TickUnit = {0.0010d, 0.0025d, 0.0050d, 0.01d, 0.025d, 0.05d, 0.1d, 0.25d, 0.5d, 1.0d, 2.5d, 5.0d, 10.0d,
            25.0d, 50.0d, 100.0d, 250.0d, 500.0d, 1000.0d, 2500.0d, 5000.0d, 10000.0d, 25000.0d, 50000.0d, 100000.0d, 250000.0d, 500000.0d,
            1000000.0d, 2500000.0d, 5000000.0d};

    public final ObjectProperty<DoubleToObjectFunction<String>> tickLabelFormatter = new SimpleObjectProperty<>(this, "tickLabelFormatter", String::valueOf);

    private double lowVal = 0;

    private double uiRatio;

    public final ObjectProperty<double[]> units = new SimpleObjectProperty(this, "units", TickUnit);

    /**
     * @param tickLength
     * @param tickLabelDistance
     */
    public LinearAxis(int tickLength, int tickLabelDistance) {
        super(tickLength, tickLabelDistance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPositionForValue(final double v) {
        final double d = uiRatio * (v - lowVal);
        return isHorizontal() ? d : getHeight() - d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValueForPosition(double position) {
        if (!isHorizontal()) {
            position = getHeight() - position;
        }
        return position / uiRatio + lowVal;
    }

    private int unitIndex = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustLowerValue() {
        double max = logicalMaxValue.get();
        double min = logicalMinValue.get();
        double diff = max - min;
        double range = visibleRange.get();
        double low = computeLowerValue(max);
        double up = low + diff * range;
        if (up > max) {
            low = max - diff * range;
            visualMinValue.set(low);
        }
    }

    private double computeUpperValue(final double low) {
        final double max = logicalMaxValue.get();
        final double a = visibleRange.get();
        final double min = logicalMinValue.get();
        final double ll = max - min;
        return min(low + ll * a, max);
    }

    private int findNearestUnitIndex(double majorTickValueInterval) {
        // serach from unit list
        for (int i = 0; i < units.get().length; i++) {
            if (majorTickValueInterval < units.get()[i]) {
                return i;
            }
        }
        return units.get().length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void computeAxisProperties(double width, double height) {
        ticks.clear();

        final double low = computeLowerValue(logicalMaxValue.get());
        final double up = computeUpperValue(low);
        final double axisLength = getAxisLength(width, height);
        if (low == up || Double.isNaN(low) || Double.isNaN(up) || axisLength <= 0) {
            return;
        }

        lowVal = low;
        visualMaxValue.set(up);

        // layout scroll bar
        double max = logicalMaxValue.get();
        double min = logicalMinValue.get();

        if (low == min && up == max) {
            scrollBarValue.set(-1);
            scrollBarSize.set(1);
        } else {
            double logicalDiff = max - min;
            double visualDiff = up - low;
            scrollBarValue.set((low - min) / (logicalDiff - visualDiff));
            scrollBarSize.set(visualDiff / logicalDiff);
        }

        // search sutable unit
        double visibleValueDistance = up - low;
        int nextUnitIndex = findNearestUnitIndex(visibleValueDistance / tickNumber.get());
        boolean usePrevious = nextUnitIndex == unitIndex;

        double nextUnitSize = units.get()[nextUnitIndex];

        double visibleStartUnitBasedValue = floor(low / nextUnitSize) * nextUnitSize;
        double uiRatio = axisLength / visibleValueDistance;

        int actualVisibleMajorTickCount = (int) (ceil((up - visibleStartUnitBasedValue) / nextUnitSize));

        if (actualVisibleMajorTickCount <= 0 || 2000 < actualVisibleMajorTickCount) {
            return;
        }

        this.uiRatio = uiRatio;

        boolean isH = isHorizontal();

        ObservableList<AxisLabel> labels = getLabels();
        if (!usePrevious) {
            labels.clear();
        }

        ArrayList<AxisLabel> unused = new ArrayList<>(labels);
        ArrayList<AxisLabel> labelList = new ArrayList<>(actualVisibleMajorTickCount + 1);

        for (int i = 0; i <= actualVisibleMajorTickCount + 1; i++) {
            double value = visibleStartUnitBasedValue + nextUnitSize * i;
            if (value > up) {
                break;// i==k
            }
            double majorpos = uiRatio * (value - low);
            if (value >= low) {
                ticks.add(floor(isH ? majorpos : height - majorpos));
                boolean find = false;
                for (int t = 0, lsize = unused.size(); t < lsize; t++) {
                    AxisLabel a = unused.get(t);
                    if (a.id == value) {
                        labelList.add(a);
                        unused.remove(t);
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    AxisLabel a = new AxisLabel();
                    a.id = value;
                    Text text = new Text(tickLabelFormatter.get().apply(value));
                    text.getStyleClass().add("tick-label");
                    a.node = text;
                    labelList.add(a);
                }
            }
        } // end for

        // これで大丈夫か？
        labels.removeAll(unused);
        for (int i = 0, e = labelList.size(); i < e; i++) {
            AxisLabel axisLabel = labelList.get(i);

            if (!labels.contains(axisLabel)) {
                labels.add(i, axisLabel);
            }
        }
    }
}