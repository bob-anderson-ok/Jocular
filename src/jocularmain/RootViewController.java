package jocularmain;

import static java.lang.Math.sqrt;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import jocularmain.JocularMain.SolverService;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;
import utils.ErrBarUtils;
import utils.ErrorBarItem;
import utils.HistStatItem;
import utils.JocularUtils;
import static utils.JocularUtils.calcMagDrop;
import utils.MonteCarloResult;
import utils.Observation;
import utils.SqSolution;

public class RootViewController implements Initializable {

    private static final int EMPTY_FIELD = -1;
    private static final int FIELD_ENTRY_ERROR = -2;
    private static final int SOLUTION_LIST_HEADER_SIZE = 2;
    private static double RELATIVE_LIKEHOOD_NEEDED_TO_BE_DISPLAYED = 0.01;
    private static final int NUM_TRIALS_FOR_ERR_BAR_DETERMINATION = 100000;

    private static ManagedChart smartChart;
    private static JocularMain jocularMain;
    private String markerSelectedName = "none";

    private XYChartMarker dLeftMarker;
    private XYChartMarker dRightMarker;
    private XYChartMarker rLeftMarker;
    private XYChartMarker rRightMarker;

    List<SqSolution> solutions;

    private static HashMap<DataType, XYChart.Series<Number, Number>> chartSeries = new HashMap<>();

    //<editor-fold defaultstate="collapsed" desc="FXML GUI fx:ids">
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
    ListView reportListView;

    @FXML
    CheckBox obsLightFontCheckbox;
    @FXML
    CheckBox obsPointsOnlyCheckbox;
    @FXML
    CheckBox solutionEnvelopeCheckbox;

    // Confidence Interval selection buttons
    @FXML
    RadioButton conInt68RadioButton;
    @FXML
    RadioButton conInt90RadioButton;
    @FXML
    RadioButton conInt95RadioButton;
    @FXML
    RadioButton conInt99RadioButton;

    @FXML
    CheckBox allowManualNoiseEntryCheckbox;

    @FXML
    TextField sigmaBtext;
    @FXML
    TextField sigmaAtext;

    @FXML
    TextField minEventText;
    @FXML
    TextField maxEventText;

    @FXML
    TextField minMagDropText;
    @FXML
    TextField maxMagDropText;

    @FXML
    ProgressBar generalPurposeProgressBar;
    @FXML
    Label progressLabel;

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="FXML referenced methods">
    //
    //<editor-fold defaultstate="collapsed" desc="Action Buttons">
    @FXML
    public void computeCandidates() {

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation data to process.", jocularMain.primaryStage);
            return;
        }

        if (jocularMain.solverServiceRunning()) {
            jocularMain.showInformationDialog("There is a solution already in progress.", jocularMain.primaryStage);
            return;
        }

        // Provide an empty Solution List
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        solutionList.setItems(items);

        eraseSolutionAndRelatedSeries();
        repaintChart();

        double sigmaB = validateSigmaBtext();
        if (sigmaB == FIELD_ENTRY_ERROR || sigmaB == EMPTY_FIELD) {
            jocularMain.showErrorDialog("Baseline noise estimate missing or invalid.", jocularMain.primaryStage);
            solutionList.setItems(items);
            return;
        }

        double sigmaA = validateSigmaAtext();
        if (sigmaA == FIELD_ENTRY_ERROR) {
            jocularMain.showErrorDialog("Event noise entry is invalid.", jocularMain.primaryStage);
            solutionList.setItems(items);
            return;
        } else if (sigmaA == EMPTY_FIELD) {
            sigmaA = sigmaB;
            sigmaAtext.setText(sigmaBtext.getText());
        }

        int minEventSize = validateMinEventText();
        if (minEventSize == FIELD_ENTRY_ERROR) {
            jocularMain.showErrorDialog("Invalid minEventSize entry.", jocularMain.primaryStage);
            solutionList.setItems(items);
            return;
        }

        int maxEventSize = validateMaxEventText();
        if (minEventSize == FIELD_ENTRY_ERROR) {
            jocularMain.showErrorDialog("Invalid maxEventSize entry.", jocularMain.primaryStage);
            solutionList.setItems(items);
            return;
        }

        if (minEventSize > maxEventSize && maxEventSize != EMPTY_FIELD) {
            jocularMain.showErrorDialog("Invalid: minEventSize is > maxEventSize", jocularMain.primaryStage);
            solutionList.setItems(items);
            return;
        }

        double minMagDrop = validateMinMagDrop();
        double maxMagDrop = validateMaxMagDrop();

        if (Double.isNaN(minMagDrop) || Double.isNaN(maxMagDrop)) {
            return;
        }

        if (minMagDrop >= maxMagDrop) {
            jocularMain.showErrorDialog("Invalid settings of Min and Max Mag Drop: Min Mag Drop must be less than Max Mag Drop",
                                        jocularMain.primaryStage);
            return;
        }

        SolutionStats solutionStats = new SolutionStats();

        progressLabel.setText(("'solution' in progress..."));
        clearSolutionList();

        SqSolver.computeCandidates(
            generalPurposeProgressBar.progressProperty(),
            this::handleSolverDone, this::handleSolverCancelled, this::handleSolverFailed,
            jocularMain, solutionStats,
            sigmaB, sigmaA,
            minMagDrop, maxMagDrop,
            minEventSize, maxEventSize,
            dLeftMarker, dRightMarker, rLeftMarker, rRightMarker);

        return;
    }

    @FXML
    public void estimateNoiseValues() {

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation from which to estimate noise values.", jocularMain.primaryStage);
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

        double sigma;
        if (baselinePoints.size() < 2) {
            jocularMain.showErrorDialog("Cannot calculate baseline noise because less than 2 points available", jocularMain.primaryStage);
            sigmaBtext.setText("NaN");
        } else {
            sigma = JocularUtils.calcSigma(baselinePoints);
            sigmaBtext.setText(String.format("%.4f", sigma));
        }

        if (eventPoints.size() < 2) {
            jocularMain.showErrorDialog("Cannot calculate event noise because less than 2 points available", jocularMain.primaryStage);
            sigmaAtext.setText("NaN");
        } else {
            sigma = JocularUtils.calcSigma(eventPoints);
            sigmaAtext.setText(String.format("%.4f", sigma));
        }

        if (eventPoints.size()
            < 10) {
            jocularMain.showInformationDialog(
                "There are only " + eventPoints.size() + " points in the 'event'."
                + "  This will give an unreliable estimate of the event noise.",
                jocularMain.primaryStage
            );
        }

        if (baselinePoints.size()
            < 10) {
            jocularMain.showInformationDialog(
                "There are only " + baselinePoints.size() + " points in the 'baseline'."
                + "  This observation cannot be reliably processed because the baseline noise value is too uncertain.",
                jocularMain.primaryStage
            );
        }
    }

    @FXML
    public void applyTrims() {

        if (jocularMain.getCurrentObservation() == null) {
            jocularMain.showErrorDialog("There is no observation to apply trims to.", jocularMain.primaryStage);
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

        // Since we've (probably) changed the data set, erase previous solution
        // and everything related to it.
        eraseSolutionAndRelatedSeries();
        repaintChart();
        clearSolutionList();

        rightTrimMarker.setInUse(false);
        leftTrimMarker.setInUse(false);
    }

    @FXML
    public void cancelSolution() {
        jocularMain.cancelSolverService();
        jocularMain.cancelErrBarService();
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

    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Checkbox Actions">
    //
    @FXML
    public void manualNoiseClicked() {
        sigmaAtext.setEditable(allowManualNoiseEntryCheckbox.isSelected());
        sigmaBtext.setEditable(allowManualNoiseEntryCheckbox.isSelected());

    }

    @FXML
    public void solEnvelopeClicked() {
        if (solutionEnvelopeCheckbox.isSelected()) {
            showSolutionEnvelope();
        } else {
            chartSeries.remove(DataType.UPPER_ENVELOPE);
            chartSeries.remove(DataType.LOWER_ENVELOPE);
        }
        repaintChart();
    }

    @FXML
    public void confidenceIntervalButtonClicked() {
        prepareAndShowReport();
    }

    @FXML
    public void replotObservation() {
        // This forces a 'relook' at the state of the checkboxes that give the user options
        // on the look of the observation data display (points, points and lines, light or dark)
        // and gets called whenver one of those checkboxes is clicked.
        repaintChart();
    }

    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Menu Item Actions">
    @FXML
    public void doEstimateErrorBars() {
        jocularMain.showErrorBarTool();
    }

    @FXML
    public void displayReportHelp() {
        jocularMain.showHelpDialog("Help/report.help.html");
    }

    @FXML
    public void snapshotTheChart() {
        WritableImage wim = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
        chart.snapshot(null, wim);
        jocularMain.saveSnapshotToFile(wim, jocularMain.primaryStage);
    }

    @FXML
    public void snapshotTheWholeWindow() {
        WritableImage wim = jocularMain.mainScene.snapshot(null);
        jocularMain.saveSnapshotToFile(wim, jocularMain.primaryStage);
    }

    @FXML
    void showSampleDataDialog() {
        jocularMain.showSampleDataDialog();
    }

    @FXML
    public void doOpenRecentFiles() {
        jocularMain.showInformationDialog("Open Recent Files:  not yet implemented.", jocularMain.primaryStage);
    }

    @FXML
    public void doReadLimovieFile() {
        if (jocularMain.solverServiceRunning()) {
            jocularMain.showInformationDialog("This operation is blocked: solution process on current"
                + " observation is in progress.", jocularMain.primaryStage);
            return;
        }
        jocularMain.showInformationDialog("Read Limovie File:  not yet implemented.", jocularMain.primaryStage);
    }

    @FXML
    public void doReadTangraFile() {
        if (jocularMain.solverServiceRunning()) {
            jocularMain.showInformationDialog("This operation is blocked: solution process on current"
                + " observation is in progress.", jocularMain.primaryStage);
            return;
        }
        jocularMain.showInformationDialog("Read Tangra File:  not yet implemented.", jocularMain.primaryStage);
    }

    @FXML
    public void doShowSubframeTimingBand() {
        if (jocularMain.getCurrentSolution() == null) {
            jocularMain.showErrorDialog("There is no solution to process.", jocularMain.primaryStage);
            return;
        }

        double solutionB = jocularMain.getCurrentSolution().B;
        double solutionA = jocularMain.getCurrentSolution().A;

        double sigB = validateSigmaBtext();
        if (sigB == FIELD_ENTRY_ERROR || sigB == EMPTY_FIELD) {
            jocularMain.showErrorDialog("Baseline Noise text field is empty or erroneous.", jocularMain.primaryStage);
            return;
        }

        double sigA = validateSigmaAtext();
        if (sigA == FIELD_ENTRY_ERROR || sigA == EMPTY_FIELD) {
            jocularMain.showErrorDialog("Event Noise text field is empty or erroneous.", jocularMain.primaryStage);
            return;
        }

        int n = jocularMain.getCurrentObservation().readingNumbers.length;

        double bSFL = JocularUtils.calcBsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);
        double eSFL = JocularUtils.calcAsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);

        if (bSFL <= eSFL) {
            jocularMain.showInformationDialog("Subframe timing is not applicable for this solution.", jocularMain.primaryStage);
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

    @FXML
    public void showIntroHelp() {
        jocularMain.showHelpDialog("Help/gettingstarted.help.html");
    }

    @FXML
    public void showAbout() {
        jocularMain.showHelpDialog("Help/about.help.html");
    }

    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Radiobutton Actions">
    //
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

    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Solution List clicked">
    //
    @FXML
    public void getSelectedSolution(MouseEvent arg) {
        int indexClickedOn = solutionList.getSelectionModel().getSelectedIndex();
        if (indexClickedOn >= SOLUTION_LIST_HEADER_SIZE) {
            eraseSolutionAndRelatedSeries();
            addSolutionCurveToMainPlot(solutions.get(indexClickedOn - SOLUTION_LIST_HEADER_SIZE));
            repaintChart();
            jocularMain.setCurrentSolution(solutions.get(indexClickedOn - SOLUTION_LIST_HEADER_SIZE));
            reportListView.setItems(null);
            calcErrorBars();
        }
    }

    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Chart actions">
    //
    @FXML
    void respondToRightButtonClick() {
        revertToOriginalAxisScaling();
    }

    //</editor-fold> Chart related methods
    //
    //<editor-fold defaultstate="collapsed" desc="Help Displays">
    //
    @FXML
    public void displayNoiseHelp() {
        jocularMain.showHelpDialog("Help/noisevalues.help.html");
    }

    @FXML
    public void displayMinMaxEventHelp() {
        jocularMain.showHelpDialog("Help/eventlimits.help.html");

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

    //</editor-fold> Help display methods
    //
    //</editor-fold> Methods referenced in FXML code
    //
    public static void setMainApp(JocularMain main) {
        jocularMain = main;
    }

    //<editor-fold defaultstate="collapsed" desc="Report preparation methods">
    private void prepareAndShowReport() {
        if (!reportIsPossible()) {
            return;
        }

        SqSolution curSol = jocularMain.getCurrentSolution();
        HashMap<String, ErrorBarItem> errBars = jocularMain.getCurrentErrBarValues();

        boolean subFrameTimingUsed = errBars.get("D68").subFrameTimingEstimate;

        ObservableList<String> resultItems = FXCollections.observableArrayList();

        int conInterval = getSelectedConfidenceInterval();

        double baselineSD = sigmaAtCI(conInterval, curSol.sigmaB) / sqrt(curSol.numBaselinePoints - 1);
        double eventSD = sigmaAtCI(conInterval, curSol.sigmaA) / sqrt(curSol.numEventPoints - 1);

        if (subFrameTimingUsed) {
            resultItems.add("Error bars: sub-frame timing");
        } else {
            resultItems.add("Error bars: monte carlo trials");
        }

        resultItems.add(
            String.format(
                "B = %8.2f  +/- %.2f @ %d%s",
                curSol.B,
                baselineSD,
                conInterval,
                "%"
            )
            + String.format(
                "\nA = %8.2f  +/- %.2f @ %d%s",
                curSol.A,
                eventSD,
                conInterval,
                "%"
            )
        );

        String magDropReport = prepareMagDropReport(
            curSol.B, baselineSD, curSol.A, eventSD
        );
        resultItems.add(magDropReport);

        if (!Double.isNaN(curSol.D)) {
            resultItems.add(
                String.format(
                    "D = %.2f  +%.2f/-%.2f @ %d%s",
                    curSol.D,
                    errBars.get("D" + conInterval).barPlus,
                    errBars.get("D" + conInterval).barMinus,
                    conInterval,
                    "%"
                )
            );
        }
        if (!Double.isNaN(curSol.R)) {
            resultItems.add(
                String.format(
                    "R = %.2f  +%.2f/-%.2f @ %d%s",
                    curSol.R,
                    errBars.get("R" + conInterval).barPlus,
                    errBars.get("R" + conInterval).barMinus,
                    conInterval,
                    "%"
                )
            );
        }
        if (!(Double.isNaN(curSol.D) || Double.isNaN(curSol.R))) {
            resultItems.add(
                String.format(
                    "Dur = %.2f  +%.2f/-%.2f @ %d%s",
                    curSol.R - curSol.D,
                    errBars.get("R" + conInterval).barPlus + errBars.get("D" + conInterval).barMinus,
                    errBars.get("R" + conInterval).barMinus + errBars.get("D" + conInterval).barPlus,
                    conInterval,
                    "%"
                )
            );
        }

        double signal = curSol.B - curSol.A;
        int numPoints = jocularMain.getCurrentObservation().obsData.length;
        int dur;
        if (Double.isNaN(curSol.R)) {
            dur = numPoints - (int) curSol.D;
        } else if (Double.isNaN(curSol.D)) {
            dur = (int) curSol.R;
        } else {
            dur = (int) (curSol.R - curSol.D);
        }
        dur = Math.max(dur, 1);

        double falsePos = JocularUtils.falsePositiveProbability(
            signal,
            curSol.sigmaB,
            dur,
            numPoints
        );

        resultItems.add(
            String.format(
                "false positive probability: %.4f",
                falsePos
            )
        );

        reportListView.setItems(resultItems);

        if (solutionEnvelopeCheckbox.isSelected()) {
            showSolutionEnvelope();
            repaintChart();
        }
    }

    private String prepareMagDropReport(double B, double Bsd, double A, double Asd) {
        double nominalMagDrop = calcMagDrop(B, A);
        double minMagDrop = calcMagDrop(B - Bsd, A + Asd);
        double maxMagDrop = calcMagDrop(B + Bsd, A - Asd);
        return String.format(
            "min magDrop: %5.2f\nnom magDrop: %5.2f\nmax magDrop: %5.2f",
            minMagDrop, nominalMagDrop, maxMagDrop);
    }

    private boolean reportIsPossible() {
        return (jocularMain.getCurrentSolution() != null)
            && (jocularMain.getCurrentErrBarValues() != null);
    }
    //</editor-fold> Report preparation methods

    private int getSelectedConfidenceInterval() {
        if (conInt68RadioButton.isSelected()) {
            return 68;
        }
        if (conInt90RadioButton.isSelected()) {
            return 90;
        }
        if (conInt95RadioButton.isSelected()) {
            return 95;
        }
        if (conInt99RadioButton.isSelected()) {
            return 99;
        }
        return -1;
    }

    private boolean inSubframeTimingNoiseRegime(int n, double sigB, double sigA, double solutionB, double solutionA) {
        double bSFL = JocularUtils.calcBsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);
        double eSFL = JocularUtils.calcAsideSubframeBoundary(n, sigB, sigA, solutionB, solutionA);

        if (bSFL <= eSFL) {
            return false;
        }
        return true;
    }

    //<editor-fold defaultstate="collapsed" desc="Error Bar related methods">
    public void calcErrorBars() {
        if (jocularMain.getCurrentSolution() == null) {
            jocularMain.showErrorDialog(
                "There is no solution to determine confidence intervals for.",
                jocularMain.primaryStage
            );
            return;
        }

        boolean errBarServiceRunning = jocularMain.errBarServiceRunning();
        boolean solverServiceRunning = jocularMain.solverServiceRunning();
        if (errBarServiceRunning || solverServiceRunning) {
            jocularMain.showInformationDialog(
                "There is already an error bar calculation or solution in progress",
                jocularMain.primaryStage
            );
            return;
        }

        SqSolution sqSol = jocularMain.getCurrentSolution();

        if (Double.isNaN(sqSol.sigmaA) || Double.isNaN(sqSol.sigmaB)) {
            jocularMain.showErrorDialog("Noise levels missing.", jocularMain.primaryStage);
            return;
        }

        if (inSubframeTimingNoiseRegime(jocularMain.getCurrentObservation().obsData.length,
                                        sqSol.sigmaB, sqSol.sigmaA,
                                        sqSol.B, sqSol.A)) {
            jocularMain.setCurrentErrBarValues(getErrBarDataForSubframeCase());
            prepareAndShowReport();

            return;
        }

        double snrA = (sqSol.B - sqSol.A) / sqSol.sigmaA;
        double sym = sqSol.sigmaA / sqSol.sigmaB;

        double snrEff;
        if (sym > 0.5) {
            snrEff = snrA;
        } else if (sym < 0.12) {
            snrEff = 3.0 * snrA;
        } else {
            snrEff = 2.0 * snrA;
        }

        int numPointsInTrialSample;
        if (snrEff >= 2.0) {
            numPointsInTrialSample = 100;
        } else if (snrEff >= 1.0) {
            numPointsInTrialSample = 200;
        } else if (snrEff >= 0.5) {
            numPointsInTrialSample = 600;
        } else if (snrEff >= 0.25) {
            numPointsInTrialSample = 1800;
        } else {
            numPointsInTrialSample = 2000;
        }

        // Set up the parameters for a monte carlo estimation of confidence intervals.
        TrialParams trialParams = new TrialParams();
        trialParams.baselineLevel = sqSol.B;
        trialParams.eventLevel = sqSol.A;
        trialParams.numTrials = NUM_TRIALS_FOR_ERR_BAR_DETERMINATION;
        trialParams.sampleWidth = numPointsInTrialSample;
        trialParams.mode = MonteCarloMode.RANDOM;
        trialParams.sigmaB = sqSol.sigmaB;
        trialParams.sigmaA = sqSol.sigmaA;

        progressLabel.setText("error bar calculation...");

        jocularMain.errBarServiceStart(
            false,
            trialParams,
            this::handleSuccessfulErrBarService,
            this::handleNonSuccessfulErrBarService,
            this::handleNonSuccessfulErrBarService,
            generalPurposeProgressBar.progressProperty()
        );
    }

    private double sigmaAtCI(int confidenceInterval, double sigma) {
        double sigmaMultiplier = 1.0;
        switch (confidenceInterval) {
            case 68:
                sigmaMultiplier = 1.0;
                break;
            case 90:
                sigmaMultiplier = 1.644854;
                break;
            case 95:
                sigmaMultiplier = 1.959964;
                break;
            case 99:
                sigmaMultiplier = 2.575829;
                break;
            default:
                throw new IllegalArgumentException("sigmaAtCI() invalid confidence interval: " + confidenceInterval);
        }
        return sigma * sigmaMultiplier;
    }

    private void fillErrItemFields(ErrorBarItem errItem, int confidenceInterval, double sigmaRdg) {
        errItem.targetCI = confidenceInterval;
        errItem.barPlus = sigmaAtCI(errItem.targetCI, sigmaRdg);
        errItem.barMinus = errItem.barPlus;
        errItem.actualCI = (double) errItem.targetCI;
        errItem.subFrameTimingEstimate = true;
    }

    private HashMap<String, ErrorBarItem> getErrBarDataForSubframeCase() {
        HashMap<String, ErrorBarItem> ans = new HashMap<>();

        SqSolution curSol = jocularMain.getCurrentSolution();
        double sigma;

        double frac;
        double D = curSol.D;
        double R = curSol.R;

        if (!Double.isNaN(D)) {

            frac = curSol.dTransitionIndex - D;
            sigma = curSol.sigmaB - (curSol.sigmaB - curSol.sigmaA) * frac;
            double sigmaRdg = sigma / (curSol.B - curSol.A);

            ErrorBarItem errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 68, sigmaRdg);
            ans.put("D68", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 90, sigmaRdg);
            ans.put("D90", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 95, sigmaRdg);
            ans.put("D95", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 99, sigmaRdg);
            ans.put("D99", errItem);

        }

        if (!Double.isNaN(R)) {
            frac = curSol.rTransitionIndex - R;
            sigma = curSol.sigmaA + (curSol.sigmaB - curSol.sigmaA) * frac;
            double sigmaRdg = sigma / (curSol.B - curSol.A);

            ErrorBarItem errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 68, sigmaRdg);
            ans.put("R68", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 90, sigmaRdg);
            ans.put("R90", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 95, sigmaRdg);
            ans.put("R95", errItem);

            errItem = new ErrorBarItem();
            fillErrItemFields(errItem, 99, sigmaRdg);
            ans.put("R99", errItem);
        }

        return ans;
    }

    private void handleNonSuccessfulErrBarService(WorkerStateEvent event) {
        resetProgressIndicator();
    }

    private void handleSuccessfulErrBarService(WorkerStateEvent event) {

        if (!jocularMain.errBarServiceFinished()) {
            return;
        }

        resetProgressIndicator();

        MonteCarloResult monteCarloResult;

        monteCarloResult = jocularMain.getErrBarServiceCumResults();

        // The following message should never be triggered. The numPointsInTrialSample was experimentally
        // determined to result in < 2% reject rate.
        if (monteCarloResult.numRejections > NUM_TRIALS_FOR_ERR_BAR_DETERMINATION / 10) {
            double rejectRate = (monteCarloResult.numRejections / NUM_TRIALS_FOR_ERR_BAR_DETERMINATION) * 100.0;
            jocularMain.showErrorDialog(
                String.format(
                    "This event has a SNR below 0.25 and experienced a %.0f reject rate during "
                    + "error bar estimation.  The error bar estimates are therefore suspect. "
                    + "Use the Error Bar Study Panel to hand-craft an error bar estimation for SNR below 0.25.",
                    rejectRate
                ),
                jocularMain.errorBarPanelStage
            );
        }

        ArrayList<HistStatItem> statsArray = ErrBarUtils.getInstance().buildHistStatArray(monteCarloResult.histogram);

        boolean centered = true;  // Because the trial was run with MonteCarloMode.Random, the solutions are 'centered' around mid-frame
        HashMap<String, ErrorBarItem> errBarData = ErrBarUtils.getInstance().getErrorBars(statsArray, centered);

        jocularMain.setCurrentErrBarValues(errBarData);

        prepareAndShowReport();
    }

    private String toStringErrBarItem(ErrorBarItem item) {
        return String.format("%d.0%s  %4.1f%s  %.1f/%.1f",
                             item.targetCI, "%",
                             item.actualCI, "%",
                             item.barPlus,
                             item.barMinus
        );
    }
    //</editor-fold> Error Bar related methods

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
        // transitions.  Here we employ a useful procedure: we differentiate the obsData, get the sigma for
        // the differentiated obsData, then adjust for the increase in noise (sqrt(2)) due
        // to the differentiation procedure.

        if (jocularMain.getCurrentObservation().readingNumbers.length < 3) {
            jocularMain.showErrorDialog("Cannot estimate noise: too few points in observation.", jocularMain.primaryStage);
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

        //jocularMain.getCurrentSolution().sigmaB = sigma;
        //jocularMain.getCurrentSolution().sigmaA = sigma;
        return;
    }

    class LogLcomparator implements Comparator<SqSolution> {

        @Override
        public int compare(SqSolution one, SqSolution two) {
            // sort is smallest to largest
            return Double.compare(one.aicc, two.aicc);
        }
    }

    private void resetProgressIndicator() {
        generalPurposeProgressBar.progressProperty().unbind();
        generalPurposeProgressBar.setProgress(0.0);
        progressLabel.setText("");
    }

    //<editor-fold defaultstate="collapsed" desc="Solver Service methods">
    //
    private void handleSolverCancelled(WorkerStateEvent event) {
        resetProgressIndicator();
    }

    private void printCurrentStateOfSolverServices() {
        System.out.println("");
        for (SolverService solService : jocularMain.multiCoreSolverService) {
            System.out.println("solService state: " + solService.getState());
        }
    }

    private void handleSolverFailed(WorkerStateEvent event) {
        printCurrentStateOfSolverServices();
        resetProgressIndicator();
    }

    private void handleSolverDone(WorkerStateEvent event) {
        //printCurrentStateOfSolverServices();
        if (!jocularMain.solverServiceFinished()) {
            return;
        }

        resetProgressIndicator();
        solutions = jocularMain.getCumSolverSolutions();
        ObservableList<String> items = FXCollections.observableArrayList();
        if (solutions.isEmpty()) {
            items.add("No significant feature meeting the supplied limits on magDrop and event size was found.");
            solutionList.setItems(items);
            return;
        }

        // If we got any solutions, we've got work to do ...
        if (solutions.size() > 0) {
            // Sort them in ascending order of AICc
            LogLcomparator logLcomparator = new LogLcomparator();
            Collections.sort(solutions, logLcomparator);
            jocularMain.addSolutionCurveToMainPlot(solutions.get(0));
            jocularMain.setCurrentSolution(solutions.get(0));

            // Fill in relative likelihoods
            double minAicc = solutions.get(0).aicc;
            for (SqSolution sol : solutions) {
                sol.relLikelihood = Math.exp(minAicc - sol.aicc);
            }
        }

        SolutionStats solutionStats = jocularMain.getCumSolutionStats();

        solutionStats.numValidTransitionPairs = jocularMain.getCumSolutionStats().numValidTransitionPairs;
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
            //items.add(String.format("Straight line:  AICc=%11.2f  logL=%11.2f  relative probability of SqWave versus straight line > 1000",
            //                        solutionStats.straightLineAICc,
            //                        solutionStats.straightLineLogL));
            items.add(String.format("Relative probability of SqWave versus straight line > 1000"));
        } else {
            //items.add(String.format("Straight line:  AICc=%11.2f  logL=%11.2f  relative probability of SqWave versus straight line= %.1f",
            //                        solutionStats.straightLineAICc,
            //                        solutionStats.straightLineLogL,
            //                        probabilitySqWaveOverLine));
            items.add(String.format("Relative probability of SqWave versus straight line= %.1f",
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
        calcErrorBars();
    }

    //
    //</editor-fold> Solver service methods
    //<editor-fold defaultstate="collapsed" desc="Text field validation routines">
    //
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
                jocularMain.showErrorDialog(sourceId + " must be > 0.0", jocularMain.primaryStage);
                return FIELD_ENTRY_ERROR;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage(), jocularMain.primaryStage);
            return FIELD_ENTRY_ERROR;
        }
    }

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
                jocularMain.showErrorDialog(sourceId + " must be > 0", jocularMain.primaryStage);
                return FIELD_ENTRY_ERROR;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage(), jocularMain.primaryStage);
            return FIELD_ENTRY_ERROR;
        }
    }

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
            jocularMain.showErrorDialog("Min Mag Drop number format error: " + e.getMessage(), jocularMain.primaryStage);
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
            jocularMain.showErrorDialog("Max Mag Drop number format error: " + e.getMessage(), jocularMain.primaryStage);
            return Double.NaN;
        }
    }

    //
    //</editor-fold> Text field validation routines
    //
    public void clearSolutionList() {
        solutionList.setItems(null);
        reportListView.setItems(null);
        jocularMain.setCurrentSolution(null);
        eraseSolutionAndRelatedSeries();
        repaintChart();
    }

    //<editor-fold defaultstate="collapsed" desc="Chart (main plot) related methods">
    //
    /**
     * a node which displays a value on hover, but is otherwise empty
     */
    class HoveredNode extends StackPane {

        HoveredNode(int readingNumber, double intensity) {
            setOnMouseEntered(e -> outputLabel.setText(String.format("RdgNbr %d Intensity %.2f", readingNumber, intensity)));
            setOnMouseExited(e -> outputLabel.setText(""));
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

    private void eraseSolutionAndRelatedSeries() {
        chartSeries.put(DataType.SOLUTION, null);
        chartSeries.put(DataType.SUBFRAME_BAND, null);
        chartSeries.put(DataType.UPPER_ENVELOPE, null);
        chartSeries.put(DataType.LOWER_ENVELOPE, null);
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
        repaintChart();
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
        if (observation != null) {
            chartSeries.put(DataType.OBSDATA, getObservationSeries(observation));
        }
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

    private void showSolutionEnvelope() {
        SqSolution curSol = jocularMain.getCurrentSolution();
        if (curSol == null) {
            return;
        }

        HashMap<String, ErrorBarItem> errBars = jocularMain.getCurrentErrBarValues();
        if (errBars == null) {
            return;
        }
        int conInterval = getSelectedConfidenceInterval();

        double B = curSol.B;
        double A = curSol.A;

        double Bbar = sigmaAtCI(conInterval, curSol.sigmaB) / sqrt(curSol.numBaselinePoints - 1);
        double Abar = sigmaAtCI(conInterval, curSol.sigmaA) / sqrt(curSol.numEventPoints - 1);

        double D = curSol.D;
        double R = curSol.R;

        double DplusBar;
        double DminusBar;

        if (Double.isNaN(D)) {
            DplusBar = 0.0;
            DminusBar = 0.0;
        } else {
            DplusBar = errBars.get("D" + conInterval).barPlus;
            DminusBar = errBars.get("D" + conInterval).barMinus;
        }

        double RplusBar;
        double RminusBar;
        if (Double.isNaN(R)) {
            RplusBar = 0.0;
            RminusBar = 0.0;
        } else {
            RplusBar = errBars.get("R" + conInterval).barPlus;
            RminusBar = errBars.get("R" + conInterval).barMinus;
        }

        double begin = jocularMain.getCurrentObservation().readingNumbers[0] - 1.0;
        int lastReadingIndex = jocularMain.getCurrentObservation().readingNumbers.length - 1;
        double end = jocularMain.getCurrentObservation().readingNumbers[lastReadingIndex];

        XYChart.Series<Number, Number> series;
        XYChart.Data<Number, Number> data;

        if (Double.isNaN(R)) {
            // Process solution with only a D edge
            // Prepare points for upper envelope.
            series = new XYChart.Series<Number, Number>();
            data = new XYChart.Data(begin, B + Bbar);
            series.getData().add(data);
            if (D + DplusBar >= end) {
                data = new XYChart.Data(end, B + Bbar);
                series.getData().add(data);
            } else {
                data = new XYChart.Data(D + DplusBar, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D + DplusBar, A + Abar);
                series.getData().add(data);
                data = new XYChart.Data(end, A + Abar);
                series.getData().add(data);
            }
            chartSeries.put(DataType.UPPER_ENVELOPE, series);
            repaintChart();

            // Prepare points for lower envelope.
            series = new XYChart.Series<Number, Number>();
            if (D - DminusBar < begin) {
                data = new XYChart.Data(begin, A - Abar);
                series.getData().add(data);
                data = new XYChart.Data(end, A - Abar);
                series.getData().add(data);
            } else {
                data = new XYChart.Data(begin, B - Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D - DminusBar, B - Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D - DminusBar, A - Abar);
                series.getData().add(data);
                data = new XYChart.Data(end, A - Abar);
                series.getData().add(data);
            }
            chartSeries.put(DataType.LOWER_ENVELOPE, series);
            repaintChart();
        } else if (Double.isNaN(D)) {
            // Process solution with only an R edge
            // Prepare points for upper envelope
            series = new XYChart.Series<Number, Number>();
            if (R - RminusBar > begin) {
                data = new XYChart.Data(begin, A + Abar);
                series.getData().add(data);
                data = new XYChart.Data(R - RminusBar, A + Abar);
                series.getData().add(data);
                data = new XYChart.Data(R - RminusBar, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(end, B + Bbar);
                series.getData().add(data);
            } else {
                data = new XYChart.Data(begin, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(end, B + Bbar);
                series.getData().add(data);
            }
            chartSeries.put(DataType.UPPER_ENVELOPE, series);
            repaintChart();

            // Prepare points for lower envelope
            series = new XYChart.Series<Number, Number>();
            if (R + RplusBar < end) {
                data = new XYChart.Data(begin, A - Abar);
                series.getData().add(data);
                data = new XYChart.Data(R + RplusBar, A - Abar);
                series.getData().add(data);
                data = new XYChart.Data(R + RplusBar, B - Bbar);
                series.getData().add(data);
                data = new XYChart.Data(end, B - Bbar);
                series.getData().add(data);
            } else {
                data = new XYChart.Data(begin, A - Abar);
                series.getData().add(data);
                data = new XYChart.Data(end, A - Abar);
                series.getData().add(data);
            }
            chartSeries.put(DataType.LOWER_ENVELOPE, series);
            repaintChart();
        } else {
            // Process solution with both a D and an R
            // Prepare points for upper envelope.
            series = new XYChart.Series<Number, Number>();
            if (D + DplusBar >= R - RminusBar) {
                jocularMain.showInformationDialog("Solution has overlapping D and R edges !!", jocularMain.primaryStage);
                data = new XYChart.Data(begin, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(end, B + Bbar);
                series.getData().add(data);
            } else {
                data = new XYChart.Data(begin, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D + DplusBar, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D + DplusBar, A + Abar);
                series.getData().add(data);
                data = new XYChart.Data(R - RminusBar, A + Abar);
                series.getData().add(data);
                data = new XYChart.Data(R - RminusBar, B + Bbar);
                series.getData().add(data);
                data = new XYChart.Data(end, B + Bbar);
                series.getData().add(data);
            }
            chartSeries.put(DataType.UPPER_ENVELOPE, series);
            repaintChart();

            // Prepare points for lower envelope.
            series = new XYChart.Series<Number, Number>();
            if (D - DminusBar > begin) {
                data = new XYChart.Data(begin, B - Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D - DminusBar, B - Bbar);
                series.getData().add(data);
                data = new XYChart.Data(D - DminusBar, A - Abar);
                series.getData().add(data);
                if (R + RplusBar < end) {
                    data = new XYChart.Data(R + RplusBar, A - Abar);
                    series.getData().add(data);
                    data = new XYChart.Data(R + RplusBar, B - Bbar);
                    series.getData().add(data);
                    data = new XYChart.Data(end, B - Bbar);
                    series.getData().add(data);
                } else {
                    data = new XYChart.Data(end, A - Abar);
                    series.getData().add(data);
                }

            } else {
                data = new XYChart.Data(begin, A - Abar);
                series.getData().add(data);
                if (R + RplusBar < end) {
                    data = new XYChart.Data(R + RplusBar, A - Abar);
                    series.getData().add(data);
                    data = new XYChart.Data(R + RplusBar, B - Bbar);
                    series.getData().add(data);
                    data = new XYChart.Data(end, B - Bbar);
                    series.getData().add(data);
                } else {
                    data = new XYChart.Data(begin, A - Abar);
                    series.getData().add(data);
                }
            }
            chartSeries.put(DataType.LOWER_ENVELOPE, series);
            repaintChart();
        }
    }

    public void repaintChart() {
        chart.getData().clear();

        //System.out.println("Repainting chart...");
        XYChart.Series<Number, Number> series;

        Set<DataType> dataTypes = chartSeries.keySet();
        for (DataType dataType : dataTypes) {
            series = chartSeries.get(dataType);
            if (series == null) {
                continue;
            }
            if (dataType == DataType.OBSDATA) {
                series.setName(getUserPreferredObsStyle());
            } else {
                series.setName(dataType.getName());
            }

            chart.getData().add(series);

            String lineColor = PlotType.lookup(series.getName()).lineColor();
            String symbolColor = PlotType.lookup(series.getName()).symbolColor();

            //System.out.println("   series:" + series.getName() + "  lineColor:" + lineColor + "  symbolColor:" + symbolColor);
            Set<Node> dataNodes = chart.lookupAll(".series" + (chart.getData().size() - 1));
            for (Node dataNode : dataNodes) {
                dataNode.setStyle("-fx-stroke: " + lineColor
                    + "; -fx-background-color:transparent," + symbolColor);
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

    //
    //</editor-fold> Chart (main plot) related methods
    //<editor-fold defaultstate="collapsed" desc="Chart marker related methods">
    //
    private void makeMarkersVisible() {
        hideToggleButton.setSelected(false);
        hideUnhideMarkers();
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

    //
    //</editor-fold> Chart marker related methods
    //
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
         * In order to have the 'markers' adapt to window resizing and axes //
         * changes, they have to be redrawn continuously. So we register a //
         * new handler with AnimationTimer. That handler gets called about // 60
         * times a second.
         */
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                smartChart.repaintMarkers();
            }
        }.start();

    }
}
