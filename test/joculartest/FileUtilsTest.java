package joculartest;

import jocularmain.FileType;
import org.junit.Test;
import utils.FileUtils;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;

public class FileUtilsTest {
    
    private FileUtils obsFileReader;
    
    public FileUtilsTest() {
    }

    @Before
    public void setUp(){
        obsFileReader = FileUtils.getInstance();
    }
    
    @After
    public void tidyUp() {
        obsFileReader.closeObservationFile();
    }
    
    @Test
    public void openObservationFile_shouldReturnTrue_whenFileCanBeOpened() {
        String filePath = "/Users/bob/NetBeansProjects/Jocular/test/joculartest/LimovieTestFile.csv";
        boolean fileOpened = obsFileReader.openObservationFile( filePath );
        assertThat(fileOpened).isEqualTo(true);
    }
    
    @Test
    public void getFileType_shouldReturnLimovie_whenLimovieFileIsInUse() {
        String filePath = "/Users/bob/NetBeansProjects/Jocular/test/joculartest/LimovieTestFile.csv";
        boolean fileOpened = obsFileReader.openObservationFile( filePath );
        assertThat(fileOpened).isEqualTo(true);
        
        FileType fileType = obsFileReader.getFileType();
        assertThat(fileType).isEqualTo(FileType.LIMOVIE);
    }
    
}
