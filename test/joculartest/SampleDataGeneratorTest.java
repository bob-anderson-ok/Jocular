package joculartest;

import org.junit.Test;
import utils.Observation;
import static org.fest.assertions.api.Assertions.assertThat;
import utils.SampleDataGenerator;

public class SampleDataGeneratorTest {

    public SampleDataGeneratorTest() {
    }

    final double SOME_D_EVENT_VALUE = 43.25;
    final double SOME_R_EVENT_VALUE = 297.75;
    final double SOME_A_VALUE = 2.0;
    final double SOME_B_VALUE = 12.0;
    final double SOME_SIGMAA_VALUE = 1.0;
    final double SOME_SIGMAB_VALUE = 2.0;
    final int SOME_DATA_LENGTH = 1254;

    @Test
    public void setParams_shouldFillObservationCorrectly() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetOne");

        Observation trialObs = dataGen
                .setDevent(SOME_D_EVENT_VALUE)
                .setRevent(SOME_R_EVENT_VALUE)
                .setSigmaA(SOME_SIGMAA_VALUE)
                .setSigmaB(SOME_SIGMAB_VALUE)
                .setAintensity(SOME_A_VALUE)
                .setBintensity(SOME_B_VALUE)
                .setNumDataPoints(SOME_DATA_LENGTH)
                .setParams();

        assertThat(trialObs.getfilePath()).isEqualTo("../internally-generated-data");
        assertThat(trialObs.getfileName()).isEqualTo("internally-generated-data");
        assertThat(trialObs.numberOfDataColumns).isEqualTo(3);
        assertThat(trialObs.lengthOfDataColumns).isEqualTo(SOME_DATA_LENGTH);
        assertThat(trialObs.obsData.length).isEqualTo(SOME_DATA_LENGTH);
        assertThat(trialObs.secData.length).isEqualTo(SOME_DATA_LENGTH);
        assertThat(trialObs.getobsDataColumn()).isEqualTo(0);
        assertThat(trialObs.getsecDataColumn()).isEqualTo(1);
        assertThat(trialObs.addedInfo.size()).isEqualTo(0);

        //System.out.println(trialObs);
    }

    @Test
    public void build_shouldFillDataArraysCorrectly() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetTwo");

        dataGen
                .setDevent(SOME_D_EVENT_VALUE)
                .setRevent(SOME_R_EVENT_VALUE)
                .setSigmaA(SOME_SIGMAA_VALUE)
                .setSigmaB(SOME_SIGMAB_VALUE)
                .setAintensity(SOME_A_VALUE)
                .setBintensity(SOME_B_VALUE)
                .setNumDataPoints(SOME_DATA_LENGTH)
                .setParams();

        Observation trialObs = dataGen.build();

        //System.out.println(trialObs);
    }

    @Test(expected = IllegalStateException.class)
    public void build_shouldThrowExceptionWhenNeededValuesNotPresent() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetThree");

        dataGen
                .setDevent(SOME_D_EVENT_VALUE)
                .setRevent(SOME_R_EVENT_VALUE)
                //.setSigmaA(SOME_SIGMAA_VALUE)
                .setSigmaB(SOME_SIGMAB_VALUE)
                .setAintensity(SOME_A_VALUE)
                .setBintensity(SOME_B_VALUE)
                .setNumDataPoints(SOME_DATA_LENGTH)
                .setParams();

        dataGen.build();
    }

    @Test
    public void observation_shouldFillObsData_whenObsDataColumnIsChanged() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetFour");

        dataGen
            .setDevent(SOME_D_EVENT_VALUE)
            .setRevent(SOME_R_EVENT_VALUE)
            .setSigmaA(SOME_SIGMAA_VALUE)
            .setSigmaB(SOME_SIGMAB_VALUE)
            .setAintensity(SOME_A_VALUE)
            .setBintensity(SOME_B_VALUE)
            .setNumDataPoints(SOME_DATA_LENGTH)
            .setParams();

        Observation trialObs = dataGen.build();

        int currentColumn = trialObs.getobsDataColumn();
        trialObs.setobsDataColumn(currentColumn+1);
        
        assertThat(trialObs.columnData[currentColumn+1][0]).isEqualTo(trialObs.obsData[0]);

        //System.out.println(trialObs);
    }
    
    @Test
    public void valuesSurroundingDandRTransitionPoints_shouldBeCorrect() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetFive");

        dataGen
            .setDevent(39.25)
            .setRevent(139.75)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(12.0)
            .setNumDataPoints(200)
            .setParams();

        Observation trialObs = dataGen.build();
        
        assertThat(trialObs.obsData[39]).isEqualTo(12.0);
        assertThat(trialObs.obsData[40]).isEqualTo(4.5);
        assertThat(trialObs.obsData[41]).isEqualTo(2.0);
        
        assertThat(trialObs.obsData[139]).isEqualTo(2.0);
        assertThat(trialObs.obsData[140]).isEqualTo(4.5);
        assertThat(trialObs.obsData[141]).isEqualTo(12.0);
    }
    
    @Test
    public void valuesSurroundingOnlyDTransition_shouldBeCorrect() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetSix");

        dataGen
            .setDevent(39.25)
            //.setRevent(139.75)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(12.0)
            .setNumDataPoints(200)
            .setParams();

        Observation trialObs = dataGen.build();
        
        assertThat(trialObs.obsData[39]).isEqualTo(12.0);
        assertThat(trialObs.obsData[40]).isEqualTo(4.5);
        assertThat(trialObs.obsData[41]).isEqualTo(2.0);
        
        assertThat(trialObs.obsData[0]).isEqualTo(12.0);
        assertThat(trialObs.obsData[199]).isEqualTo(2.0);
    }
    
    @Test
    public void valuesSurroundingValidNegativeDTransition_shouldBeCorrect() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetSeven");

        dataGen
            .setDevent(2.0)
            .setRevent(8.0)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(12.0)
            .setNumDataPoints(15)
            .setParams();

        Observation trialObs = dataGen.build();
        
        assertThat(trialObs.obsData[0]).isEqualTo(12.0);
        assertThat(trialObs.obsData[2]).isEqualTo(12.0);
        assertThat(trialObs.obsData[3]).isEqualTo(2.0);
        
        assertThat(trialObs.obsData[8]).isEqualTo(2.0);
        assertThat(trialObs.obsData[9]).isEqualTo(12.0);
        assertThat(trialObs.obsData[14]).isEqualTo(12.0);
    }
    
    @Test
    public void valuesSurroundingOnlyRTransition_shouldBeCorrect() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetThree");

        dataGen
            .setDevent(-39.25)  // NaN also works
            .setRevent(139.75)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(12.0)
            .setNumDataPoints(200)
            .setParams();

        Observation trialObs = dataGen.build();
        
        assertThat(trialObs.obsData[139]).isEqualTo(2.0);
        assertThat(trialObs.obsData[140]).isEqualTo(4.5);
        assertThat(trialObs.obsData[141]).isEqualTo(12.0);
        
        assertThat(trialObs.obsData[0]).isEqualTo(2.0);
        assertThat(trialObs.obsData[199]).isEqualTo(12.0);
    }
    
    @Test
    public void valuesSurroundingIntegerEventTimes_shouldBeCorrect() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetThree");

        dataGen
            .setDevent(-39.25)  // NaN also works
            .setRevent(139.75)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(12.0)
            .setNumDataPoints(200)
            .setParams();

        Observation trialObs = dataGen.build();
        
        assertThat(trialObs.obsData[139]).isEqualTo(2.0);
        assertThat(trialObs.obsData[140]).isEqualTo(4.5);
        assertThat(trialObs.obsData[141]).isEqualTo(12.0);
        
        assertThat(trialObs.obsData[0]).isEqualTo(2.0);
        assertThat(trialObs.obsData[199]).isEqualTo(12.0);
    }

}
