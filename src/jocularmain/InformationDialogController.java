package jocularmain;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class InformationDialogController {

    @FXML
    private TextArea informationText;

    @FXML
    public void showInformation(String msg) {
        informationText.setText(msg);
    }

}

