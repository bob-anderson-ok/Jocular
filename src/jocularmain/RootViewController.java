package jocularmain;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;
import utils.JocularUtils;
import utils.Observation;
import utils.SqSolution;

public class RootViewController implements Initializable {

    private static final int EMPTY_FIELD = -1;
    private static final int FIELD_ENTRY_ERROR = -2;
    private static final int SOLUTION_LIST_HEADER_SIZE = 2;
    private static double RELATIVE_LIKEHOOD_NEEDED_TO_BE_DISPLAYED = 0.01;

    private static ManagedChart smartChart;
    private static JocularMain jocularMain;

    private static HashMap<DataType, XYChart.Series<Number, Number>> chartSeries = new HashMap<>();

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
    CheckBox obsLightFontCheckbox;

    @FXML
    CheckBox obsPointsOnlyCheckbox;

    @FXML
    public void snapshotTheChart() {

        WritableImage wim = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
        chart.snapshot(null, wim);
        saveSnapshotToFile(wim);

    }

    @FXML
    public void snapshotTheWholeWindow() {
        WritableImage wim = jocularMain.mainScene.snapshot(null);
        saveSnapshotToFile(wim);
    }

    private void saveSnapshotToFile(WritableImage wim) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Observation Plot");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("png", "*.png"));

        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try {
                boolean okOrNot = ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", file);
                if (okOrNot) {
                    jocularMain.showInformationDialog("Wrote file: " + file);
                } else {
                    jocularMain.showErrorDialog("Failed to write: " + file);
                }
            } catch (IOException e) {
                jocularMain.showErrorDialog(e.getMessage());
            }
        }
    }

    @FXML
    public void getSelectedSolution(MouseEvent arg) {
        int indexClickedOn = solutionList.getSelectionModel().getSelectedIndex();
        if (indexClickedOn >= SOLUTION_LIST_HEADER_SIZE) {
            chartSeries.put(DataType.SUBFRAME_BAND, null);
            addSolutionCurveToMainPlot(solutions.get(indexClickedOn - SOLUTION_LIST_HEADER_SIZE));
        }
    }

    @FXML
    public void replotObservation() {
        // This forces a 'relook' at the state of the checkboxes that give the user options
        // on the look of the observation data display (points, points and lines, light or dark)
        // and gets called whenver one of those checkboxes is clicked.
        repaintChart();
    }

    @FXML
    CheckBox useBaselineNoiseAsEventNoiseCheckbox;

    @FXML
    public void setEventAndBaselineNoiseEqual() {
        if (useBaselineNoiseAsEventNoiseCheckbox.isSelected()) {
            sigmaAtext.setText(sigmaBtext.getText());
        }
    }

    private String getUserPreferredObsStyle() {
        if (obsLightFontCheckbox.isSelected() && obsPointsOnlyCheckbox.isSelected()) {
            return "obsPoints";
        }

        if (obsLightFontCheckbox.isSelected() && !obsPointsOnlyCheckbox.isSelected()) {
            return "obsData";
        }

        if (!obsLightFontCheckbox.isSelected() && obsPointsOnlyCheckbox.isSelected()) {
            return "ObsPoints";
        } else {
            return "ObsData";
        }
    }

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

        System.out.println("In subframe band code");

        if (jocularMain.getCurrentSolution() == null) {
            jocularMain.showErrorDialog("There is no solution to process.");
            return;
        }

        double solutionB = jocularMain.getCurrentSolution().B;
        double solutionA = jocularMain.getCurrentSolution().A;

        double sigB = validateSigmaBtext();
        if (sigB == FIELD_ENTRY_ERROR || sigB == EMPTY_FIELD) {
            jocularMain.showErrorDialog("Baseline Noise text field is empty or erroneous.");
            return;
        }

        double sigA = validateSigmaAtext();
        if (sigA == FIELD_ENTRY_ERROR || sigA == EMPTY_FIELD) {
            jocularMain.showErrorDialog("Event Noise text field is empty or erroneous.");
            return;
        }

        int n = jocularMain.getCurrentObservation().readingNumbers.length;

        double bSFL = JocularUtils.calcBsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);
        double eSFL = JocularUtils.calcAsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);

        if (bSFL <= eSFL) {
            jocularMain.showInformationDialog("Subframe timing is not applicable for this soultion.");
            return;
        }

        int x0 = jocularMain.getCurrentObservation().readingNumbers[0];
        int lastReadingIndex = jocularMain.getCurrentObservation().readingNumbers.length - 1;
        int xn = jocularMain.getCurrentObservation().readingNumbers[lastReadingIndex];

        // Now we create the series to display as a rectangle.
        XYChart.Series<Number, Number> series;
        series = new XYChart.Series<Number, Number>();
        XYChart.Data<Number, Number> data;

        data = new XYChart.Data(x0, eSFL);
        series.getData().add(data);
        data = new XYChart.Data(xn, eSFL);
        series.getData().add(data);
        data = new XYChart.Data(xn, bSFL);
        series.getData().add(data);
        data = new XYChart.Data(x0, bSFL);
        series.getData().add(data);
        data = new XYChart.Data(x0, eSFL);
        series.getData().add(data);

        chartSeries.put(DataType.SUBFRAME_BAND, series);
        repaintChart();
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

    private void useLowSnrNoiseEstimation() {
        // In the case of low SNR observations, it is difficult to place markers around the
        // transitions.  Here use a trick: we differentiate the obsData, get the sigma for
        // the differentiated obsData, then adjust for the increase in noise (sqrt(2)) due
        // to the differentiation procedure.

        if (jocularMain.getCurrentObservation().readingNumbers.length < 3) {
            jocularMain.showErrorDialog("Cannot estimate noise: too few points in observation.");
        }

        double[] diffObs = new double[jocularMain.getCurrentObservation().readingNumbers.length - 1];

        for (int i = 0; i < diffObs.length; i++) {
            diffObs[i]
                = jocularMain.getCurrentObservation().obsData[i + 1]
                - jocularMain.getCurrentObservation().obsData[i];
        }

        double sigma = JocularUtils.calcSigma(diffObs) / Math.sqrt(2.0);
        sigmaAtext.setText(String.format("%.4f", sigma));
        sigmaBtext.setText(sigmaAtext.getText());

        return;
    }

    @FXML
    public void estimateNoiseValues() {

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation from which to estimate noise values.");
            return;
        }

        if (!(dLeftMarker.isInUse() || dRightMarker.isInUse() || rLeftMarker.isInUse() || rRightMarker.isInUse())) {
            useLowSnrNoiseEstimation();
            return;
        }

        ArrayList<Double> baselinePoints = new ArrayList<>();
        ArrayList<Double> eventPoints = new ArrayList<>();

        for (int i = 0; i < jocularMain.getCurrentObservation().readingNumbers.length; i++) {
            if (dLeftMarker.isInUse()
                && jocularMain.getCurrentObservation().readingNumbers[i] < dLeftMarker.getXValue()) {

                baselinePoints.add(jocularMain.getCurrentObservation().obsData[i]);
                continue;
            }

            if (rRightMarker.isInUse()
                && jocularMain.getCurrentObservation().readingNumbers[i] > rRightMarker.getXValue()) {

                baselinePoints.add(jocularMain.getCurrentObservation().obsData[i]);
                continue;
            }

            if (dRightMarker.isInUse() && rLeftMarker.isInUse()
                && jocularMain.getCurrentObservation().readingNumbers[i] > dRightMarker.getXValue()
                && jocularMain.getCurrentObservation().readingNumbers[i] < rLeftMarker.getXValue()) {

                eventPoints.add(jocularMain.getCurrentObservation().obsData[i]);
                continue;
            }

            if (dRightMarker.isInUse() && !rLeftMarker.isInUse()
                && jocularMain.getCurrentObservation().readingNumbers[i] > dRightMarker.getXValue()) {

                eventPoints.add(jocularMain.getCurrentObservation().obsData[i]);
                continue;
            }

            if (rLeftMarker.isInUse() && !dRightMarker.isInUse()
                && jocularMain.getCurrentObservation().readingNumbers[i] < rLeftMarker.getXValue()) {

                eventPoints.add(jocularMain.getCurrentObservation().obsData[i]);
            }
        }

        if (baselinePoints.size() < 2) {
            jocularMain.showErrorDialog("Cannot calculate baseline noise because less than 2 points available");
            sigmaBtext.setText("NaN");
            return;
        }

        double sigma = JocularUtils.calcSigma(baselinePoints);
        sigmaBtext.setText(String.format("%.4f", sigma));

        if (useBaselineNoiseAsEventNoiseCheckbox.isSelected() || eventPoints.size() < 2) {
            sigmaAtext.setText(sigmaBtext.getText());
            return;
        }

        sigma = JocularUtils.calcSigma(eventPoints);
        sigmaAtext.setText(String.format("%.4f", sigma));

        if (eventPoints.size() < 10) {
            jocularMain.showInformationDialog("There are only " + eventPoints.size() + "points in the 'event'."
                + "  This will give an unreliable estimate of the event noise.  Consider checking the box "
                + "that allows the baseline noise estimate to be used as the event noise estimate.");
        }

        if (baselinePoints.size() < 10) {
            jocularMain.showInformationDialog("There are only " + baselinePoints.size() + "points in the 'baseline'."
                + "  This observation cannot be reliably processed because the baseline noise value is too uncertain. "
                + "Suggestion: trim so that only event points remain; estimate noise with no markers; remember this value; "
                + "untrim the data, manually enter noise values, set proper proper markers, and run solution.");
        }
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

    List<SqSolution> solutions;

    @FXML
    public void computeCandidates() {

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation data to process.");
            return;
        }

        // Provide an empty Solution List
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        solutionList.setItems(items);

        chartSeries.put(DataType.SOLUTION, null);
        chartSeries.put(DataType.SUBFRAME_BAND, null);
        repaintChart();

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

        double minMagDrop = validateMinMagDrop();
        double maxMagDrop = validateMaxMagDrop();

        if (Double.isNaN(minMagDrop) || Double.isNaN(maxMagDrop)) {
            return;
        }

        if (minMagDrop >= maxMagDrop) {
            jocularMain.showErrorDialog("Invalid settings of Min and Max Mag Drop: Min Mag Drop must be less than Max Mag Drop");
            return;
        }

        SolutionStats solutionStats = new SolutionStats();

        solutions = SqSolver.computeCandidates(
            jocularMain, solutionStats,
            sigmaB, sigmaA,
            minMagDrop, maxMagDrop,
            minEventSize, maxEventSize,
            dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);

        items = FXCollections.observableArrayList();
        if (solutions.isEmpty()) {
            items.add("No significant feature meeting the supplied limits on magDrop and event size was found.");
            solutionList.setItems(items);
            return;
        }

        // !!!!! These are extra header lines in the solution list.
        // !!!!! The number MUST always equal SOLUTION_LIST_HEADER SIZE
        // !!!!! in order for clicking on a solution list entry to display the correct solution
        // Adding one header line
        items = FXCollections.observableArrayList();
        items.add(String.format("Number of transition pairs considered: %,d   Number of valid transition pairs: %,d",
                                solutionStats.numTransitionPairsConsidered,
                                solutionStats.numValidTransitionPairs));
        double probabilitySqWaveOverLine = Math.exp((solutionStats.straightLineAICc - solutions.get(0).aicc) / 2.0);

        // Adding another header line.
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

        // Do not add another header line without updating SOLUTION_LIST_HEADER_SIZE
        // Build a string version for the clients viewing pleasure.
        for (SqSolution solution : solutions) {
            if (solution.relLikelihood < RELATIVE_LIKEHOOD_NEEDED_TO_BE_DISPLAYED) {
                break;
            }
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

    @FXML
    TextField minMagDropText;

    @FXML
    TextField maxMagDropText;

    private double validateMinMagDrop() {
        double ans;
        try {
            if (minMagDropText.getText().isEmpty()) {
                minMagDropText.setText("0.01");
                return 0.01;
            } else {
                ans = Double.parseDouble(minMagDropText.getText());
            }

            if (ans < 0.01 || ans > 1000.0) {
                minMagDropText.setText("0.01");
                return 0.01;
            } else {
                return ans;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog("Min Mag Drop number format error: " + e.getMessage());
            return Double.NaN;
        }
    }

    private double validateMaxMagDrop() {
        double ans;
        try {
            if (maxMagDropText.getText().isEmpty()) {
                maxMagDropText.setText("1000.0");
                return 1000.0;
            } else {
                ans = Double.parseDouble(maxMagDropText.getText());
            }

            if (ans < 0.01 || ans > 1000.0) {
                minEventText.setText("1000.0");
                return 1000.0;
            } else {
                return ans;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog("Max Mag Drop number format error: " + e.getMessage());
            return Double.NaN;
        }
    }

    @FXML
    public void clearSolutionList() {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        solutionList.setItems(items);
        //sigmaBtext.setText("");
        //sigmaAtext.setText("");
        chartSeries.put(DataType.SOLUTION, null);
        chartSeries.put(DataType.SUBFRAME_BAND, null);
        repaintChart();
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

        chartSeries.put(DataType.OBSDATA, getObservationSeries(jocularMain.getCurrentObservation()));

        // Since we've (probably) changed the data set, erase previous solution and subframe band.
        chartSeries.put(DataType.SOLUTION, null);
        chartSeries.put(DataType.SUBFRAME_BAND, null);
        repaintChart();
        clearSolutionList();

        rightTrimMarker.setInUse(false);
        leftTrimMarker.setInUse(false);
    }

    private XYChart.Series<Number, Number> getObservationSeries(Observation observation) {
        XYChart.Series<Number, Number> series;
        series = new XYChart.Series<Number, Number>();
        XYChart.Data<Number, Number> data;

        int numDataPoints = observation.obsData.length;

        // Build the data series, point by point, adding a 'hover node' to each point
        // so that client can get data coordinate values by putting his cursor on a data point.
        for (int i = 0; i < numDataPoints; i++) {
            data = new XYChart.Data(observation.readingNumbers[i], observation.obsData[i]);
            HoveredNode hNode = new HoveredNode(observation.readingNumbers[i], observation.obsData[i]);
            data.setNode(hNode);
            series.getData().add(data);
        }
        chartSeries.put(DataType.OBSDATA, series);
        return series;
    }

    public void clearMainPlot() {
        chartSeries.clear();
        repaintChart();
    }

    public void showObservationDataWithTheoreticalLightCurve(Observation observation, SqSolution solution) {
        clearMainPlot();
        sigmaAtext.setText("");
        sigmaBtext.setText("");
        chartSeries.put(DataType.OBSDATA, getObservationSeries(observation));
        chartSeries.put(DataType.SAMPLE, getTheoreticalLightCurveSeries(observation, solution));
        repaintChart();
    }

    public void showObservationDataAlone(Observation observation) {
        clearMainPlot();
        sigmaAtext.setText("");
        sigmaBtext.setText("");
        chartSeries.put(DataType.OBSDATA, getObservationSeries(observation));
        repaintChart();
    }

    private void resetLegends() {
        // We have to update all the legend labels for this chart at the same time.
        Set<Node> items = chart.lookupAll("Label.chart-legend-item");

        for (Node item : items) {
            Label label = (Label) item;
            String lineColor = PlotType.lookup(label.getText()).lineColor();
            String symbolColor = PlotType.lookup(label.getText()).symbolColor();

            // If there is no plot line, its color will be transparent, so we skip over changing
            // its label.  That allows the default legend behavior of showing the plot symbol
            // to take over.
            if (lineColor.equals("transparent")) {
                final Circle circle = new Circle();
                circle.setRadius(2);
                circle.setFill(Color.web(symbolColor));
                label.setGraphic(circle);
            } else {
                final Rectangle rectangle = new Rectangle(10, 2, Color.web(lineColor));
                label.setGraphic(rectangle);
            }
        }
    }

    public void repaintChart() {
        chart.getData().clear();

        XYChart.Series<Number, Number> series;
        series = chartSeries.get(DataType.OBSDATA);
        if (series != null) {
            //series.setName("ObsData");
            series.setName(getUserPreferredObsStyle());
            System.out.println("We will style OBSDATA");
            chart.getData().add(chartSeries.get(DataType.OBSDATA));
            Set<Node> dataNodes = chart.lookupAll(".series" + (chart.getData().size() - 1));
            for (Node dataNode : dataNodes) {

                dataNode.setStyle("-fx-stroke: " + PlotType.lookup(series.getName()).lineColor()
                    + "; -fx-background-color:transparent," + PlotType.lookup(series.getName()).symbolColor());
            }
        }

        series = chartSeries.get(DataType.SAMPLE);
        if (series != null) {
            series.setName("Sample");
            System.out.println("We will style SAMPLE light curve");
            chart.getData().add(chartSeries.get(DataType.SAMPLE));
            Set<Node> dataNodes = chart.lookupAll(".series" + (chart.getData().size() - 1));
            for (Node dataNode : dataNodes) {

                dataNode.setStyle("-fx-stroke: " + PlotType.lookup(series.getName()).lineColor()
                    + "; -fx-background-color:transparent," + PlotType.lookup(series.getName()).symbolColor());
            }
        }

        series = chartSeries.get(DataType.SOLUTION);
        if (series != null) {
            series.setName("Solution");
            System.out.println("We will style SOLUTION light curve");
            chart.getData().add(chartSeries.get(DataType.SOLUTION));
            Set<Node> dataNodes = chart.lookupAll(".series" + (chart.getData().size() - 1));
            for (Node dataNode : dataNodes) {

                dataNode.setStyle("-fx-stroke: " + PlotType.lookup(series.getName()).lineColor()
                    + "; -fx-background-color:transparent," + PlotType.lookup(series.getName()).symbolColor());
            }
        }

        series = chartSeries.get(DataType.SUBFRAME_BAND);
        if (series != null) {
            series.setName("SubframeBand");
            System.out.println("We will style SUBFRAME_BAND light curve");
            chart.getData().add(chartSeries.get(DataType.SUBFRAME_BAND));
            Set<Node> dataNodes = chart.lookupAll(".series" + (chart.getData().size() - 1));
            for (Node dataNode : dataNodes) {

                dataNode.setStyle("-fx-stroke: " + PlotType.lookup(series.getName()).lineColor()
                    + "; -fx-background-color:transparent," + PlotType.lookup(series.getName()).symbolColor());
            }
        }

        resetLegends();
    }

    public void addSolutionCurveToMainPlot(SqSolution solution) {
        chartSeries.put(DataType.SOLUTION, getTheoreticalLightCurveSeries(jocularMain.getCurrentObservation(), solution));
        repaintChart();
    }

    public void addSampleCurveToMainPlot(SqSolution solution) {
        chartSeries.put(DataType.SAMPLE, getTheoreticalLightCurveSeries(jocularMain.getCurrentObservation(), solution));
        repaintChart();
    }

    private XYChart.Series<Number, Number> getTheoreticalLightCurveSeries(Observation sampleData, SqSolution solution) {
        XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
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

    @FXML
    void displayMinMaxMagDropHelp() {
        jocularMain.showHelpDialog("Help/magdrop.help.html");
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
                    markerRBnone.setSelected(true);
                    markerRBnone.requestFocus();
                    markerSelectedName = "none";
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
