package jocularmain;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import utils.ErrBarUtils;
import utils.ErrorBarItem;
import utils.HistStatItem;
import utils.MonteCarloResult;

public class ErrorBarFXMLController implements Initializable {

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
    RadioButton randomRadioButton;
    @FXML
    RadioButton leftEdgeRadioButton;
    @FXML
    RadioButton midPointRadioButton;

    @FXML
    RadioButton onexRadioButton;
    @FXML
    RadioButton twoxRadioButton;
    @FXML
    RadioButton fivexRadioButton;
    @FXML
    RadioButton tenxRadioButton;
    @FXML
    RadioButton twentyxRadioButton;
    @FXML
    RadioButton scaleToPeakRadioButton;

    @FXML
    ListView mainListView;
    @FXML
    ListView resultsListView;

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

        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        MonteCarloResult monteCarloResult = monteCarloTrial.calcHistogram();

        if (monteCarloResult.numRejections > trialParams.numTrials / 50) {
            jocularMain.showErrorDialog("More than 2% of the trials were rejected."
                + " Possibly noise levels are too high or there are not enough points in the trial sample.",
                                        jocularMain.errorBarPanelStage);
            mainListView.setItems(null);
            resultsListView.setItems(null);
            mainChart.getData().clear();
            return;
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
        HashMap<Integer, ErrorBarItem> errBarData = ErrBarUtils.getInstance().getErrorBars(statsArray, centered);

        // Display the error bar stats in the resultsListView
        ObservableList<String> resultItems = FXCollections.observableArrayList();
        resultItems.add("CI     CI act  left   peak   right  width    +/-");
        resultItems.add(toStringErrBarItem(errBarData.get(68)));
        resultItems.add(toStringErrBarItem(errBarData.get(90)));
        resultItems.add(toStringErrBarItem(errBarData.get(95)));
        resultItems.add(toStringErrBarItem(errBarData.get(99)));
        resultItems.add(String.format("\n%,d samples were rejected on the way to %,d good trials.",
                                      monteCarloResult.numRejections, trialParams.numTrials));
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

        int yMax;

        if (twoxRadioButton.isSelected()) {
            yMax = numTrials / 2;
        } else if (fivexRadioButton.isSelected()) {
            yMax = numTrials / 5;
        } else if (tenxRadioButton.isSelected()) {
            yMax = numTrials / 10;
        } else if (twentyxRadioButton.isSelected()) {
            yMax = numTrials / 20;
        } else if (scaleToPeakRadioButton.isSelected()) {
            yMax = maxValue(values);
        } else {
            yMax = numTrials;
        }

        NumberAxis yaxis = (NumberAxis) mainChart.getYAxis();
        yaxis.setLowerBound(-yMax / 10);
        yaxis.setUpperBound(yMax + yMax / 10);
        yaxis.setTickUnit(yMax / 10);

        NumberAxis xaxis = (NumberAxis) mainChart.getXAxis();
        xaxis.setLowerBound(-numPoints / 10);
        xaxis.setUpperBound(numPoints + numPoints / 10);
        xaxis.setTickUnit(numPoints / 10);

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Don't need to intialize anything here.
    }

}
