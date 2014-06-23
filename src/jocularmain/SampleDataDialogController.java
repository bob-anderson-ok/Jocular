package jocularmain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
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
    CheckBox simulateIntegratedCameraOutput;
    @FXML
    TextField binCountText;
    @FXML
    TextField offsetText;
    @FXML
    TextField processingNoiseText;

    @FXML
    void simulateIntegratedOutputClicked() {
        if (simulateIntegratedCameraOutput.isSelected()) {
            binCountText.setEditable(true);
            offsetText.setEditable(true);
            processingNoiseText.setEditable(true);
            binCountText.setPromptText("enabled");
            offsetText.setPromptText("enabled");
            processingNoiseText.setPromptText("enabled");
        } else {
            binCountText.setEditable(false);
            offsetText.setEditable(false);
            processingNoiseText.setEditable(false);
            binCountText.setPromptText("disabled");
            offsetText.setPromptText("disabled");
            processingNoiseText.setPromptText("disabled");
            binCountText.setText("");
            offsetText.setText("");
            processingNoiseText.setText("");
        }
    }

    @FXML
    void showSimulatedIntegratedCameraOutputHelp() {
        jocularMain.showHelpDialog("Help/integratedcameraoutput.help.html");
    }

    @FXML
    void writeSampleToFile() {
        if (!validateParameters()) {
            jocularMain.showErrorDialog("Invalid or missing parameter settings.", jocularMain.primaryStage);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Write sample data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tangra file", "*.csv"));
        fileChooser.setInitialFileName("*.csv");
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                FileWriter writer = new FileWriter(file);
                writeHeaderLines(writer);
                writeDataLines(writer);
                writer.close();
            } catch (IOException e) {
                jocularMain.showErrorDialog(e.getMessage(), jocularMain.primaryStage);
            }
        }
    }

    private void writeHeaderLines(FileWriter writer) throws IOException {
        writer.write("Tangra format file from Jocular sample data generator\n");
        writer.write("\n");
        writer.write("sigmaB = " + sigmaBtext.getText() + "\n");
        writer.write("sigmaA = " + sigmaAtext.getText() + "\n");
        writer.write("\n");
        writer.write("baselineIntensity = " + baselineIntensityText.getText() + "\n");
        writer.write("eventIntensity = " + eventIntensityText.getText() + "\n");
        writer.write("\n");
        if (!Double.isNaN(dTime)) {
            writer.write("D (FrameNum) = " + dTimeText.getText() + "\n");
        }
        if (!Double.isNaN(rTime)) {
            writer.write("R (FrameNum) = " + rTimeText.getText() + "\n");
        }
        writer.write("\n");
        if (!Double.isNaN(dTime)) {
            if (dTime >= 0.0) {
                writer.write("D (time) = " + frameNumToUTC(dTime) + "\n");
            } else {
                writer.write("D (time) = -" + frameNumToUTC(-dTime) + "\n");
            }
        }
        if (!Double.isNaN(rTime)) {
            writer.write("R (time) = " + frameNumToUTC(rTime) + "\n");
        }
        writer.write("\n");
        writer.write("numPoints = " + numberOfDataPointsText.getText() + "\n");
        writer.write("\n");
        if (simulateIntegratedCameraOutput.isSelected()) {
            writer.write("binCount = " + binCountText.getText() + "\n");
            writer.write("offset = " + offsetText.getText() + "\n");
            writer.write("processNoise = " + processingNoiseText.getText() + "\n");
            writer.write("\n");
        }
        if (!randSeedText.getText().isEmpty()) {
            writer.write("randGenSeed = " + randSeedText.getText() + "\n");
            writer.write("\n");
        }
        writer.write("FrameNo,Time (UT),Signal (1),Background (1)\n");
        //writer.write("No.,Signal1,Signal2,H,M,S,,,,/Frame,Object1,Object2,,,,,,,\n");
    }

    private String frameNumToUTC(double frameNum) {
        double fNet = frameNum;
        int hours = (int) Math.floor(fNet / 3600.0);
        fNet = fNet - 3600 * hours;
        int minutes = (int) Math.floor(fNet / 60.0);
        double seconds = fNet - 60 * minutes;
        return String.format("%02d:%02d:%06.3f", hours, minutes, seconds);
    }

    private String frameNumToHMS(double frameNum) {
        double fNet = frameNum;
        int hours = (int) Math.floor(fNet / 3600.0);
        fNet = fNet - 3600 * hours;
        int minutes = (int) Math.floor(fNet / 60.0);
        double seconds = fNet - 60 * minutes;
        return String.format("%02d,%02d,%06.3f", hours, minutes, seconds);
    }

    private void writeDataLines(FileWriter writer) throws IOException {
        Observation curObs = jocularMain.getCurrentObservation();
        for (int i = 0; i < curObs.obsData.length; i++) {
            int frameNum = curObs.readingNumbers[i];
            double obsValue = curObs.obsData[i];
            String line = String.format(
                "%d,%s,%.2f,0\n",
                frameNum,
                frameNumToUTC(frameNum),
                obsValue
            );
            writer.write(line);
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent key) {
        if (key.getCode() == KeyCode.ENTER) {
            checkParametersAndDisplayIfValid();
        }
    }

    @FXML
    void doCreateNewSample() {
        if (jocularMain.solverServiceRunning()) {
            jocularMain.showInformationDialog("This operation is blocked: solution process on current"
                + " observation is in progress.", jocularMain.errorBarPanelStage);
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
                if (dTime > -1.0) {
                    sampleSolution.D = dTime;
                } else {
                    sampleSolution.D = Double.NaN;
                }
                if (rTime > numberOfDataPoints - 1) {
                    sampleSolution.R = Double.NaN;
                } else {
                    sampleSolution.R = rTime;
                }

                double signal = baselineIntensity - eventIntensity;

                int intD = (int) dTime;
                int intR = (int) rTime;
                if (intD < 0) {
                    intD = -1;
                }
                if (intR > (numberOfDataPoints - 1)) {
                    intR = numberOfDataPoints - 1;
                }
                int dur = intR - intD;
                if (dur <= 0) {
                    dur = 1;
                }
                double falsePos = JocularUtils.falsePositiveProbability(
                    signal,
                    sigmaB,
                    dur,
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
        } else {
            jocularMain.setCurrentObservation(null);
            jocularMain.setCurrentSolution(null);
            jocularMain.clearSolutionList();
            jocularMain.setCurrentSolution(null);
            jocularMain.repaintObservation();
        }
    }

    private double sigmaA;
    private double sigmaB;
    private double baselineIntensity;
    private double eventIntensity;
    private double dTime;
    private double rTime;
    private int numberOfDataPoints;
    private int offset;
    private int binSize;
    private double processingNoise;

    private boolean validateParameters() {
        errorLabel.setText("");
        try {
            sigmaB = Double.parseDouble(sigmaBtext.getText());
            sigmaA = Double.parseDouble(sigmaAtext.getText());
            baselineIntensity = Double.parseDouble(baselineIntensityText.getText());
            eventIntensity = Double.parseDouble(eventIntensityText.getText());

            if (!offsetText.getText().isEmpty()) {
                offset = Integer.parseInt(offsetText.getText());
            } else {
                offset = 0;
            }
            if (!binCountText.getText().isEmpty()) {
                binSize = Integer.parseInt(binCountText.getText());
            } else {
                binSize = 0;
            }
            if (!processingNoiseText.getText().isEmpty()) {
                processingNoise = Double.parseDouble(processingNoiseText.getText());
            } else {
                processingNoise = 0.0;
            }

            if (binSize < 0) {
                errorLabel.setText("bin count cannot be negative");
                return false;
            }

            if (offset != 0 && (offset < 0 || offset >= binSize)) {
                errorLabel.setText("offset must be > 0 and < bin count");
                return false;
            }

            if (processingNoise < 0.0) {
                errorLabel.setText("processing noise cannot be negative");
                return false;
            }

            if (dTimeText.getText().isEmpty()) {
                dTime = Double.NaN;
            } else {
                dTime = Double.parseDouble(dTimeText.getText());
            }

            numberOfDataPoints = Integer.parseInt(numberOfDataPointsText.getText());

            if (numberOfDataPoints < 5) {
                errorLabel.setText("num data points cannot be less than 5");
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
                errorLabel.setText("B cannot be less than A");
                return false;
            }

            if (rTime < dTime) {
                errorLabel.setText("D cannot occur after R");
                return false;
            }

            boolean dEdgePresent = !(dTime <= -1 || dTime > numberOfDataPoints - 2);
            boolean rEdgePresent = !(rTime <= 0 || rTime > numberOfDataPoints - 1);
            if (!(dEdgePresent || rEdgePresent)) {
                errorLabel.setText("There is no edge in the data.");
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
            .setoffset(offset)
            .setbinSize(binSize)
            .setprocessingNoise(processingNoise)
            .setParams();

        return dataGen.build();

    }
}
