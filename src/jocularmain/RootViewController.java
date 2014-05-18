package jocularmain;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;
import utils.Observation;
import utils.SqSolution;

public class RootViewController implements Initializable {

    ManagedChart smartChart;
    private static JocularMain jocularMain;

    public static void setMainApp(JocularMain main) {
        jocularMain = main;
    }

    @FXML
    LineChart<Number, Number> chart;

    @FXML
    Label outputLabel;

    @FXML
    RadioButton markerRBnone;

    @FXML
    RadioButton markerRBtrimLeft;

    @FXML
    RadioButton markerRBdLeft;

    @FXML
    RadioButton markerRBdRight;

    @FXML
    RadioButton markerRBrLeft;

    @FXML
    RadioButton markerRBrRight;

    @FXML
    RadioButton markerRBtrimRight;

    @FXML
    ToggleButton hideToggleButton;

    @FXML
    ListView solutionList;

    @FXML
    void respondToRightButtonClick() {
        revertToOriginalAxisScaling();
    }

    @FXML
    void showSampleDataDialog() {
        jocularMain.showSampleDataDialog();
    }

    @FXML
    void generateErrorMsg() {
        jocularMain.showErrorDialog("So, you thought that you were getting away scott-free.  Think again!");
    }

    /**
     * a node which displays a value on hover, but is otherwise empty
     */
    class HoveredNode extends StackPane {

        HoveredNode(int readingNumber, double intensity) {
            setOnMouseEntered(e -> outputLabel.setText(String.format("RdgNbr %d Intensity %.2f", readingNumber, intensity)));
            setOnMouseExited(e -> outputLabel.setText(""));
        }
    }

    @FXML
    public void computeCandidates() {
        try {
            SolutionStats solutionStats = new SolutionStats();

            List<SqSolution> solutions = SqSolver.computeCandidates(
                jocularMain, solutionStats, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);

            ObservableList<String> items = FXCollections.observableArrayList();
            items.add(String.format("Number of transition pairs considered: %,d   Number of valid transition pairs: %,d",
                                    solutionStats.numTransitionPairsConsidered,
                                    solutionStats.numValidTransitionPairs));
            for (SqSolution solution : solutions) {
                items.add(solution.toString());
            }

            solutionList.setItems(items);
        } catch (IllegalArgumentException e) {
            //jocularMain.showErrorDialog(e.getMessage());
        }
    }

    public void clearSolutionList() {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        solutionList.setItems(items);
    }

    @FXML
    public void applyTrims() {

        if (jocularMain.getCurrentObservation() == null) {
            return;
        }

        int maxRightTrim = jocularMain.getCurrentObservation().lengthOfDataColumns - 1;
        int minLeftTrim = 0;

        int leftTrim;
        XYChartMarker leftTrimMarker = smartChart.getMarker("trimLeft");
        if (leftTrimMarker.isInUse()) {
            leftTrim = (int) Math.ceil(leftTrimMarker.getXValue());
        } else {
            leftTrim = minLeftTrim;
        }

        if (!jocularMain.inRange(leftTrim)) {
            leftTrim = minLeftTrim;
        }

        int rightTrim;
        XYChartMarker rightTrimMarker = smartChart.getMarker("trimRight");
        if (rightTrimMarker.isInUse()) {
            rightTrim = (int) Math.floor(rightTrimMarker.getXValue());
        } else {
            rightTrim = maxRightTrim;
        }

        if (!jocularMain.inRange(rightTrim)) {
            rightTrim = maxRightTrim;
        }

        // Here we deal with the user setting the left trim to the right of
        // the right trim.  We'll make it work rather than throwing an exception.
        if (rightTrim < leftTrim) {
            int temp;
            temp = leftTrim;
            leftTrim = rightTrim;
            rightTrim = temp;
        }

        jocularMain.getCurrentObservation().setLeftTrimPoint(leftTrim);
        jocularMain.getCurrentObservation().setRightTrimPoint(rightTrim);
        showDataWithTheoreticalLightCurve(jocularMain.getCurrentObservation(), jocularMain.getCurrentSolution());
    }

    @FXML
    void undoTrims() {
        if (jocularMain.getCurrentObservation() == null) {
            return;
        }

        int maxRightTrim = jocularMain.getCurrentObservation().lengthOfDataColumns - 1;
        int minLeftTrim = 0;

        jocularMain.getCurrentObservation().setLeftTrimPoint(minLeftTrim);
        jocularMain.getCurrentObservation().setRightTrimPoint(maxRightTrim);
        showDataWithTheoreticalLightCurve(jocularMain.getCurrentObservation(), jocularMain.getCurrentSolution());
    }

    public void showDataWithTheoreticalLightCurve(Observation sampleData, SqSolution solution) {
        XYChart.Series<Number, Number> series;
        series = new XYChart.Series<Number, Number>();
        series.setName("Data");
        XYChart.Data<Number, Number> data;

        int numDataPoints = sampleData.obsData.length;

        for (int i = 0; i < numDataPoints; i++) {
            data = new XYChart.Data(sampleData.readingNumbers[i], sampleData.obsData[i]);
            HoveredNode hNode = new HoveredNode(sampleData.readingNumbers[i], sampleData.obsData[i]);
            data.setNode(hNode);
            series.getData().add(data);
        }

        // Remove all series from the chart
        chart.getData().clear();

        // Add the data curve --- this uses symbols at the data points
        chart.getData().add(series);

        // Add the theoretical light curve line plot --- no symbols at the data points
        chart.getData().add(getTheoreticalLightCurve(sampleData, solution));
    }

    public void addSolutionCurve(Observation obs, SqSolution solution) {
        chart.getData().add(getTheoreticalLightCurve(obs, solution));
    }

    private XYChart.Series<Number, Number> getTheoreticalLightCurve(Observation sampleData, SqSolution solution) {
        XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
        series.setName("Solution");
        XYChart.Data<Number, Number> data;
        int numDataPoints = sampleData.lengthOfDataColumns;

        // A dEvent in the range of -1.0 to 0.0 affects the value of
        // observation[0].  Therefore we need to plot the theoretical
        // light curve from -1 to righthand edge.  validLeftEdge provides
        // this adjustment.
        int validLeftEdge = -1;

        // Create the series that plots the theoretical light curve without trims.
        if (Double.isNaN(solution.D) || solution.D < validLeftEdge) {
            data = new XYChart.Data(validLeftEdge, solution.A);
            series.getData().add(data);
            data = new XYChart.Data(solution.R, solution.A);
            series.getData().add(data);
            data = new XYChart.Data(solution.R, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(numDataPoints - 1, solution.B);
            series.getData().add(data);
        } else if (Double.isNaN(solution.R) || solution.R > numDataPoints) {
            data = new XYChart.Data(validLeftEdge, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(solution.D, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(solution.D, solution.A);
            series.getData().add(data);
            data = new XYChart.Data(numDataPoints - 1, solution.A);
            series.getData().add(data);
        } else {
            data = new XYChart.Data(validLeftEdge, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(solution.D, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(solution.D, solution.A);
            series.getData().add(data);
            data = new XYChart.Data(solution.R, solution.A);
            series.getData().add(data);
            data = new XYChart.Data(solution.R, solution.B);
            series.getData().add(data);
            data = new XYChart.Data(numDataPoints - 1, solution.B);
            series.getData().add(data);
        }

        return series;
    }

    @FXML
    void displayChartZoomPanMarkHelp() {
        jocularMain.showHelpDialog("Help/chart.help.html");
    }

    @FXML
    void displayMarkerSelectionHelp() {
        jocularMain.showHelpDialog("Help/marker.help.html");
    }

    private String markerSelectedName = "none";

    @FXML
    void noneRBaction() {
        markerSelectedName = "none";
    }

    @FXML
    void trimLeftRBaction() {
        markerSelectedName = "trimLeft";
        makeMarkersVisible();
    }

    @FXML
    void dLeftRBaction() {
        markerSelectedName = "dLeft";
        makeMarkersVisible();
    }

    @FXML
    void dRightRBaction() {
        markerSelectedName = "dRight";
        makeMarkersVisible();
    }

    @FXML
    void rLeftRBaction() {
        markerSelectedName = "rLeft";
        makeMarkersVisible();
    }

    @FXML
    void rRightRBaction() {
        markerSelectedName = "rRight";
        makeMarkersVisible();
    }

    @FXML
    void trimRightRBaction() {
        markerSelectedName = "trimRight";
        makeMarkersVisible();
    }

    private void makeChartDataMouseTransparent() {
        Node chartBackground = chart.lookup(".chart-plot-background");
        StableTicksAxis xAxis = (StableTicksAxis) chart.getXAxis();
        StableTicksAxis yAxis = (StableTicksAxis) chart.getYAxis();
        for (Node n : chartBackground.getParent().getChildrenUnmodifiable()) {
            if (n != chartBackground && n != xAxis && n != yAxis) {
                n.setMouseTransparent(true);
            }
        }
    }

    private void revertToOriginalAxisScaling() {
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setAutoRanging(true);
    }

    private void makeMarkersVisible() {
        hideToggleButton.setSelected(false);
        hideUnhideMarkers();
    }

    @FXML
    private void hideUnhideMarkers() {
        boolean v = !hideToggleButton.isSelected();
        smartChart.getMarker("trimLeft").setVisible(v);
        smartChart.getMarker("dLeft").setVisible(v);
        smartChart.getMarker("dRight").setVisible(v);
        smartChart.getMarker("rLeft").setVisible(v);
        smartChart.getMarker("rRight").setVisible(v);
        smartChart.getMarker("trimRight").setVisible(v);
    }

    @FXML
    private void eraseSelection() {
        if (!"none".equals(markerSelectedName)) {
            smartChart.getMarker(markerSelectedName).setInUse(false);

            markerRBnone.setSelected(true);
            markerRBnone.requestFocus();
            markerSelectedName = "none";
        }
    }

    private void setupDisplayOfCoordinates() {
        Node chartBackground = chart.lookup(".chart-plot-background");
        chartBackground.setOnMouseMoved(this::showCoordinates);
        chartBackground.setOnMouseExited(this::hideCoordinates);
    }

    private void hideCoordinates(MouseEvent mouseEvent) {
        outputLabel.setText("");
    }

    private void showCoordinates(MouseEvent mouseEvent) {
        outputLabel.setText(String.format(
            "   x = %7.2f  y = %7.2f",
            chart.getXAxis().getValueForDisplay(mouseEvent.getX()),
            chart.getYAxis().getValueForDisplay(mouseEvent.getY())
        ));
    }

    private void setupLeftClickResponder() {
        Node chartBackground = chart.lookup(".chart-plot-background");
        chartBackground.setOnMouseClicked(this::respondToLeftMouseButtonClick);
    }

    private void respondToLeftMouseButtonClick(MouseEvent mouseEvent) {
        if ("none".equals(markerSelectedName)) {
            return;
        }
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            double x = (double) chart.getXAxis().getValueForDisplay(mouseEvent.getX());
            x = Math.floor(x) + 0.5;
            smartChart.getMarker(markerSelectedName).setxValue(x).setInUse(true);

            switch (markerSelectedName) {
                case "trimLeft":
                    markerRBtrimRight.setSelected(true);
                    markerRBtrimRight.requestFocus();
                    markerSelectedName = "trimRight";
                    break;
                case "trimRight":
                    markerRBnone.setSelected(true);
                    markerRBnone.requestFocus();
                    markerSelectedName = "none";
                    break;

                case "dLeft":
                    markerRBdRight.setSelected(true);
                    markerRBdRight.requestFocus();
                    markerSelectedName = "dRight";
                    break;
                case "dRight":
                    markerRBrLeft.setSelected(true);
                    markerRBrLeft.requestFocus();
                    markerSelectedName = "rLeft";
                    break;
                case "rLeft":
                    markerRBrRight.setSelected(true);
                    markerRBrRight.requestFocus();
                    markerSelectedName = "rRight";
                    break;
                case "rRight":
                    markerRBnone.setSelected(true);
                    markerRBnone.requestFocus();
                    markerSelectedName = "none";
                    break;
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        setupLeftClickResponder();

        // Add zoom, pan, and marker managers to 'chart'.  Note that this will
        // also turn off animation in the chart and axes and turn off auto-ranging
        // on the axes.
        smartChart = new ManagedChart(chart);

        createAndAddNamedVerticalMarkers();

        //setupDisplayOfCoordinates();
        //makeChartDataMouseTransparent();
        //
        /**
         * In order to have the 'markers' adapt to window resizing and axes // changes, they have to be redrawn continuously. So we register
         * a // new handler with AnimationTimer. That handler gets called about // 60 times a second.
         */
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                smartChart.repaintMarkers();
            }
        }.start();

    }

    private XYChartMarker dLeftMarker;
    private XYChartMarker dRightMarker;
    private XYChartMarker rLeftMarker;
    private XYChartMarker rRightMarker;

    void createAndAddNamedVerticalMarkers() {
        XYChartMarker trimLeftMarker = new XYChartMarker("trimLeft", smartChart).setColor(Color.BLUE).setWidth(2);
        dLeftMarker = new XYChartMarker("dLeft", smartChart).setColor(Color.RED).setWidth(2);
        dRightMarker = new XYChartMarker("dRight", smartChart).setColor(Color.RED).setWidth(2);
        rLeftMarker = new XYChartMarker("rLeft", smartChart).setColor(Color.GREEN).setWidth(2);
        rRightMarker = new XYChartMarker("rRight", smartChart).setColor(Color.GREEN).setWidth(2);
        XYChartMarker trimRightMarker = new XYChartMarker("trimRight", smartChart).setColor(Color.BLUE).setWidth(2);

        smartChart.addMarker(trimLeftMarker);
        smartChart.addMarker(dLeftMarker);
        smartChart.addMarker(dRightMarker);
        smartChart.addMarker(rLeftMarker);
        smartChart.addMarker(rRightMarker);
        smartChart.addMarker(trimRightMarker);
    }

}
