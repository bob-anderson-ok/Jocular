package jocularmain;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ErrorDialogController {
    
    @FXML
    private TextArea errorText;
    
    public void showError(String msg) {
        errorText.setText(msg);
    }

}
