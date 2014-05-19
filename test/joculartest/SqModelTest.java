package joculartest;

import jocularmain.SqModel;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.offset;
import org.junit.Before;
import org.junit.Test;
import utils.Observation;
import utils.SampleDataGenerator;

public class SqModelTest {

    public SqModelTest() {
    }

    Observation specialObservation;
    double sigmaB;
    double sigmaA;

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

        // We copy the obsData to columnData so that 'trim' operations
        // will work properly.
        for (int i = 0; i < specialObservation.obsData.length; i++) {
            specialObservation.columnData[0][i] = specialObservation.obsData[i];
        }

        sigmaB = 2.0;
        sigmaA = 1.0;
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDandRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(4);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDandRinrangeAndTrimmedLeft() {

        specialObservation.setLeftTrimPoint(1);
        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(3);
    }

    @Test
    public void sqmodel_shouldCalculateBandA_whenDandRinrangeAndTrimmedLeft() {

        specialObservation.setLeftTrimPoint(1);
        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(3);
        assertThat(sqmodel.getB()).isEqualTo(29.0 / 3);
        assertThat(sqmodel.getA()).isEqualTo(2.0);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDandRinrangeAndTrimmedRight() {

        specialObservation.setRightTrimPoint(6);
        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(2);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(3);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDonEdgeRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(0).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDoutofrangeRinrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(-2000).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRonedge() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(7).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(4);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldCountEventAndBaselinePoints_whenDinrangeRoutofrange() {

        SqModel sqmodel = new SqModel(specialObservation);

        sqmodel.setDtransition(2).setRtransition(2000).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getNumEventPoints()).isEqualTo(5);
        assertThat(sqmodel.getNumBaselinePoints()).isEqualTo(2);
    }

    @Test
    public void sqmodel_shouldRetrunNaN_whenBothTransitionsAreOutOfRange() {
        SqModel sqmodel = new SqModel(specialObservation);

        double logL = sqmodel.setDtransition(-2).setRtransition(50).calcLogL(sigmaB, sigmaA);

        assertThat(logL).isEqualTo(Double.NaN);
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectSubframeResults() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        System.out.println("dSolution=" + sqmodel.getDsolution());
        System.out.println("rSolution=" + sqmodel.getRsolution());
        System.out.println("logLcalculated=" + logLcalculated);

        assertThat(logLcalculated).isEqualTo(-12.595112619440567, offset(1e-7));
        assertThat(sqmodel.getDsolution()).isEqualTo(1.625);
        assertThat(sqmodel.getRsolution()).isEqualTo(4.375);
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectSubframeResults_whenTrimmed() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        specialObservation.setLeftTrimPoint(1);
        specialObservation.setRightTrimPoint(6);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        System.out.println("dSolution=" + sqmodel.getDsolution());
        System.out.println("rSolution=" + sqmodel.getRsolution());
        System.out.println("logLcalculated=" + logLcalculated);

        assertThat(logLcalculated).isEqualTo(-8.960855776564255, offset(1e-7));
        assertThat(sqmodel.getDsolution()).isEqualTo(1.5);
        assertThat(sqmodel.getRsolution()).isEqualTo(4.5);
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectIntegerResults_givenMcloseToB() {

        specialObservation.obsData[2] = 9.0;
        specialObservation.obsData[5] = 9.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

//        System.out.println("dSolution=" + sqmodel.getDsolution());
//        System.out.println("rSolution=" + sqmodel.getRsolution());
//        System.out.println("logLcalculated=" + logLcalculated);
        assertThat(logLcalculated).isEqualTo(-13.260391348997054, offset(1e-7));
        assertThat(sqmodel.getDsolution()).isEqualTo(2.0);
        assertThat(sqmodel.getRsolution()).isEqualTo(4.0);
    }

    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectIntegerResults_givenMcloseToA() {

        specialObservation.obsData[2] = 3.0;
        specialObservation.obsData[5] = 3.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

//        System.out.println("dSolution=" + sqmodel.getDsolution());
//        System.out.println("rSolution=" + sqmodel.getRsolution());
//        System.out.println("logLcalculated=" + logLcalculated);
        assertThat(logLcalculated).isEqualTo(-12.624096987877163, offset(1e-7));
        assertThat(sqmodel.getDsolution()).isEqualTo(1.0);
        assertThat(sqmodel.getRsolution()).isEqualTo(5.0);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenMcloseToA() {

        specialObservation.obsData[2] = 3.0;
        specialObservation.obsData[5] = 3.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(4);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenMcloseToB() {

        specialObservation.obsData[2] = 9.0;
        specialObservation.obsData[5] = 9.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(4);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenDinSubFrameArea() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 9.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(5);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenRinSubFrameArea() {

        specialObservation.obsData[2] = 9.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(5);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenDandRinSubFrameArea() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(6);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenNoDandRinSubFrameArea() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(-1).setRtransition(5).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(3);
    }
    
    @Test
    public void sqmodelCalcLogL_shouldGiveCorrectkFactor_givenNoRandDinSubFrameArea() {

        specialObservation.obsData[2] = 7.0;
        specialObservation.obsData[5] = 7.0;
        SqModel sqmodel = new SqModel(specialObservation);

        double logLcalculated = sqmodel.setDtransition(2).setRtransition(15).calcLogL(sigmaB, sigmaA);

        assertThat(sqmodel.getkFactor()).isEqualTo(3);
    }
}
