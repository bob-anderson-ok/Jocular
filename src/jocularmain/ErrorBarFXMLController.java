package jocularmain;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

public class ErrorBarFXMLController implements Initializable {
    
    @FXML
    public void calculateDistribution() {
        System.out.println("calculateDistribution clicked"); 
    }
    
    @FXML
    public void writePanelToFile() {
        System.out.println("writePanelToFile clicked");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Don't need to intialize anything here.
    }

}
