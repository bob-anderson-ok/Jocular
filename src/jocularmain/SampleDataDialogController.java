package jocularmain;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import utils.Observation;
import utils.JocularUtils;
import utils.SampleDataGenerator;
import utils.SqSolution;

public class SampleDataDialogController {

    private static JocularMain jocularMain;

    public static void setMainApp(JocularMain main) {
        jocularMain = main;
    }

    @FXML
    TextField sigmaBtext;
    @FXML
    TextField sigmaAtext;
    @FXML
    TextField baselineIntensityText;
    @FXML
    TextField eventIntensityText;
    @FXML
    TextField dTimeText;
    @FXML
    TextField rTimeText;
    @FXML
    TextField numberOfDataPointsText;
    @FXML
    TextField randSeedText;
    @FXML
    Label errorLabel;
    @FXML
    CheckBox showSampleLightcurveCheckbox;
    @FXML
    Label falsePositiveLabel;

    @FXML
    private void handleKeyPress(KeyEvent key) {
        if (key.getCode() == KeyCode.ENTER) {
            checkParametersAndDisplayIfValid();
        }
    }

    @FXML
    void doCreateNewSample() {
        if (jocularMain.solverServiceRunning()) {
            jocularMain.showInformationDialog("This operation is blocked: solution process on current" +
                " observation is in progress.", jocularMain.errorBarPanelStage);
            return;
        }
        checkParametersAndDisplayIfValid();
    }

    @FXML
    void showRandGenSeedHelp() {
        jocularMain.showHelpDialog("Help/randgenseed.help.html");
    }

    private void checkParametersAndDisplayIfValid() {
        if (validateParameters()) {
            try {
                if (!randSeedText.getText().isEmpty()) {
                    long randomSeed = Long.parseLong(randSeedText.getText());
                    JocularUtils.setGaussianGeneratorSeed(randomSeed);
                }
                Observation sampleObs = createSampleData();

                SqSolution sampleSolution = new SqSolution();
                sampleSolution.B = baselineIntensity;
                sampleSolution.A = eventIntensity;
                sampleSolution.D = dTime;
                sampleSolution.R = rTime;
                double signal = baselineIntensity - eventIntensity;
                double falsePos = JocularUtils.falsePositiveProbability(
                    signal, 
                    sigmaB, 
                    (int) (rTime - dTime), 
                    numberOfDataPoints
                );
                
                falsePositiveLabel.setText(String.format("False Positive Probability: %.4f", falsePos));

                jocularMain.setCurrentObservation(sampleObs);
                jocularMain.setCurrentSolution(sampleSolution);
                if (showSampleLightcurveCheckbox.isSelected()) {
                    jocularMain.repaintObservationAndSolution();
                } else {
                    jocularMain.repaintObservation();
                }
                jocularMain.clearSolutionList();
                jocularMain.setCurrentSolution(null);

            } catch (NumberFormatException e) {
                errorLabel.setText("Error creating artificial data: " + e.getMessage());
            }
        }
    }

    private double sigmaA;
    private double sigmaB;
    private double baselineIntensity;
    private double eventIntensity;
    private double dTime;
    private double rTime;
    private int numberOfDataPoints;

    private boolean validateParameters() {
        errorLabel.setText("");
        try {
            sigmaB = Double.parseDouble(sigmaBtext.getText());
            sigmaA = Double.parseDouble(sigmaAtext.getText());
            baselineIntensity = Double.parseDouble(baselineIntensityText.getText());
            eventIntensity = Double.parseDouble(eventIntensityText.getText());

            if (dTimeText.getText().isEmpty()) {
                dTime = Double.NaN;
            } else {
                dTime = Double.parseDouble(dTimeText.getText());
            }

            numberOfDataPoints = Integer.parseInt(numberOfDataPointsText.getText());

            if (numberOfDataPoints < 5) {
                errorLabel.setText("number of data points cannot be lesss than 5");
                return false;
            }

            if (rTimeText.getText().isEmpty()) {
                rTime = Double.NaN;
            } else {
                rTime = Double.parseDouble(rTimeText.getText());
            }

            if (rTimeText.getText().isEmpty() && dTimeText.getText().isEmpty()) {
                errorLabel.setText("D and R cannot both be missing.");
                return false;
            }

            if (baselineIntensity < eventIntensity) {
                errorLabel.setText("B (baseline intensity) cannot be less than A (event intensity)");
                return false;
            }

            if (rTime < dTime) {
                errorLabel.setText("D cannot occur after R");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            errorLabel.setText("Number format error: " + e.getMessage());
            return false;
        }
    }

    private Observation createSampleData() {
        
        SampleDataGenerator dataGen = new SampleDataGenerator("artificial data");

        dataGen
            .setDevent(dTime)
            .setRevent(rTime)
            .setSigmaA(sigmaA)
            .setSigmaB(sigmaB)
            .setAintensity(eventIntensity)
            .setBintensity(baselineIntensity)
            .setNumDataPoints(numberOfDataPoints)
            .setParams();

        return dataGen.build();

    }
}
