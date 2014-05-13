package joculartest;

import jocularmain.SqModel;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.offset;
import org.junit.Before;
import org.junit.Test;
import utils.Observation;
import static utils.RandUtils.setGaussianGeneratorSeed;
import utils.SampleDataGenerator;

public class SqModelTest {

    public SqModelTest() {
    }

    Observation specialObservation;

    @Before
    public void setUp() {
        SampleDataGenerator dataGen = new SampleDataGenerator("provides known fixed data points");

        dataGen.setDevent(1.5)
            .setRevent(4.5)
            .setSigmaA(0.0)
            .setSigmaB(0.0)
            .setAintensity(2.0)
            .setBintensity(10.0)
            .setNumDataPoints(8)
            .setParams();

        specialObservation = dataGen.build();

        specialObservation.obsData[0] = 11.0;
        specialObservation.obsData[1] = 9.0;
        specialObservation.obsData[2] = 6.0;
        specialObservation.obsData[3] = 1.0;
        specialObservation.obsData[4] = 3.0;
        specialObservation.obsData[5] = 6.0;
        specialObservation.obsData[6] = 11.0;
        specialObservation.obsData[7] = 9.0;
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDandRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(4);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDonEdgeRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(0).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDoutofrangeRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(-2000).setRtransition(5).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRonedge() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(7).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRoutofrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(2000).calcLogL();

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCalculateSigmaAandB() {
        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL();

        // This test is only possible because of the use of 'canned'
        // known data values in specialObservation.
        
        assertThat(sqmodel.getSigmaB()).isEqualTo(Math.sqrt(4.0/3));
        assertThat(sqmodel.getSigmaA()).isEqualTo(Math.sqrt(2.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqmodel_shouldThrowException_whenBothTransitionsAreOutOfRange() {
        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(-2).setRtransition(50).calcLogL();
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectResult() {
    
        specialObservation.obsData[2] = 5.0;
        specialObservation.obsData[5] = -18.5;
        SqModel sqmodel = new SqModel( specialObservation );
        
        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL();
        
        System.out.println("dSolution=" + sqmodel.getDsolution());
        System.out.println("rSolution=" + sqmodel.getRsolution());
        //assertThat(logLcalculated).isEqualTo(-12345.6);
    }
}
