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
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDandRinrange() {
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
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(4);
    }
    
    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDonEdgeRinrange() {
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
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(0).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }
    
    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDoutofrangeRinrange() {
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
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(-2000).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }
    
    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRonedge() {
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
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(7).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }
    
    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRoutofrange() {
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
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(2000).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

//    @Test
//    public void sqmodelCalcLogL_shouldGiveCorrectResult() {
//        SampleDataGenerator dataGen = new SampleDataGenerator("testSetTwo");
//
//        dataGen.setDevent(1.5)
//                .setRevent(4.5)
//                .setSigmaA(1.0)
//                .setSigmaB(1.0)
//                .setAintensity(1.0)
//                .setBintensity(11.0)
//                .setNumDataPoints(8)
//                .setParams();
//
//        Observation specialObservation = dataGen.build();
//        SqModel sqmodel = new SqModel( specialObservation );
//        
//        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL();
//        
//        assertThat(logLcalculated).isEqualTo(-12345.6);
//    }
}
