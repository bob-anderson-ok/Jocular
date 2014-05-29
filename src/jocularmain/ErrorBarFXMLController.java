package jocularmain;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

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
    ListView mainListView;

    @FXML
    public void showErrorBarHelp() {
        jocularMain.showHelpDialog("Help/errorbar.help.html");
    }
    
    @FXML
    public void writePanelToFile() {
        WritableImage wim = jocularMain.errorBarPanelScene.snapshot(null);
        saveSnapshotToFile(wim);
    }

    private void saveSnapshotToFile(WritableImage wim) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image as png file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("png", "*.png"));

        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try {
                boolean okOrNot = ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", file);
                if (okOrNot) {
                    jocularMain.showInformationDialog("Wrote file: " + file, jocularMain.primaryStage);
                } else {
                    jocularMain.showErrorDialog("Failed to write: " + file, jocularMain.primaryStage);
                }
            } catch (IOException e) {
                jocularMain.showErrorDialog(e.getMessage(), jocularMain.primaryStage);
            }
        }
    }


    @FXML
    public void calculateDistribution() {

        baselineLevel = validateBaselineLevelText();
        if (baselineLevel == EMPTY_FIELD || baselineLevel == FIELD_ENTRY_ERROR) {
            return;
        }

        eventLevel = validateEventLevelText();
        if (eventLevel == EMPTY_FIELD || eventLevel == FIELD_ENTRY_ERROR) {
            return;
        }

        sigmaB = validateSigmaBtext();
        if (sigmaB == EMPTY_FIELD || sigmaB == FIELD_ENTRY_ERROR) {
            return;
        }

        sigmaA = validateSigmaAtext();
        if (sigmaA == EMPTY_FIELD || sigmaA == FIELD_ENTRY_ERROR) {
            return;
        }

        try {
            String text = numPointsText.getText();
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(" Number of points in trial cannot be empty", jocularMain.errorBarPanelStage);
                return;
            }
            numPoints = Integer.parseInt(text);
            if (numPoints <= 0.0) {
                jocularMain.showErrorDialog(" Number of points in trial must be > 0.0", jocularMain.errorBarPanelStage);
                return;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(" number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return;
        }

        try {
            String text = numTrialsText.getText();
            if (text.isEmpty()) {
                jocularMain.showErrorDialog(" Number of trials cannot be empty", jocularMain.errorBarPanelStage);
                return;
            }
            numTrials = Integer.parseInt(text);
            if (numTrials <= 0.0) {
                jocularMain.showErrorDialog(" Number of trials must be > 0.0", jocularMain.errorBarPanelStage);
                return;
            }
        } catch (NumberFormatException e) {
            jocularMain.showErrorDialog(" number format error: " + e.getMessage(), jocularMain.errorBarPanelStage);
            return;
        }

        // Just in case none of the radio buttons referenced below is selected, we have a GUI design
        // error, which we crassly turn into an NPE
        monteCarloMode = null;

        if (randomRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.RANDOM;
        }
        if (leftEdgeRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.LEFT_EDGE;
        }
        if (midPointRadioButton.isSelected()) {
            monteCarloMode = MonteCarloMode.MID_POINT;
        }

        // OK, time to set up the trial...
        trialParams.baselineLevel = baselineLevel;
        trialParams.eventLevel = eventLevel;
        trialParams.numTrials = numTrials;
        trialParams.sampleWidth = numPoints;
        trialParams.mode = monteCarloMode;
        trialParams.sigmaB = sigmaB;
        trialParams.sigmaA = sigmaA;

        // ... and run it.
        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        int[] histogram = monteCarloTrial.calcHistogram();

        ObservableList<String> items = FXCollections.observableArrayList();
        items = FXCollections.observableArrayList();

        items.add("histogram: ");
        for (int i = 0; i < histogram.length; i++) {
            items.add(String.format("%3d %,8d", i, histogram[i]));
        }

        mainListView.setItems(items);

        plotData(histogram);
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
