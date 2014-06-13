package jocularmain;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import org.gillius.jfxutils.chart.ManagedChart;
import org.gillius.jfxutils.chart.StableTicksAxis;
import utils.ErrBarUtils;
import utils.ErrorBarItem;
import utils.HistStatItem;
import utils.MonteCarloResult;

public class ErrorBarFXMLController implements Initializable {

    private static ManagedChart smartChart;
    
    private static final int EMPTY_FIELD = -1;
    private static final int FIELD_ENTRY_ERROR = -2;

    private double baselineLevel;
    private double eventLevel;
    private double sigmaB;
    private double sigmaA;
    private int numPoints;
    private int numTrials;
    private MonteCarloMode monteCarloMode;
    private TrialParams trialParams = new TrialParams();

    private static JocularMain jocularMain;

    public static void setMainApp(JocularMain main) {
        jocularMain = main;
    }

    @FXML
    LineChart<Number, Number> mainChart;

    @FXML
    TextField baselineLevelText;
    @FXML
    TextField eventLevelText;
    @FXML
    TextField sigmaBtext;
    @FXML
    TextField sigmaAtext;
    @FXML
    TextField numPointsText;
    @FXML
    TextField numTrialsText;

    @FXML
    CheckBox overplotCheckbox;
    @FXML
    CheckBox recalcLevels;

    @FXML
    RadioButton randomRadioButton;
    @FXML
    RadioButton leftEdgeRadioButton;
    @FXML
    RadioButton midPointRadioButton;

    @FXML
    ListView mainListView;
    @FXML
    ListView resultsListView;

    @FXML
    ProgressBar trialsProgressBar;

    @FXML
    public void cancelTrials() {
        jocularMain.cancelErrBarService();
    }

    @FXML
    public void showErrorBarHelp() {
        jocularMain.showHelpDialog("Help/errorbar.help.html");
    }

    @FXML
    public void writePanelToFile() {
        WritableImage wim = jocularMain.errorBarPanelScene.snapshot(null);
        jocularMain.saveSnapshotToFile(wim, jocularMain.errorBarPanelStage);
    }

    @FXML
    public void calculateDistribution() {

        if (!validateInputTextFields()) {
            return;
        }

        // validateInputTextFields() has filled in all the 'globals' referenced
        // in the next code block.  We use them to set up the parameters for a Monte Carlo run.
        trialParams.baselineLevel = baselineLevel;
        trialParams.eventLevel = eventLevel;
        trialParams.numTrials = numTrials;
        trialParams.sampleWidth = numPoints;
        trialParams.mode = monteCarloMode;
        trialParams.sigmaB = sigmaB;
        trialParams.sigmaA = sigmaA;

        clearListViewsAndPlot();
        trialsProgressBar.setVisible(true);

        jocularMain.errBarServiceStart(
            recalcLevels.isSelected(),
            trialParams,
            this::handleErrBarServiceSucceeded,
            this::handleErrBarServiceNonSuccess,
            this::handleErrBarServiceNonSuccess,
            trialsProgressBar.progressProperty()
        );
    }

    private void clearListViewsAndPlot() {
        mainListView.setItems(null);
        resultsListView.setItems(null);
        if (!overplotCheckbox.isSelected()) {
            mainChart.getData().clear();
        }
    }

    private void handleErrBarServiceNonSuccess(WorkerStateEvent event) {
        trialsProgressBar.setVisible(false);
    }

    private void handleErrBarServiceSucceeded(WorkerStateEvent event) {

        // Since we're (possibly) muti-threaded, we get called at the completion
        // of each thread. But they might not all be done, so we need to ask
        // whether all threads have completed before grabbibg the results.
        if ( ! jocularMain.errBarServiceFinished()) {
            return;
        }

        trialsProgressBar.setVisible(false);

        MonteCarloResult monteCarloResult = jocularMain.getErrBarServiceCumResults();

        if (monteCarloResult.numRejections > numTrials / 50) {
            jocularMain.showErrorDialog("More than 2% of the trials were rejected."
                + " Possibly noise levels are too high or there are not enough points in the trial sample."
                + " Number of rejects: " + monteCarloResult.numRejections,
                                        jocularMain.errorBarPanelStage);
            clearListViewsAndPlot();
            //return;
        }

        // Display the histogram array in the mainListView.
        ObservableList<String> items;
        items = FXCollections.observableArrayList();
        items.add("  R    num @ R");
        for (int i = 0; i < monteCarloResult.histogram.length; i++) {
            items.add(String.format("%3d %,8d", i, monteCarloResult.histogram[i]));
        }
        mainListView.setItems(items);

        // Plot the probability mass distribution.
        plotData(monteCarloResult.histogram);

        ArrayList<HistStatItem> statsArray = ErrBarUtils.getInstance().buildHistStatArray(monteCarloResult.histogram);

        boolean centered = randomRadioButton.isSelected() || midPointRadioButton.isSelected();
        HashMap<String, ErrorBarItem> errBarData = ErrBarUtils.getInstance().getErrorBars(statsArray, centered);

        // Display the error bar stats in the resultsListView
        ObservableList<String> resultItems = FXCollections.observableArrayList();
        resultItems.add("CI     CI act  left   peak   right  width    +/-");
        resultItems.add(toStringErrBarItem(errBarData.get("D68")));
        resultItems.add(toStringErrBarItem(errBarData.get("D90")));
        resultItems.add(toStringErrBarItem(errBarData.get("D95")));
        resultItems.add(toStringErrBarItem(errBarData.get("D99")));
        resultItems.add(String.format("\n%,d samples were rejected on the way to %,d good trials.",
                                      monteCarloResult.numRejections, numTrials));
        resultItems.add(String.format("\n%d cores were used in this calculation", Runtime.getRuntime().availableProcessors()));
        
        int numHistEntries = 0;
        for(int i=0;i<monteCarloResult.histogram.length;i++) {
            numHistEntries += monteCarloResult.histogram[i];
        }
        resultItems.add("Number of histogram entries: " + numHistEntries);
        resultsListView.setItems(resultItems);

    }

    private String toStringErrBarItem(ErrorBarItem item) {
        return String.format("%d.0%s  %4.1f%s %5d  %5d  %5d  %5d    %.1f/%.1f",
                             item.targetCI, "%",
                             item.actualCI, "%",
                             item.leftIndex,
                             item.peakIndex,
                             item.rightIndex,
                             item.width,
                             item.barPlus,
                             item.barMinus
        );
    }

    private boolean validateInputTextFields() {
        baselineLevel = validateBaselineLevelText();
        if (baselineLevel == EMPTY_FIELD || baselineLevel == FIELD_ENTRY_ERROR) {
            return false;
        }

        eventLevel = validateEventLevelText();
        if (eventLevel == EMPTY_FIELD || eventLevel == FIELD_ENTRY_ERROR) {
            return false;
        }

        sigmaB = validateSigmaBtext();
        if (sigmaB == EMPTY_FIELD || sigmaB == FIELD_ENTRY_ERROR) {
            return false;
        }

        sigmaA = validateSigmaAtext();
        if (sigmaA == EMPTY_FIELD || sigmaA == FIELD_ENTRY_ERROR) {
            return false;
        }

        try {
            String text = numPointsText.getText();
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(" Number of points in trial cannot be empty", jocularMain.errorBarPanelStage);
                return false;
            }
            numPoints = Integer.parseInt(text);
            if (numPoints <= 0.0) {
                jocularMain.showErrorDialog(" Number of points in trial must be > 0.0", jocularMain.errorBarPanelStage);
                return false;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(" number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return false;
        }

        try {
            String text = numTrialsText.getText();
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(" Number of trials cannot be empty", jocularMain.errorBarPanelStage);
                return false;
            }
            numTrials = Integer.parseInt(text);
            if (numTrials <= 0.0) {
                jocularMain.showErrorDialog(" Number of trials must be > 0.0", jocularMain.errorBarPanelStage);
                return false;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(" number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return false;
        }

        if (randomRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.RANDOM;
        } else if (leftEdgeRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.LEFT_EDGE;
        } else if (midPointRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.MID_POINT;
        } else {
            jocularMain.showErrorDialog("Program design error: unimplemented Monte Carlo Mode radio button",
                                        jocularMain.errorBarPanelStage);
            return false;
        }

        return true;
    }

    private XYChart.Series<Number, Number> getMassDistributionSeries(int[] hist) {
        XYChart.Series<Number, Number> series;
        series = new XYChart.Series<Number, Number>();
        XYChart.Data<Number, Number> data;

        int numDataPoints = hist.length;

        // Build the data series, point by point, adding a 'hover node' to each point
        // so that client can get data coordinate values by putting his cursor on a data point.
        for (int i = 0; i < numDataPoints; i++) {
            data = new XYChart.Data(i, 0);
            series.getData().add(data);
            data = new XYChart.Data(i, hist[i]);
            series.getData().add(data);
            data = new XYChart.Data(i, 0);
            series.getData().add(data);
        }
        series.setName("probMassDist");

        return series;
    }

    private void plotData(int[] values) {
        if (!overplotCheckbox.isSelected()) {
            mainChart.getData().clear();
        }
        mainChart.getData().add(getMassDistributionSeries(values));
    }

    private int maxValue(int[] values) {
        int ans = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > ans) {
                ans = values[i];
            }
        }
        return ans;
    }

    private double validateSigmaBtext() {
        return sigmaValue(sigmaBtext.getText(), "Baseline Noise");
    }

    private double validateSigmaAtext() {
        return sigmaValue(sigmaAtext.getText(), "Event Noise");
    }

    private double sigmaValue(String text, String sourceId) {
        try {
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(sourceId + " cannot be empty", jocularMain.errorBarPanelStage);
                return EMPTY_FIELD;
            }
            double value = Double.parseDouble(text);
            if (value <= 0.0) {
                jocularMain.showErrorDialog(sourceId + " must be > 0.0", jocularMain.errorBarPanelStage);
                return FIELD_ENTRY_ERROR;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return FIELD_ENTRY_ERROR;
        }
    }

    private double validateBaselineLevelText() {
        return levelValue(baselineLevelText.getText(), "Baseline Level");
    }

    private double validateEventLevelText() {
        return levelValue(eventLevelText.getText(), "Event Level");
    }

    private double levelValue(String text, String sourceId) {
        try {
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(sourceId + " cannot be empty", jocularMain.errorBarPanelStage);
                return EMPTY_FIELD;
            }
            double value = Double.parseDouble(text);
            return value;

        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(sourceId + " number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return FIELD_ENTRY_ERROR;
        }
    }

    @FXML
    void respondToRightButtonClick() {
        revertToOriginalAxisScaling();
    }
    
    private void revertToOriginalAxisScaling() {
        mainChart.getXAxis().setAutoRanging(true);
        mainChart.getYAxis().setAutoRanging(true);
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Add zoom, pan, and marker managers to 'chart'.  Note that this will
        // also turn off animation in the chart and axes and turn off auto-ranging
        // on the axes.
        smartChart = new ManagedChart(mainChart);
    }

}
