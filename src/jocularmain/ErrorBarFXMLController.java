package jocularmain;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class ErrorBarFXMLController implements Initializable {

    private static final int EMPTY_FIELD = -1;
    private static final int FIELD_ENTRY_ERROR = -2;

    private double baselineLevel;
    private double eventLevel;
    private double sigmaB;
    private double sigmaA;
    private int numPoints;
    private int numTrials;

    private static JocularMain jocularMain;

    public static void setMainApp(JocularMain main) {
        jocularMain = main;
    }

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
    RadioButton randomRadioButton;
    @FXML
    RadioButton leftEdgeRadioButton;
    @FXML
    RadioButton midPointRadioButton;

    @FXML
    public void calculateDistribution() {
        System.out.println("calculateDistribution clicked");
        sigmaB = validateSigmaBtext();
        if (sigmaB == EMPTY_FIELD || sigmaB == FIELD_ENTRY_ERROR) {
            return;
        }

    }

    @FXML
    public void writePanelToFile() {
        System.out.println("writePanelToFile clicked");
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Don't need to intialize anything here.
    }

}
