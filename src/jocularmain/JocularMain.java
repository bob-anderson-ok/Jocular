package jocularmain;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
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
import utils.JocularUtils;
import utils.Observation;
import utils.SqSolution;

/**
 *
 * @author Bob Anderson
 */
public class JocularMain extends Application {

    private Observation obsInMainPlot = null;
    private SqSolution currentSqSolution = null;

    private RootViewController rootViewController;
    private Stage sampleDataDialogStage;
    private ArrayList<Stage> openHelpScreenList = new ArrayList<>();

    public Scene mainScene;
    public Stage errorBarPanelStage;
    public Scene errorBarPanelScene;
    public Stage primaryStage;
    
    public SolverService solverService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        solverService = new SolverService();
        
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
    }

    public SqSolution getCurrentSolution() {
        return currentSqSolution;
    }

    public void setCurrentSolution(SqSolution newSolution) {
        currentSqSolution = newSolution;
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
    // setters of private variable go here
    public final void setdTranCandidates(int[] dTran) {
        dTranCandidates = dTran;
    }
    public final void setrTranCandidates(int[] rTran) {
        rTranCandidates = rTran;
    }
    public final void setsqmodel( SqModel sqmodel) {
        this.sqmodel = sqmodel;
    }
    public final void setsigmaB(double sigmaB) {
        this.sigmaB = sigmaB;
    }
    public final void setsigmaA(double sigmaA){
        this.sigmaA= sigmaA;
    }
    public final void setn( int n) {
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

    @Override
    protected Task<List<SqSolution>> createTask() {
        return new Task<List<SqSolution>>() {
            @Override
            protected List<SqSolution> call() {
                List<SqSolution> sqsolutions = new ArrayList<>();
                // work goes in here
                numValidTranPairs = 0;
                for (int i = 0; i < dTranCandidates.length; i++) {
                    for (int j = 0; j < rTranCandidates.length; j++) {
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


}
