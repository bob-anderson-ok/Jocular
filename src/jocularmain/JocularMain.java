package jocularmain;

import java.net.URL;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
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

    public RootViewController rootViewControllerInstance;
    public ErrorDialogController errorDialogControllerInstance;
    public Observation obsInMainPlot;
    public SqSolution  currentSqSolution;
    
    private Stage errorDialogStage;
    private Stage sampleDataDialogStage;
    private Stage primaryStage;
    private ArrayList<Stage> openHelpScreenList = new ArrayList<>();
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // We save this reference because some other dialogs that we will be
        // creating need to be able to provide a 'parent'
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

        // Publish a reference to RootViewController
        rootViewControllerInstance = fxmlLoader.getController();
        
        scene.getStylesheets().add(this.getClass().getResource("JocularStyleSheet.css").toExternalForm());

        primaryStage.titleProperty().set("Jocular 0.1");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Give view controllers a reference to JocularMain so that they
        // can invoke methods provided by this class. We give all view
        // controllers a static method that we can call to provide the
        // reference to JocularMain.
        RootViewController.setMainApp(this);
        SampleDataDialogController.setMainApp(this);
        ErrorDialogController.setMainApp(this);
        
        // Create the dialogs that will be used.  This initial call does
        // not cause them to be 'shown'.  This call is made because of an
        // often needed side effect: the publishing of a reference to the
        // dialog's controller instance (created by fxmlLoader()).
        showErrorDialog();
        showSampleDataDialog();
    }

    private void closeAllRemainingHelpScreens(WindowEvent e) {
        for (Stage s : openHelpScreenList) {
            s.close();
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
      
    public void showErrorDialog() {
        if (errorDialogStage == null) {
            try {
                URL fxmlLocation = getClass().getResource("ErrorDialog.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
                AnchorPane page = fxmlLoader.load();
                Scene scene = new Scene(page);
                
                // Publish a reference to the view controller.
                errorDialogControllerInstance = fxmlLoader.getController();
                

                errorDialogStage = new Stage(StageStyle.UTILITY);
                errorDialogStage.initModality(Modality.APPLICATION_MODAL);
                errorDialogStage.setResizable(false);

                errorDialogStage.initOwner(primaryStage);
                errorDialogStage.setScene(scene);

                // We do not 'show' the dialog on the initial build, which was
                // triggered through the main start() method.
                
            } catch (Exception e) {
                System.out.println("in buildErrorDialog(): " + e.toString());
            }
        } else {
            errorDialogStage.show();
        }
    }

    public void showSampleDataDialog() {
        if (sampleDataDialogStage == null) {
            try {
                URL fxmlLocation = getClass().getResource("SampleDataDialog.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
                AnchorPane page = fxmlLoader.load();
                Scene scene = new Scene(page);

                sampleDataDialogStage = new Stage(StageStyle.UTILITY);
                sampleDataDialogStage.initModality(Modality.NONE);
                sampleDataDialogStage.setResizable(false);

                sampleDataDialogStage.initOwner(primaryStage);
                sampleDataDialogStage.setScene(scene);

                // We do not 'show' the dialog on the initial build, which was
                // triggered through the main start() method.
                
            } catch (Exception e) {
                System.out.println("in buildSampleDataDialog(): " + e.toString());
            }
        } else {
            sampleDataDialogStage.show();
        }
    }
}
