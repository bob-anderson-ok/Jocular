package jocularmain;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import utils.Observation;
import utils.RandUtils;
import utils.SampleDataGenerator;
import utils.SqSolution;

public class SampleDataDialogController {

    private static JocularMain mainApp;

    public static void setMainApp(JocularMain main) {
        mainApp = main;
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
    private void handleKeyPress(KeyEvent key) {
        if (key.getCode() == KeyCode.ENTER) {
            checkParametersAndDisplayIfValid();
        }
    }

    @FXML
    void handleMouseClick() {
        checkParametersAndDisplayIfValid();
    }
    
    @FXML
    void showRandGenSeedHelp() {
        mainApp.showHelpDialog("Help/randgenseed.help.html");
    }

    private void checkParametersAndDisplayIfValid() {
        if (validateParameters()) {
            try {
                if (!randSeedText.getText().isEmpty()) {
                    long randomSeed = Long.parseLong(randSeedText.getText());
                    RandUtils.setGaussianGeneratorSeed(randomSeed);
                }
                Observation sampleObs = createSampleData();
                
                SqSolution sampleSolution = new SqSolution();
                sampleSolution.B = baselineIntensity;
                sampleSolution.A = eventIntensity;
                sampleSolution.D = dTime;
                sampleSolution.R = rTime;
                
                mainApp.obsInMainPlot = sampleObs;
                mainApp.currentSqSolution = sampleSolution;
                
                mainApp.rootViewControllerInstance.showArtificialData(sampleObs, sampleSolution);
                
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
            
            if (rTimeText.getText().isEmpty()) {
                rTime = Double.NaN;
            } else {
                rTime = Double.parseDouble(rTimeText.getText());
            }
            
            if (rTimeText.getText().isEmpty() && dTimeText.getText().isEmpty()) {
                errorLabel.setText( "D and R cannot both be missing.");
                return false;
            }
            
            if ( baselineIntensity < eventIntensity) {
                errorLabel.setText( "B (baseline intensity) cannot be less than A (event intensity)");
                return false;
            }
            
            if ( rTime < dTime) {
                errorLabel.setText( "D cannot occur after R");
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
