package joculartest;

import jocularmain.SqModel;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Test;
import utils.Observation;
import utils.SampleDataGenerator;

public class SqModelTest {
    
    public SqModelTest() {
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectResult() {
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetTwo");

        dataGen.setDevent(1.5)
                .setRevent(4.5)
                .setSigmaA(1.0)
                .setSigmaB(1.0)
                .setAintensity(1.0)
                .setBintensity(11.0)
                .setNumDataPoints(8)
                .setParams();

        Observation specialObservation = dataGen.build();
        SqModel sqmodel = new SqModel( specialObservation );
        
        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL();
        
        assertThat(logLcalculated).isEqualTo(-12345.6);
    }

}
