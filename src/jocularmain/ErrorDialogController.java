package jocularmain;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ErrorDialogController {

    private static JocularMain mainApp;

    public static void setMainApp(JocularMain main) {
        mainApp = main;
    }
    
    @FXML
    private TextArea errorText;
    
    public void showError(String msg) {
        mainApp.buildErrorDialog();
        errorText.setText(msg);
    }

}
