package jocularmain;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import utils.Observation;
import utils.SqSolution;

/**
 *
 * @author Bob Anderson
 */
public class JocularMain extends Application {

    private Observation obsInMainPlot=null;
    private SqSolution currentSqSolution=null;

    private RootViewController rootViewController;
    private Stage sampleDataDialogStage;
    private Stage primaryStage;
    private ArrayList<Stage> openHelpScreenList = new ArrayList<>();

    public Scene mainScene;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

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

        primaryStage.titleProperty().set("Jocular 0.51");
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
            
            Stage errorBarStage = new Stage();
            
            errorBarStage.setTitle("Error Bar Exploration Tool");
            errorBarStage.initModality(Modality.NONE);
            errorBarStage.setResizable(true);
            errorBarStage.setScene(scene);
            errorBarStage.show();
        } catch(Exception e) {
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

    public void showErrorDialog(String msg) {
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

            stage.initOwner(primaryStage);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            System.out.println("in showErrorDialog(): " + e.toString());
        }
    }
    
    public void showInformationDialog(String msg) {
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

            stage.initOwner(primaryStage);
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
    
    public void repaintObservationAndSolution(){
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
     * @return
     * the first readingNumber to the right of the trimmed observation.
     */
    public int getOutOfRangeOfObsOnTheRight() {
        return obsInMainPlot.readingNumbers[obsInMainPlot.readingNumbers.length-1] + 1;
    }
    
    /**
     * 
     * @return 
     * the first readingNumber to the left of the trimmed observation.
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
    
    public void setCurrentSolution( SqSolution newSolution) {
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
}
