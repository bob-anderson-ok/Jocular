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
import javafx.beans.binding.DoubleBinding;
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
import utils.FileUtils;
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

    public String version = "Jocular 0.81";

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

    private List<ErrBarService> multiCoreErrBarServices = new ArrayList<>();
    public List<SolverService> multiCoreSolverService = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        int numCores = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < numCores; i++) {
            ErrBarService ebs = new ErrBarService();
            multiCoreErrBarServices.add(ebs);
        }

        for (int i = 0; i < numCores; i++) {
            SolverService solService = new SolverService();
            multiCoreSolverService.add(solService);
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

        primaryStage.titleProperty().set(version);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Give view controllers a reference to JocularMain so that they
        // can invoke methods provided by this class. We give all view
        // controllers a static method that we can call to provide the
        // reference to JocularMain.
        RootViewController.setMainApp(this);
        SampleDataDialogController.setMainApp(this);
        FileUtils.getInstance().setMainApp(this);

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

    public void repaintObservationAndSolution() {
        rootViewController.showObservationDataWithTheoreticalLightCurve(obsInMainPlot, currentSqSolution);
    }

    public void addSampleCurveToMainPlot(SqSolution solution) {
        rootViewController.addSampleCurveToMainPlot(solution);
    }

    //<editor-fold defaultstate="collapsed" desc="Dialogs">
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

    public void showSampleDataDialog() {
        sampleDataDialogStage.show();
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Public methods involving current Observation">
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

    public void repaintObservation() {
        rootViewController.showObservationDataAlone(obsInMainPlot);
    }

    public boolean inRange(int index) {
        return (index >= 0) && (index < obsInMainPlot.lengthOfDataColumns);
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Public methods involving current Solution">
    public SqSolution getCurrentSolution() {
        return currentSqSolution;
    }

    public void setCurrentSolution(SqSolution newSolution) {
        currentSqSolution = newSolution;
        errBarData = null;
    }

    public void addSolutionCurveToMainPlot(SqSolution solution) {
        rootViewController.addSolutionCurveToMainPlot(solution);
    }

    public void clearSolutionList() {
        rootViewController.clearSolutionList();
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Current Error Bar Table setter and getter">
    public void setCurrentErrBarValues(HashMap<String, ErrorBarItem> errBarData) {
        this.errBarData = errBarData;
    }

    public HashMap<String, ErrorBarItem> getCurrentErrBarValues() {
        return errBarData;
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Solver Service Utility Routines">
    public void setupSolverService(
        EventHandler<WorkerStateEvent> successHandler,
        EventHandler<WorkerStateEvent> cancelledHandler,
        EventHandler<WorkerStateEvent> failedHandler,
        SolutionStats solutionStats,
        int[] dTranCandidates, int[] rTranCandidates,
        SqModel sqmodel,
        double sigmaB, double sigmaA,
        int n,
        double minMagDrop, double maxMagDrop,
        DoubleProperty progressProperty
    ) {
        // Build a one dimensional array of acceptable transition pairs. Two
        // parallel arrays will hold the validated pairs. The arrays are longer than needed,
        // but we'll keep a count of how much is valid for later use in chopping up
        // the work into equal amounts for each core.
        int nCores = Runtime.getRuntime().availableProcessors();
        int[] dTran = new int[dTranCandidates.length * rTranCandidates.length + nCores];
        int[] rTran = new int[dTranCandidates.length * rTranCandidates.length + nCores];
        int tranPairArraySize = 0;
        for (int i = 0; i < dTranCandidates.length; i++) {
            for (int j = 0; j < rTranCandidates.length; j++) {
                sqmodel
                    .setDtransition(dTranCandidates[i])
                    .setRtransition(rTranCandidates[j]);
                if (!sqmodel.validTransitionPair()) {
                    continue;
                } else {
                    dTran[tranPairArraySize] = dTranCandidates[i];
                    rTran[tranPairArraySize] = rTranCandidates[j];
                    tranPairArraySize++;
                }
            }
        }

        // The following code 'fudges' the number of trial pairs so that the
        // chunkSize will be an even multiple of the number of cores. We
        // pad with tran pairs that will be rejected without being processed
        // because R happens before D
        int numPaddingPairsNeeded = nCores - tranPairArraySize % nCores;
        for (int i = 0; i < numPaddingPairsNeeded; i++) {
            dTran[tranPairArraySize] = 2;
            rTran[tranPairArraySize] = 1;
            tranPairArraySize++;
        }

        // Split the 'work' array into nCore parts.
        int chunkSize = tranPairArraySize / nCores;
        int startIndex = 0;

        DoubleBinding progress = null;

        for (SolverService solService : multiCoreSolverService) {
            int[] dTranChunk = new int[chunkSize];
            System.arraycopy(dTran, startIndex, dTranChunk, 0, chunkSize);
            int[] rTranChunk = new int[chunkSize];
            System.arraycopy(rTran, startIndex, rTranChunk, 0, chunkSize);
            startIndex += chunkSize;

            SolutionStats solStatForThread = new SolutionStats();
            solStatForThread.straightLineAICc = solutionStats.straightLineAICc;
            solStatForThread.straightLineLogL = solutionStats.straightLineLogL;

            solService.setsolutionStats(solStatForThread);
            solService.setsqmodel(new SqModel(sqmodel.getObs()));
            solService.setsigmaB(sigmaB);
            solService.setsigmaA(sigmaA);
            solService.setn(n);
            solService.setminMagDrop(minMagDrop);
            solService.setmaxMagDrop(maxMagDrop);

            solService.setTranPairs(dTranChunk, rTranChunk, chunkSize);
            solService.setOnSucceeded(successHandler);
            solService.setOnCancelled(cancelledHandler);
            solService.setOnFailed(failedHandler);

            DoubleBinding scaledProgress = solService.progressProperty().divide(nCores);
            if (progress == null) {
                progress = scaledProgress;
            } else {
                progress = progress.add(scaledProgress);
            }

            solService.reset();
            solService.restart();
        }
        progressProperty.bind(progress);
    }

    public boolean solverServiceFinished() {
        for (SolverService solService : multiCoreSolverService) {
            if (solService.getState() != Worker.State.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    public boolean solverServiceRunning() {
        for (SolverService solService : multiCoreSolverService) {
            if (solService.getState() == Worker.State.RUNNING || solService.getState() == Worker.State.SCHEDULED) {
                return true;
            }
        }
        return false;
    }

    public void cancelSolverService() {
        for (SolverService solService : multiCoreSolverService) {
            solService.cancel();
        }
    }

    public List<SqSolution> getCumSolverSolutions() {
        List<SqSolution> sqsolutions = new ArrayList<>();

        for (SolverService solService : multiCoreSolverService) {
            sqsolutions.addAll(solService.getSolutionList());
        }

        return sqsolutions;
    }

    public SolutionStats getCumSolutionStats() {
        SolutionStats solutionStats = new SolutionStats();
        solutionStats.straightLineAICc = Double.NaN;
        solutionStats.straightLineLogL = Double.NaN;
        for (SolverService solService : multiCoreSolverService) {
            solutionStats.numTransitionPairsConsidered += solService.getSolutionStats().numTransitionPairsConsidered;
            solutionStats.numValidTransitionPairs += solService.getSolutionStats().numValidTransitionPairs;
            solutionStats.straightLineAICc = solService.getSolutionStats().straightLineAICc;
            solutionStats.straightLineLogL = solService.getSolutionStats().straightLineLogL;
        }
        return solutionStats;
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Solver Service">
    public class SolverService extends Service<Void> {

        // private variables requiring external initialization
        int[] dTran;
        int[] rTran;
        int numPairs;
        int n;
        SqModel sqmodel;
        double sigmaB;
        double sigmaA;
        double minMagDrop;
        double maxMagDrop;

        // return variables
        int numValidTranPairs;
        SolutionStats solutionStats; // Provided as a parameter.
        List<SqSolution> sqsolutions;

        // local variables
        double solMagDrop;

        // setters of private variables go here
        public final void setsolutionStats(SolutionStats solutionStats) {
            this.solutionStats = solutionStats;
        }

        public final void setTranPairs(int[] dTran, int[] rTran, int numPairs) {
            this.dTran = dTran;
            this.rTran = rTran;
            this.numPairs = numPairs;
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

        public final List<SqSolution> getSolutionList() {
            return sqsolutions;
        }

        public final SolutionStats getSolutionStats() {
            return solutionStats;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    sqsolutions = new ArrayList<>();
                    // work goes in here
                    numValidTranPairs = 0;
                    solutionStats.numTransitionPairsConsidered = numPairs;

                    int loopCount = 0;
                    int maxLoopCount = numPairs;

                    for (int i = 0; i < numPairs; i++) {
                        if (isCancelled()) {
                            break;
                        }
                        loopCount++;
                        updateProgress(loopCount, maxLoopCount);
                        SqSolution newSolution = new SqSolution();
                        newSolution.dTransitionIndex = dTran[i];
                        newSolution.rTransitionIndex = rTran[i];

                        sqmodel
                            .setDtransition(newSolution.dTransitionIndex)
                            .setRtransition(newSolution.rTransitionIndex);

                        // We let sqmodel determine when a dTran:rTran
                        // combination is valid.
                        if (!sqmodel.validTransitionPair()) {
                            continue;
                        }

                        try {
                            newSolution.logL = sqmodel
                                .calcLogL(sigmaB, sigmaA);
                        } catch (Exception e) {
                            System.out.println("Exception: " + e.getMessage());
                        }

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
                    solutionStats.numValidTransitionPairs = numValidTranPairs;
                    return null;
                }
            };
        }
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Error Bar Service Utility Routines">
    public void errBarServiceStart(boolean recalBandA,
                                   TrialParams trialParams,
                                   EventHandler<WorkerStateEvent> successHandler,
                                   EventHandler<WorkerStateEvent> cancelledHandler,
                                   EventHandler<WorkerStateEvent> failedHandler,
                                   DoubleProperty progressProperty,
                                   double[] timeCoeffs) {

        int totalTrials = trialParams.numTrials;
        int numCores = Runtime.getRuntime().availableProcessors();
        int numTrialsPerCore = totalTrials / numCores;

        DoubleBinding progress = null;

        for (ErrBarService ebs : multiCoreErrBarServices) {

            DoubleBinding scaledProgress = ebs.progressProperty().divide(numCores);
            if (progress == null) {
                progress = scaledProgress;
            } else {
                progress = progress.add(scaledProgress);
            }

            ebs.settrialParams(trialParams);
            ebs.setTimeCoeffArray(timeCoeffs);
            ebs.setRecalcBandA(recalBandA);
            ebs.settrialsPerCore(numTrialsPerCore);
            ebs.setOnSucceeded(successHandler);
            ebs.setOnCancelled(cancelledHandler);
            ebs.setOnFailed(failedHandler);
        }
        progressProperty.bind(progress);

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

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Error Bar Service">
    private class ErrBarService extends Service<Void> {

        private TrialParams trialParams;
        private MonteCarloResult ans;
        private int extraTrials = 0;
        private int trialsPerCore = 0;
        private boolean recalcBandA;
        private double[] timeCoeff = new double[10];

        public void settrialParams(TrialParams trialParams) {
            this.trialParams = trialParams;
        }
        
        public void setTimeCoeffArray(double[] coeffs) {
            for ( int i = 0; i < timeCoeff.length; i++ ) {
                timeCoeff[i] = coeffs[i];
            }
        }

        public void setExtraTrials(int extraTrials) {
            this.extraTrials = extraTrials;
        }

        public void settrialsPerCore(int trialsPerCore) {
            this.trialsPerCore = trialsPerCore;
        }

        public void setRecalcBandA(boolean recalcBandA) {
            this.recalcBandA = recalcBandA;
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
                    double[] noise = new double[trialParams.sampleWidth];
                    double[] rawNoise = new double[rdgs.length + timeCoeff.length];
                                     
                    int[] offset = new int[trialParams.sampleWidth];

                    double logLstraightLine = 0.0;
                    int rejectedSolutions = 0;

                    int centerIndex = trialParams.sampleWidth / 2;

                    for (int i = 0; i < histogram.length; i++) {
                        histogram[i] = 0;
                    }
                    
                    double sdCorr = 0.0;
                    
                    for ( int i = 0; i < timeCoeff.length; i++ ) {
                        sdCorr += timeCoeff[i] * timeCoeff[i];
                    }
                    sdCorr = Math.sqrt(sdCorr);

                    int trialNum = 0;

                    for (int k = 0; k < trialsPerCore + extraTrials; k++) {
                        if (isCancelled()) {
                            break;
                        }

                        trialNum++;
                        updateProgress(trialNum, trialsPerCore);

                        // Get our noise vector
                        
                        for (int i = 0; i < rawNoise.length; i++) {
                            rawNoise[i] = ThreadLocalRandom.current().nextGaussian();
                        }
                        
                        for (int i = 0; i < rdgs.length; i++) {
                            noise[i] = 0.0;
                            for(int j = 0; j < timeCoeff.length; j++) {
                                noise[i] += rawNoise[i+j] * timeCoeff[j];                            
                            }
                            noise[i] = noise[i] / sdCorr;
                        }
                        
                        // Make a new sample
                        for (int i = 0; i < centerIndex; i++) {
                            rdgs[i] = noise[i] * trialParams.sigmaA
                                + trialParams.eventLevel;
                        }

                        if (trialParams.mode == MonteCarloMode.LEFT_EDGE) {
                            rdgs[centerIndex] = noise[centerIndex] * trialParams.sigmaA
                                + trialParams.eventLevel;
                        } else if (trialParams.mode == MonteCarloMode.RIGHT_EDGE) {
                            rdgs[centerIndex] = noise[centerIndex] * trialParams.sigmaB
                                + trialParams.baselineLevel;
                        } else if (trialParams.mode == MonteCarloMode.MID_POINT) {
                            rdgs[centerIndex] = noise[centerIndex] * ((trialParams.sigmaA + trialParams.sigmaB) / 2.0)
                                + (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
                        } else if (trialParams.mode == MonteCarloMode.RANDOM) {
                            double frac = ThreadLocalRandom.current().nextDouble();
                            double level = trialParams.baselineLevel * (1.0 - frac) + trialParams.eventLevel * frac;
                            double noiseAtLevel = trialParams.sigmaB * (1.0 - frac) + trialParams.sigmaA * frac;
                            rdgs[centerIndex] = noise[centerIndex] * noiseAtLevel + level;
                        } else {
                            throw new InternalError("Design error: MonteCarlo mode not implemented.");
                        }

                        for (int i = centerIndex + 1; i < rdgs.length; i++) {
                            rdgs[i] = noise[i] * trialParams.sigmaB
                                + trialParams.baselineLevel;
                        }

                        double A;
                        double B;
                        if (recalcBandA) {
                            double sum;
                            sum = 0.0;
                            for (int i = 0; i < centerIndex; i++) {
                                sum += rdgs[i];
                            }
                            A = sum / centerIndex;
                            sum = 0.0;
                            for (int i = centerIndex + 1; i < rdgs.length; i++) {
                                sum += rdgs[i];
                            }
                            B = sum / (rdgs.length - centerIndex);
                        } else {
                            B = trialParams.baselineLevel;
                            A = trialParams.eventLevel;
                        }

                        // The test case has now been created.  Calculate the various arrays that depend on this case.
                        double midLevel = (B + A) / 2.0;
                        double midSigma = (trialParams.sigmaA + trialParams.sigmaB) / 2.0;

                        logLstraightLine = 0.0;
                        for (int i = 0; i < trialParams.sampleWidth; i++) {
                            logLB[i] = logL(rdgs[i], B, trialParams.sigmaB);
                            logLA[i] = logL(rdgs[i], A, trialParams.sigmaA);
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

    //</editor-fold>
}
