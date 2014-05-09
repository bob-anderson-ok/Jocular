/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gillius.jfxutils.chart;

import jocularmain.XYChartMarker;
import java.util.HashMap;
import java.util.Map;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author bob
 */
public class ManagedChart {

    public final XYChart xychart;
    private final Map<String, XYChartMarker> markers;
    public StackPane overlay;

    public ManagedChart(XYChart chart)
    {
        markers = new HashMap<>();
        this.xychart = chart;
        addZoomAndPanManagers();
    }

    public void addMarker(XYChartMarker newMarker)
    {
        markers.put(newMarker.name(), newMarker);
        //System.out.println("Vertical line added --- name is " + name);
        //System.out.println("markers.size = " + markers.size());
        //System.out.println("marker string: " + markers.get(name));
    }

    public XYChartMarker getMarker(String markerName)
    {
        return markers.get(markerName);
    }

    public double getMarkerValue(String markerName) {
        if (!markers.containsKey(markerName)) {
            return Double.NaN;
        }
        return markers.get(markerName).getXValue();
    }
    
    public boolean setMarkerValue(String markerName, double newValue)
    {
        if (!markers.containsKey(markerName)) {
            return false;
        }

        markers.get(markerName).setxValue(newValue);

        return true;
    }

    public void repaintMarkers()
    {
        for (XYChartMarker chartMark : markers.values()) {
            // First remove the child line from the overlay to erase it.
            overlay.getChildren().remove(chartMark.markerLine());
            // Then the draw() method re-adds the marker line as child and
            // thus display it properly dimensioned and position.
            chartMark.draw();
        }
    }

    public boolean removeMarker(String name)
    {
        if (markers.containsKey(name)) {
            markers.remove(name);
            return true;
        } else {
            return false;
        }
    }

    private void addZoomAndPanManagers()
    {
        //xychart.getStylesheets().add("newCascadeStyleSheet.css");
        //System.out.println("style sheet " + xychart.getStylesheets());
        //xychart.getStyleClass().add("chart-series-line");
        // For the zoom and pan to work, it is necessary (I don't know why) for
        // there to be no animation on the x and y axis.  Here we enforce that
        // by overriding the as-designed settings
        xychart.getXAxis().setAnimated(false);
        xychart.getYAxis().setAnimated(false);

        // We also force animation off so that adding series data to the chart
        // will not be animated.
        xychart.setAnimated(false);

        ChartPanManager panner;
        panner = new ChartPanManager(xychart);
        // Setup a mouse event filter so that:
        //   panning only works via secondary (right) mouse button 
        //   without ctrl held down
        panner.setMouseFilter(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent)
            {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.isShortcutDown()) {
                    //let it through
                } else {
                    mouseEvent.consume();
                }
            }
        });

        // Add all the handlers that the panner needs.
        panner.start();

        // Setup a mouse event filter so that:
        //   zooming works only via primary (left) mouse button 
        //   without ctrl/cmd held down
        overlay = JFXChartUtil.setupZooming(xychart, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent)
            {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && !mouseEvent.isShortcutDown()) {
                    //let it through
                } else {
                    mouseEvent.consume();
                }
            }
        });
        overlay.setAlignment(Pos.TOP_LEFT);
    }
}
