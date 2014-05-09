/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jocularmain;

import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;

/**
 *
 * @author Bob Anderson
 */
public class XYChartMarker
{

    private final StackPane overlay;
    private final XYChart xychart;
    private final String name;
    private Color lineColor = Color.RED;
    private double xValue = 0.0;
    private int strokeWidth = 1;
    private boolean inUse = false;
    private boolean visible = true;
    private final Line markerLine;

    public XYChartMarker(String markerName, ManagedChart smartChart) {
        overlay = smartChart.overlay;
        xychart = smartChart.xychart;
        name = markerName;
        markerLine = new Line();
        markerLine.setStrokeWidth(strokeWidth);
        markerLine.setStroke(lineColor);
        markerLine.setMouseTransparent(true);
    }

    public XYChartMarker setColor(Color newColor) {
        lineColor = newColor;
        return this;
    }

    public boolean isInUse() {
        return inUse;
    }

    public XYChartMarker setInUse(boolean inUseState) {
        inUse = inUseState;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public XYChartMarker setVisible(boolean visibleState) {
        visible = visibleState;
        return this;
    }

    public XYChartMarker setxValue(double newxValue) {
        xValue = newxValue;
        return this;
    }

    public XYChartMarker setWidth(int newWidth) {
        strokeWidth = newWidth;
        return this;
    }

    public double getXValue() {
        return xValue;
    }

    public String name() {
        return name;
    }

    public Line markerLine() {
        return markerLine;
    }

    public void draw() {
        if (!inUse || !visible) {
            overlay.getChildren().remove(markerLine);
            return;
        }

        StableTicksAxis xAxis = (StableTicksAxis) xychart.getXAxis();

        if (xAxis.isValueOnAxis(xValue)) {

            // Erase previous marker.  This works even if the markerLine is
            // not yet a child of the overlay.
            overlay.getChildren().remove(markerLine);

            Node chartBackground = xychart.lookup(".chart-plot-background");

            double yHeight = chartBackground.getBoundsInParent().getHeight();

            final double shiftX = xSceneShift(chartBackground);
            final double shiftY = ySceneShift(chartBackground);

            double xScale = xAxis.getScale();
            double xZero = xAxis.getLowerBound();

            double valueShift = (xValue - xZero) * xScale;

            markerLine.setStartX(0.0);
            markerLine.setStartY(0.0);
            markerLine.setEndX(0.0);
            markerLine.setEndY(yHeight);
            markerLine.setStroke(lineColor);
            markerLine.setStrokeWidth(strokeWidth);
            markerLine.setStrokeLineCap(StrokeLineCap.ROUND);

            markerLine.setTranslateX(shiftX + valueShift - strokeWidth / 2.0);
            markerLine.setTranslateY(shiftY - strokeWidth / 2.0);

            overlay.getChildren().add(markerLine);
        }
    }

    //Recursive calls: because marker lines are drawn in the coordinate system
    // of the overlay, we terminate the recursive descent at overlay.
    private double xSceneShift(Node node) {
        if (node==overlay) return 0.0;
        return node.getParent() == null ? 0 : node.getBoundsInParent().getMinX()
                + xSceneShift(node.getParent());
    }

    private double ySceneShift(Node node) {
        if (node==overlay) return 0.0;
        return node.getParent() == null ? 0 : node.getBoundsInParent().getMinY()
                + ySceneShift(node.getParent());
    }

}
