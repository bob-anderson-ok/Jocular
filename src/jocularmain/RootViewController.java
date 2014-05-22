package jocularmain;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import static jocularmain.PlotType.*;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;
import utils.JocularUtils;
import utils.Observation;
import utils.SqSolution;

public class RootViewController implements Initializable {

    private static final int EMPTY_FIELD = -1;
    private static final int FIELD_ENTRY_ERROR = -2;

    private static ManagedChart smartChart;
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
    public void doOpenRecentFiles() {
        jocularMain.showInformationDialog("Open Recent Files:  not yet implemented.");
    }

    @FXML
    public void doReadLimovieFile() {
        jocularMain.showInformationDialog("Read Limovie File:  not yet implemented.");
    }

    @FXML
    public void doReadTangraFile() {
        jocularMain.showInformationDialog("Read Tangra File:  not yet implemented.");
    }

    @FXML
    public void doEstimateErrorBars() {
        jocularMain.showInformationDialog("Estimate Error Bars:  not yet implemented.");
    }

    @FXML
    public void doShowSubframeTimingBand() {
        jocularMain.showInformationDialog("Show Subframe Timing Band:  not yet implemented.");
    }

    @FXML
    public void estimateSigmaB() {
        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation from which to estimate baseline noise.");
            return;
        }
        double sigmaB = estimateSigma();
        if (sigmaB > 0.0) {
            sigmaBtext.setText(String.format("%.4f", sigmaB));
            eraseAllMarkers();
        }
    }

    @FXML
    public void estimateSigmaA() {
        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation from which to estimate event noise.");
            return;
        }
        double sigmaA = estimateSigma();
        if (sigmaA > 0.0) {
            sigmaAtext.setText(String.format("%.4f", sigmaA));
            eraseAllMarkers();
        }
    }

    public double estimateSigma() {
        if (dLeftMarker.isInUse() != dRightMarker.isInUse()) {
            jocularMain.showErrorDialog("D markers must be used in pairs.");
            return 0.0;
        }

        if (rLeftMarker.isInUse() != rRightMarker.isInUse()) {
            jocularMain.showErrorDialog("R markers must be used in pairs.");
            return 0.0;
        }

        ArrayList<Double> includedPoints = new ArrayList<>();

        for (int i = 0; i < jocularMain.getCurrentObservation().obsData.length; i++) {
            if (includedWithinMarkers(i, dLeftMarker, dRightMarker, rLeftMarker, rRightMarker)) {
                includedPoints.add(jocularMain.getCurrentObservation().obsData[i]);
            }
        }

        if (includedPoints.size() < 2) {
            jocularMain.showErrorDialog("Too few points to estimate noise.  Probably an error in setting selection markers.");
            return 0.0;
        }

        // Unbox the ArrayList<Double> into a double[]
        double[] pointsInEstimate = new double[includedPoints.size()];
        for (int i = 0; i < pointsInEstimate.length; i++) {
            pointsInEstimate[i] = (double) includedPoints.get(i);
        }

        double sigma = JocularUtils.calcSigma(pointsInEstimate);

        return sigma;

    }

    private boolean includedWithinMarkers(int index,
                                          XYChartMarker dLeft, XYChartMarker dRight,
                                          XYChartMarker rLeft, XYChartMarker rRight) {
        if (dLeft.isInUse()) {
            if (index > Math.floor(dLeft.getXValue()) && index < Math.ceil(dRight.getXValue())) {
                return true;
            }
        }

        if (rLeft.isInUse()) {
            if (index > Math.floor(rLeft.getXValue()) && index < Math.ceil(rRight.getXValue())) {
                return true;
            }
        }

        return false;
    }

    @FXML
    public void showIntroHelp() {
        jocularMain.showHelpDialog("Help/gettingstarted.help.html");

    }

    @FXML
    public void showAbout() {
        jocularMain.showHelpDialog("Help/about.help.html");
    }

    @FXML
    public void displayNoiseHelp() {
        jocularMain.showHelpDialog("Help/noisevalues.help.html");
    }

    @FXML
    public void displayMinMaxEventHelp() {
        jocularMain.showHelpDialog("Help/eventlimits.help.html");
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

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation data to process.");
            return;
        }

        // Erase the Solution List
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        solutionList.setItems(items);

        double sigmaB = validateSigmaBtext();
        if (sigmaB == FIELD_ENTRY_ERROR || sigmaB == EMPTY_FIELD) {
            items.add("No solutions: invalid Baseline Noise entry");
            solutionList.setItems(items);
            return;
        }

        double sigmaA = validateSigmaAtext();
        if (sigmaA == FIELD_ENTRY_ERROR) {
            items.add("No solutions: invalid Event Noise entry");
            solutionList.setItems(items);
            return;
        } else if (sigmaA == EMPTY_FIELD) {
            sigmaA = sigmaB;
            sigmaAtext.setText(sigmaBtext.getText());
        }

        int minEventSize = validateMinEventText();
        if (minEventSize == FIELD_ENTRY_ERROR) {
            items.add("No solutions: invalid minEventSize entry");
            solutionList.setItems(items);
            return;
        }

        int maxEventSize = validateMaxEventText();
        if (minEventSize == FIELD_ENTRY_ERROR) {
            items.add("No solutions: invalid maxEventSize entry");
            solutionList.setItems(items);
            return;
        }

        if (minEventSize > maxEventSize && maxEventSize != EMPTY_FIELD) {
            items.add("No solutions: minEventSize is > maxEventSize");
            solutionList.setItems(items);
            return;
        }

        SolutionStats solutionStats = new SolutionStats();

        List<SqSolution> solutions = SqSolver.computeCandidates(
            jocularMain, solutionStats,
            sigmaB, sigmaA,
            minEventSize, maxEventSize,
            dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);

        items = FXCollections.observableArrayList();
        if (solutions.isEmpty()) {
            items.add("No solutions were possible because of constraints on min and max event size.");
            solutionList.setItems(items);
            return;
        }

        items = FXCollections.observableArrayList();
        items.add(String.format("Number of transition pairs considered: %,d   Number of valid transition pairs: %,d",
                                solutionStats.numTransitionPairsConsidered,
                                solutionStats.numValidTransitionPairs));
        double probabilitySqWaveOverLine = Math.exp((solutionStats.straightLineAICc - solutions.get(0).aicc) / 2.0);
        if (probabilitySqWaveOverLine > 1000.0) {
            items.add(String.format("Straight line:  AICc=%11.2f  logL=%11.2f  relative probability of SqWave versus straight line > 1000",
                                    solutionStats.straightLineAICc,
                                    solutionStats.straightLineLogL));
        } else {
            items.add(String.format("Straight line:  AICc=%11.2f  logL=%11.2f  relative probability of SqWave versus straight line= %.1f",
                                    solutionStats.straightLineAICc,
                                    solutionStats.straightLineLogL,
                                    probabilitySqWaveOverLine));
        }
        for (SqSolution solution : solutions) {
            items.add(solution.toString());
        }

        solutionList.setItems(items);
    }

    @FXML
    TextField sigmaBtext;

    @FXML
    TextField sigmaAtext;

    private double validateSigmaBtext() {
        return sigmaValue(sigmaBtext.getText(), "Baseline Noise");
    }

    private double validateSigmaAtext() {
        return sigmaValue(sigmaAtext.getText(), "Event Noise");
    }

    private double sigmaValue(String text, String sourceId) {
        try {
            if (text.isEmpty()) {
                return EMPTY_FIELD;
            }
            double value = Double.parseDouble(text);
            if (value <= 0.0) {
                jocularMain.showErrorDialog(sourceId + " must be > 0.0");
                return FIELD_ENTRY_ERROR;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage());
            return FIELD_ENTRY_ERROR;
        }
    }

    @FXML
    TextField minEventText;

    @FXML
    TextField maxEventText;

    private int validateMinEventText() {
        return eventValue(minEventText.getText(), "Min Event");
    }

    private int validateMaxEventText() {
        return eventValue(maxEventText.getText(), "Max Event");
    }

    private int eventValue(String text, String sourceId) {
        try {
            if (text.isEmpty()) {
                return EMPTY_FIELD;
            }
            int value = Integer.parseInt(text);
            if (value <= 0) {
                jocularMain.showErrorDialog(sourceId + " must be > 0");
                return FIELD_ENTRY_ERROR;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage());
            return FIELD_ENTRY_ERROR;
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
            jocularMain.showErrorDialog("There is no observation to apply trims to.");
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

        addSeriesToChart(chart, series, STYLE_ObsPoints);

        addSeriesToChart(chart, getTheoreticalLightCurveSeries(sampleData, solution), STYLE_Sample);

        // Experimental --- how to get names of all series in a chart.  With
        // this tool, it becomes possible to remove a series by name.
        System.out.println("num series: " + chart.getData().size());
        for (int i = 0; i < chart.getData().size(); i++) {
            System.out.println("series " + i + " name is " + chart.getData().get(i).getName());
        }
    }

    private void setLegend() {

    }

    public void addSeriesToChart(XYChart chart, XYChart.Series<Number, Number> series, PlotType plotType) {

        series.setName(plotType.seriesName());
        chart.getData().add(series);

        // Finds which series number we've just added.  Here we make the critical assumption that
        // any series added will be at the end of the list.
        int seriesNumber = chart.getData().size();

        // Set the line color and symbol color for this series
        Node dataNode = chart.lookup(".series" + (seriesNumber - 1));
        dataNode.setStyle("-fx-stroke: " + plotType.lineColor() + "; -fx-background-color:transparent," + plotType.symbolColor());


        // We have to update all the legend labels for this chart at the same time.
        Set<Node> items = chart.lookupAll("Label.chart-legend-item");

        for (Node item : items) {
            Label label = (Label) item;
            String lineColor = PlotType.lookup(label.getText()).lineColor();
            
            // If there is no plot line, its color will be transparent, so we skip over changing
            // its label.  That allows the default legend behavior of showing the plot symbol
            // to take over.
            if (lineColor == "transparent") {
                continue;
            }

            final Rectangle rectangle = new Rectangle(10, 2, Color.web(lineColor));
            label.setGraphic(rectangle);
        }
    }

    public void addSolutionCurve(Observation obs, SqSolution solution) {
        addSeriesToChart(chart, getTheoreticalLightCurveSeries(obs, solution), STYLE_Solution);
        //chart.getData().add(getTheoreticalLightCurveSeries(obs, solution));
    }

    private XYChart.Series<Number, Number> getTheoreticalLightCurveSeries(Observation sampleData, SqSolution solution) {
        XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
        series.setName("Sample");
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

    @FXML
    void displaySolutionListHelp() {
        jocularMain.showHelpDialog("Help/solutionlist.help.html");
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

    @FXML
    public void eraseAllMarkers() {
        smartChart.getMarker("trimLeft").setInUse(false);
        smartChart.getMarker("dLeft").setInUse(false);
        smartChart.getMarker("dRight").setInUse(false);
        smartChart.getMarker("rLeft").setInUse(false);
        smartChart.getMarker("rRight").setInUse(false);
        smartChart.getMarker("trimRight").setInUse(false);
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

//            switch (markerSelectedName) {
//                case "trimLeft":
//                    markerRBtrimRight.setSelected(true);
//                    markerRBtrimRight.requestFocus();
//                    markerSelectedName = "trimRight";
//                    break;
//                case "trimRight":
//                    markerRBnone.setSelected(true);
//                    markerRBnone.requestFocus();
//                    markerSelectedName = "none";
//                    break;
//
//                case "dLeft":
//                    markerRBdRight.setSelected(true);
//                    markerRBdRight.requestFocus();
//                    markerSelectedName = "dRight";
//                    break;
//                case "dRight":
//                    markerRBrLeft.setSelected(true);
//                    markerRBrLeft.requestFocus();
//                    markerSelectedName = "rLeft";
//                    break;
//                case "rLeft":
//                    markerRBrRight.setSelected(true);
//                    markerRBrRight.requestFocus();
//                    markerSelectedName = "rRight";
//                    break;
//                case "rRight":
//                    markerRBnone.setSelected(true);
//                    markerRBnone.requestFocus();
//                    markerSelectedName = "none";
//                    break;
//            }
            markerRBnone.setSelected(true);
            markerRBnone.requestFocus();
            markerSelectedName = "none";
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
