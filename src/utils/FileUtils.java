package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import jocularmain.FileType;
import jocularmain.JocularMain;

public class FileUtils {

    private File obsFile = null;
    private BufferedReader obsReader = null;
    private ArrayList<String> lineArray = new ArrayList<>();
    private JocularMain jocularMain;

    private FileUtils() {
    }

    public static FileUtils getInstance() {
        return FileUtilsHolder.INSTANCE;
    }

    public void setMainApp(JocularMain main) {
        jocularMain = main;
    }

    private static class FileUtilsHolder {
        private static final FileUtils INSTANCE = new FileUtils();
    }

    public boolean openObservationFile(String filePath) {
        try {
            closeObservationFile();
            obsFile = new File(filePath);
            obsReader = new BufferedReader(new FileReader(obsFile));
            lineArray.clear();
            String line;
            while ((line = obsReader.readLine()) != null) {
                lineArray.add(line);
            }
        } catch (FileNotFoundException e) {
            obsFile = null;
            obsReader = null;
            if (jocularMain != null) {
                jocularMain.showInformationDialog(e.getMessage(), jocularMain.primaryStage);
            }
            return false;
        } catch (IOException e) {
            if (jocularMain != null) {
                jocularMain.showErrorDialog(e.getMessage(), jocularMain.primaryStage);
            }
            return false;
        }
        return true;
    }

    public boolean closeObservationFile() {
        if (obsReader == null) {
            return false;
        } else {
            try {
                obsReader.close();
            } catch (IOException e) {
                return false;
            }
            obsFile = null;
            obsReader = null;
            lineArray.clear();
            return true;
        }
    }

    public FileType getFileType() {
        // Check for Limovie in header

        return FileType.UNSUPPORTED;
    }
}
