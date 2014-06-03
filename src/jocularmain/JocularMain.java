package jocularmain;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.log;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javax.imageio.ImageIO;
import utils.ErrorBarItem;
import utils.JocularUtils;
import static utils.JocularUtils.aicc;
import static utils.JocularUtils.logL;
import utils.MonteCarloResult;
import utils.Observation;
import utils.SqSolution;

/**
 *
 * @author Bob Anderson
 */
public class JocularMain extends Application {

    private Observation obsInMainPlot = null;
    private SqSolution currentSqSolution = null;
    private HashMap<String, ErrorBarItem> errBarData;
    
    private RootViewController rootViewController;
    private Stage sampleDataDialogStage;
    private ArrayList<Stage> openHelpScreenList = new ArrayList<>();

    public Scene mainScene;
    public Stage errorBarPanelStage;
    public Scene errorBarPanelScene;
    public Stage primaryStage;

    public SolverService solverService = new SolverService();
    //public ErrBarService errBarService = new ErrBarService();

    private List<ErrBarService> multiCoreErrBarServices = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            ErrBarService ebs = new ErrBarService();
            multiCoreErrBarServices.add(ebs);
        }
        
        // We save this reference because some other dialogs that we will be
        // creating need to be able to provide it as a 'parent'
        this.primaryStage = primaryStage;

        // We are using help screens that are 'unowned' stages. If the user left any
        // of them open at this close request, we will close them for him. To accomplish
        // that, we register a 'listener' that will run when the client asks that the
        // application be closed.
        primaryStage.setOnCloseRequest(this::closeAllRemainingHelpScreens);

        URL fxmlLocation = getClass().getResource("RootView.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        AnchorPane page = fxmlLoader.load();
        Scene scene = new Scene(page);

        mainScene = scene;

        // Save a reference to RootViewController.  We will need to call
        // public methods provided by RootViewController to make things
        // happen on the main display screen.
        rootViewController = fxmlLoader.getController();

        scene.getStylesheets().add(this.getClass().getResource("JocularStyleSheet.css").toExternalForm());

        primaryStage.titleProperty().set("Jocular 0.6");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Give view controllers a reference to JocularMain so that they
        // can invoke methods provided by this class. We give all view
        // controllers a static method that we can call to provide the
        // reference to JocularMain.
        RootViewController.setMainApp(this);
        SampleDataDialogController.setMainApp(this);

        // Create the sample data dialog only once.  Don't display
        // on build.  Only display when asked by a call to showSampleDataDialog()
        // We do this so that the dialog is 'sticky' --- entries persist bewtween
        // 'showings'.
        buildSampleDataDialog();
    }

    private void closeAllRemainingHelpScreens(WindowEvent e) {
        for (Stage s : openHelpScreenList) {
            s.close();
        }
    }

    public void showErrorBarTool() {
        try {
            URL fxmlLocation = getClass().getResource("ErrorBarFXML.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            AnchorPane page = fxmlLoader.load();
            Scene scene = new Scene(page);
            errorBarPanelScene = scene;

            ErrorBarFXMLController controller = fxmlLoader.getController();
            ErrorBarFXMLController.setMainApp(this);

            Stage errorBarStage = new Stage();
            errorBarPanelStage = errorBarStage;

            errorBarStage.setTitle("Error Bar Study Panel");
            errorBarStage.initModality(Modality.NONE);
            errorBarStage.setResizable(true);
            errorBarStage.setScene(scene);
            errorBarStage.show();
        } catch (Exception e) {
            System.out.println("in showErrorBarTool(): " + e.toString());
        }
    }

    public void showHelpDialog(String helpFile) {
        try {
            URL fxmlLocation = getClass().getResource("HelpDialog.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            AnchorPane page = fxmlLoader.load();
            Scene scene = new Scene(page);

            // Help dialogs are special in that we are not going to
            // give them an 'owner'.  That lets us move them around
            // the screen independently of RootView.  In particular, you
            // can move RootView without the Help dialogs moving too.
            // However, that means that when RootView is closed, these
            // dialogs will not be automatically closed too.  For that
            // reason, we record this new stage in a list that will
            // be maintained and used by an on-close method attached to
            // RootView that will take care of closing any open help dialogs.
            Stage helpStage = new Stage();
            openHelpScreenList.add(helpStage);
            helpStage.setOnCloseRequest(e -> openHelpScreenList.remove((Stage) e.getSource()));

            helpStage.setTitle("Jocular documentation");
            helpStage.initModality(Modality.NONE);
            helpStage.setResizable(true);

            WebView browser = (WebView) scene.lookup("#browser");
            WebEngine webEngine = browser.getEngine();

            URL helpFileURL = getClass().getResource(helpFile);
            webEngine.load(helpFileURL.toExternalForm());

            helpStage.setScene(scene);
            helpStage.show();
        } catch (Exception e) {
            System.out.println("in showHelpDialog(): " + e.toString());
        }
    }

    public void showErrorDialog(String msg, Stage owner) {
        try {
            URL fxmlLocation = getClass().getResource("ErrorDialog.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            AnchorPane page = fxmlLoader.load();
            Scene scene = new Scene(page);

            ErrorDialogController controller = fxmlLoader.getController();
            controller.showError(msg);

            Stage stage = new Stage(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.initOwner(owner);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            System.out.println("in showErrorDialog(): " + e.toString());
        }
    }

    public void showInformationDialog(String msg, Stage owner) {
        try {
            URL fxmlLocation = getClass().getResource("InformationDialog.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            AnchorPane page = fxmlLoader.load();
            Scene scene = new Scene(page);

            InformationDialogController controller = fxmlLoader.getController();
            controller.showInformation(msg);

            Stage stage = new Stage(StageStyle.UTILITY);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.initOwner(owner);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            System.out.println("in showInformationDialog(): " + e.toString());
        }
    }

    private void buildSampleDataDialog() {
        try {
            URL fxmlLocation = getClass().getResource("SampleDataDialog.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            AnchorPane page = fxmlLoader.load();
            Scene scene = new Scene(page);

            Stage stage = new Stage(StageStyle.UTILITY);
            stage.initModality(Modality.NONE);
            stage.setResizable(false);

            stage.initOwner(primaryStage);
            stage.setScene(scene);

            // We do not 'show' the dialog on the initial build.
            // That is left to a call to showSampleDataDialog()
            // Instead, we save a private refence to the 'stage' so
            // that we can 'show' it when later asked by a call tp
            // showSampleDataDialog()
            sampleDataDialogStage = stage;
        } catch (Exception e) {
            System.out.println("in buildSampleDataDialog(): " + e.toString());
        }
    }

    public void saveSnapshotToFile(WritableImage wim, Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image as png file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("png", "*.png"));

        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try {
                boolean okOrNot = ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", file);
                if (okOrNot) {
                    showInformationDialog("Wrote file: " + file, owner);
                } else {
                    showErrorDialog("Failed to write: " + file, owner);
                }
            } catch (IOException e) {
                showErrorDialog(e.getMessage(), owner);
            }
        }
    }

    public void showSampleDataDialog() {
        sampleDataDialogStage.show();
    }

    public void repaintObservationAndSolution() {
        rootViewController.showObservationDataWithTheoreticalLightCurve(obsInMainPlot, currentSqSolution);
    }

    public void repaintObservation() {
        rootViewController.showObservationDataAlone(obsInMainPlot);
    }

    public boolean inRange(int index) {
        return (index >= 0) && (index < obsInMainPlot.lengthOfDataColumns);
    }

    /**
     *
     * @return the first readingNumber to the right of the trimmed observation.
     */
    public int getOutOfRangeOfObsOnTheRight() {
        return obsInMainPlot.readingNumbers[obsInMainPlot.readingNumbers.length - 1] + 1;
    }

    /**
     *
     * @return the first readingNumber to the left of the trimmed observation.
     */
    public int getOutOfRangeOfObsOnTheLeft() {
        return obsInMainPlot.readingNumbers[0] - 1;
    }

    public Observation getCurrentObservation() {
        return obsInMainPlot;
    }

    public void setCurrentObservation(Observation newObs) {
        obsInMainPlot = newObs;
        currentSqSolution = null;
        errBarData = null;
    }

    public SqSolution getCurrentSolution() {
        return currentSqSolution;
    }

    public void setCurrentSolution(SqSolution newSolution) {
        currentSqSolution = newSolution;
        errBarData = null;
    }

    public void setCurrentErrBarValues(HashMap<String, ErrorBarItem> errBarData) {
        this.errBarData = errBarData;
    }
    
    public HashMap<String, ErrorBarItem> getCurrentErrBarValues() {
        return errBarData;
    }
    
    public void addSampleCurveToMainPlot(SqSolution solution) {
        rootViewController.addSampleCurveToMainPlot(solution);
    }

    public void addSolutionCurveToMainPlot(SqSolution solution) {
        rootViewController.addSolutionCurveToMainPlot(solution);
    }

    public void clearSolutionList() {
        rootViewController.clearSolutionList();
    }

    public class SolverService extends Service<List<SqSolution>> {

        // private variables go here
        int[] dTranCandidates;
        int[] rTranCandidates;
        int n;
        SqModel sqmodel;
        double sigmaB;
        double sigmaA;
        double solMagDrop;
        double minMagDrop;
        double maxMagDrop;
        int numValidTranPairs;
        SolutionStats solutionStats;

        // setters of private variable go here
        public final void setsolutionStats(SolutionStats solutionStats) {
            this.solutionStats = solutionStats;
        }

        public final void setdTranCandidates(int[] dTran) {
            dTranCandidates = dTran;
        }

        public final void setrTranCandidates(int[] rTran) {
            rTranCandidates = rTran;
        }

        public final void setsqmodel(SqModel sqmodel) {
            this.sqmodel = sqmodel;
        }

        public final void setsigmaB(double sigmaB) {
            this.sigmaB = sigmaB;
        }

        public final void setsigmaA(double sigmaA) {
            this.sigmaA = sigmaA;
        }

        public final void setn(int n) {
            this.n = n;
        }

        public final void setminMagDrop(double minMagDrop) {
            this.minMagDrop = minMagDrop;
        }

        public final void setmaxMagDrop(double maxMagDrop) {
            this.maxMagDrop = maxMagDrop;
        }

        // getters go here
        public final int getNumValidTranPairs() {
            return numValidTranPairs;
        }

        public final SolutionStats getsolutionStats() {
            return solutionStats;
        }

        @Override
        protected Task<List<SqSolution>> createTask() {
            return new Task<List<SqSolution>>() {
                @Override
                protected List<SqSolution> call() {
                    List<SqSolution> sqsolutions = new ArrayList<>();
                    // work goes in here
                    numValidTranPairs = 0;
                    int loopCount = 0;
                    int maxLoopCount = dTranCandidates.length * rTranCandidates.length;
                    search:
                    for (int i = 0; i < dTranCandidates.length; i++) {
                        for (int j = 0; j < rTranCandidates.length; j++) {
                            if (isCancelled()) {
                                break search;
                            }
                            loopCount++;
                            updateProgress(loopCount, maxLoopCount);
                            SqSolution newSolution = new SqSolution();
                            newSolution.dTransitionIndex = dTranCandidates[i];
                            newSolution.rTransitionIndex = rTranCandidates[j];

                            newSolution.logL = sqmodel
                                .setDtransition(newSolution.dTransitionIndex)
                                .setRtransition(newSolution.rTransitionIndex)
                                .calcLogL(sigmaB, sigmaA);

                            // We let sqmodel determine when a dTran:rTran
                            // combination is valid.  It lets us know the combo
                            // is invalid (too few event or baseline points) by
                            // returning NaN for logL.
                            if (Double.isNaN(newSolution.logL)) {
                                continue;
                            }

                            solMagDrop = JocularUtils.calcMagDrop(sqmodel.getB(), sqmodel.getA());

                            if (Double.isNaN(solMagDrop) || solMagDrop < minMagDrop || solMagDrop > maxMagDrop) {
                                continue;
                            }

                            numValidTranPairs++;

                            newSolution.D = sqmodel.getDsolution();
                            newSolution.R = sqmodel.getRsolution();
                            newSolution.B = sqmodel.getB();
                            newSolution.A = sqmodel.getA();
                            newSolution.numBaselinePoints = sqmodel.getNumBaselinePoints();
                            newSolution.numEventPoints = sqmodel.getNumEventPoints();
                            newSolution.magDrop = solMagDrop;
                            newSolution.sigmaB = sigmaB;
                            newSolution.sigmaA = sigmaA;
                            newSolution.kFactor = sqmodel.getkFactor();
                            newSolution.aicc = JocularUtils.aicc(newSolution.logL, newSolution.kFactor, n);

                            sqsolutions.add(newSolution);
                        }
                    }
                    return sqsolutions;
                }
            };
        }
    }
    
    public void errBarServiceStart (TrialParams trialParams,
                                    EventHandler<WorkerStateEvent> successHandler,
                                    EventHandler<WorkerStateEvent> cancelledHandler,
                                    EventHandler<WorkerStateEvent> failedHandler,
                                    DoubleProperty progressProperty) {
        
        int totalTrials = trialParams.numTrials;
        int numCores = Runtime.getRuntime().availableProcessors();
        int numTrialsPerCore = totalTrials / numCores;
        
        for (ErrBarService ebs : multiCoreErrBarServices) {
            progressProperty.bind(ebs.progressProperty());
            ebs.settrialParams(trialParams);
            ebs.settrialsPerCore(numTrialsPerCore);
            ebs.setOnSucceeded(successHandler);
            ebs.setOnCancelled(cancelledHandler);
            ebs.setOnFailed(failedHandler);
        }
        
        // If the numTrials was not an even multiple of the number of cores,
        // we need to add the remainder to one of the threads.
        multiCoreErrBarServices.get(0).setExtraTrials(totalTrials % numCores);
        
        for (ErrBarService ebs : multiCoreErrBarServices) {
            ebs.reset();
            ebs.restart();
        }
        
    }
    
    public boolean errBarServiceFinished() {
        for (ErrBarService ebs : multiCoreErrBarServices) {
            if (ebs.getState() != Worker.State.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }
    
    public boolean errBarServiceRunning() {
        for (ErrBarService ebs : multiCoreErrBarServices) {
            if (ebs.getState() == Worker.State.RUNNING || ebs.getState() == Worker.State.SCHEDULED) {
                return true;
            }
        }
        return false;
    }
    
    public MonteCarloResult getErrBarServiceCumResults() {
        MonteCarloResult monteCarloResult = new MonteCarloResult();
        MonteCarloResult partialResult = new MonteCarloResult();

        int numPoints = multiCoreErrBarServices.get(0).ans.histogram.length;
        monteCarloResult.histogram = new int[numPoints];

        for (ErrBarService ebs : multiCoreErrBarServices) {
            partialResult = ebs.getAnswer();
            for (int i = 0; i < partialResult.histogram.length; i++) {
                monteCarloResult.histogram[i] += partialResult.histogram[i];
            }
            monteCarloResult.numRejections += partialResult.numRejections;
        }
        return monteCarloResult;
    }
    
    public void cancelErrBarService() {
        for (ErrBarService ebs : multiCoreErrBarServices) {
            ebs.cancel();
        }
    }

    private class ErrBarService extends Service<Void> {

        private TrialParams trialParams;
        private MonteCarloResult ans;
        private int extraTrials=0;
        private int trialsPerCore=0;

        public void settrialParams(TrialParams trialParams) {
            this.trialParams = trialParams;
        }
        
        public void setExtraTrials(int extraTrials) {
            this.extraTrials = extraTrials;
        }
        
        public void settrialsPerCore(int trialsPerCore) {
            this.trialsPerCore = trialsPerCore;
        }

        public final MonteCarloResult getAnswer() {
            return ans;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {

                    int[] histogram = new int[trialParams.sampleWidth];

                    double[] logLB = new double[trialParams.sampleWidth];
                    double[] logLA = new double[trialParams.sampleWidth];
                    double[] logLM = new double[trialParams.sampleWidth];
                    double[] logLT = new double[trialParams.sampleWidth];
                    double[] rdgs = new double[trialParams.sampleWidth];
                    int[] offset = new int[trialParams.sampleWidth];

                    double logLstraightLine = 0.0;
                    int rejectedSolutions = 0;

                    int centerIndex = trialParams.sampleWidth / 2;

                    for (int i = 0; i < histogram.length; i++) {
                        histogram[i] = 0;
                    }

                    int trialNum = 0;

                    for (int k = 0; k < trialsPerCore + extraTrials; k++) {
                        if (isCancelled()) {
                            break;
                        }

                        trialNum++;
                        updateProgress(trialNum, trialsPerCore);

                        // Make a new sample
                        for (int i = 0; i < centerIndex; i++) {
                            rdgs[i] = ThreadLocalRandom.current().nextGaussian() * trialParams.sigmaA
                                + trialParams.eventLevel;
                        }

                        if (trialParams.mode == MonteCarloMode.LEFT_EDGE) {
                            rdgs[centerIndex] = ThreadLocalRandom.current().nextGaussian() * trialParams.sigmaA
                                + trialParams.eventLevel;
                        } else if (trialParams.mode == MonteCarloMode.RIGHT_EDGE) {
                            rdgs[centerIndex] = ThreadLocalRandom.current().nextGaussian() * trialParams.sigmaB
                                + trialParams.baselineLevel;
                        } else if (trialParams.mode == MonteCarloMode.MID_POINT) {
                            rdgs[centerIndex] = ThreadLocalRandom.current().nextGaussian() * ((trialParams.sigmaA + trialParams.sigmaB) / 2.0)
                                + (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
                        } else if (trialParams.mode == MonteCarloMode.RANDOM) {
                            double frac = ThreadLocalRandom.current().nextDouble();
                            double level = trialParams.baselineLevel * (1.0 - frac) + trialParams.eventLevel * frac;
                            double noiseAtLevel = trialParams.sigmaB * (1.0 - frac) + trialParams.sigmaA * frac;
                            rdgs[centerIndex] = ThreadLocalRandom.current().nextGaussian() * noiseAtLevel + level;
                        } else {
                            throw new InternalError("Design error: MonteCarlo mode not implemented.");
                        }

                        for (int i = centerIndex + 1; i < rdgs.length; i++) {
                            rdgs[i] = ThreadLocalRandom.current().nextGaussian() * trialParams.sigmaB
                                + trialParams.baselineLevel;
                        }

                        // The test case has now been created.  Calculate the various arrays that depend on this case.
                        double midLevel = (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
                        double midSigma = (trialParams.sigmaA + trialParams.sigmaB) / 2.0;

                        logLstraightLine = 0.0;
                        for (int i = 0; i < trialParams.sampleWidth; i++) {
                            logLB[i] = logL(rdgs[i], trialParams.baselineLevel, trialParams.sigmaB);
                            logLA[i] = logL(rdgs[i], trialParams.eventLevel, trialParams.sigmaA);
                            if (logLB[i] > logLA[i]) {
                                logLM[i] = logLB[i];
                                offset[i] = -1;
                            } else {
                                logLM[i] = logLA[i];
                                offset[i] = 0;
                            }
                            logLstraightLine += logL(rdgs[i], midLevel, midSigma);
                        }

                        // Initialize first cum logL term to begin iterative computation
                        logLT[0] = logLM[0];
                        for (int i = 1; i < trialParams.sampleWidth; i++) {
                            logLT[0] += logLB[i];
                        }

                        for (int i = 1; i < trialParams.sampleWidth; i++) {
                            logLT[i] = logLT[i - 1] - logLM[i - 1] - logLB[i] + logLA[i - 1] + logLM[i];
                        }

                        int indexOfMaxLogL = getIndexOfMaxValue(logLT);
                        int transitionIndex = indexOfMaxLogL + offset[indexOfMaxLogL];
                        if (transitionIndex < 0) {
                            transitionIndex = 0;
                        }

                        // We will discard any trail that result in a detected edge that is more 'probable'
                        // than a simple straight line by at least 50
                        double aiccMarginOfEdgeOverLine = log(50.0);

                        if (aicc(logLstraightLine, 1, trialParams.sampleWidth)
                            < aicc(logLT[indexOfMaxLogL], 3, trialParams.sampleWidth) + aiccMarginOfEdgeOverLine) {
                            // The trial does not contain a valid edge.
                            rejectedSolutions += 1;
                            k--; // Force a repeat try
                            if (rejectedSolutions >= trialParams.numTrials) {
                                break;
                            }
                        } else {
                            histogram[transitionIndex]++;
                        }
                    }

                    ans = new MonteCarloResult();
                    ans.histogram = histogram;
                    ans.numRejections = rejectedSolutions;

                    return null;
                }
            };
        }

        private int getIndexOfMaxValue(double[] values) {
            double highest = values[0];
            int index = 0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > highest) {
                    highest = values[i];
                    index = i;
                }
            }
            return index;
        }
    }

}
