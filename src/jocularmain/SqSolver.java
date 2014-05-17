package jocularmain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import utils.SqSolution;

public class SqSolver {

    public static int[] dTranCandidates;  // made public to simplify testing
    public static int[] rTranCandidates;  // made public to simplify testing

    /* dLeft, dRight, rLeft, and rRight are made public to make it easier to
     * test the class.
     */
    
    //private Observation obs;
    /**
     * is the left limit (inclusive) of candidate d transition indices.
     * <p>
     * It can be negative to indicate that the leftmost d transition is outside of the observation data range.
     */
    public static int dLeft;

    /**
     * is the right limit (inclusive) of candidate d transition indices.
     * <p>
     * It must be less than rLeft and >= dLeft
     * <p>
     * It can be negative to indicate that there is no candidate d transition in the observation data range.
     */
    public static int dRight;

    /**
     * is the left limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than dRight.
     * <p>
     * It can be outside the observation data range (> data length) to indicate that there is no candidate r transition in the observation.
     */
    public static int rLeft;

    /**
     * is the right limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than rLeft.
     * <p>
     * It can be outside the observation data range (> data length) to indicate that there is no righthand r transition in the observation.
     *
     */
    public static int rRight;

    static public List<SqSolution> computeCandidates(JocularMain jocularMain,
                                         XYChartMarker dLeftMarker,
                                         XYChartMarker dRightMarker,
                                         XYChartMarker rLeftMarker,
                                         XYChartMarker rRightMarker) {
        if (dLeftMarker.isInUse()) {
            dLeft = (int) Math.ceil(dLeftMarker.getXValue());
        } else {
            dLeft = jocularMain.getOutOfRangeOfObsOnTheLeft();
        }

        if (dRightMarker.isInUse()) {
            dRight = (int) Math.floor(dRightMarker.getXValue());
        } else {
            dRight = jocularMain.getOutOfRangeOfObsOnTheLeft();
        }

        if (rLeftMarker.isInUse()) {
            rLeft = (int) Math.ceil(rLeftMarker.getXValue());
        } else {
            rLeft = jocularMain.getOutOfRangeOfObsOnTheRight();
        }

        if (rRightMarker.isInUse()) {
            rRight = (int) Math.floor(rRightMarker.getXValue());
        } else {
            rRight = jocularMain.getOutOfRangeOfObsOnTheRight();
        }

        if (dRight < dLeft) {
            String errMsg = "D limits reversed: dLeft=" + dLeft + "  dRight=" + dRight;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (rRight < rLeft) {
            String errMsg = "R limits reversed: rLeft=" + rLeft + "  rRight=" + rRight;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (dRight >= rLeft) {
            String errMsg = "R limits overlap D limits: dRight=" + dRight + "  rLeft=" + rLeft;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        System.out.println("dLeft: " + dLeft + "  dRight: " + dRight
            + "  rLeft: " + rLeft + "  rRight: " + rRight);

        dTranCandidates = new int[dRight - dLeft + 1];
        rTranCandidates = new int[rRight - rLeft + 1];

        for (int i = dLeft; i <= dRight; i++) {
            dTranCandidates[i - dLeft] = i;
        }

        for (int i = rLeft; i <= rRight; i++) {
            rTranCandidates[i - rLeft] = i;
        }
        
        List<SqSolution> sqsolutions = new ArrayList<>();
        
        SqModel sqmodel = new SqModel(jocularMain.getCurrentObservation());
        
        for (int i = 0; i < dTranCandidates.length; i++) {
            for (int j=0; j<rTranCandidates.length;j++) {
                
                SqSolution newSolution = new SqSolution();
                newSolution.dTransitionIndex = dTranCandidates[i];
                newSolution.rTransitionIndex = rTranCandidates[j];
                
                newSolution.logL = sqmodel
                    .setDtransition(newSolution.dTransitionIndex)
                    .setRtransition(newSolution.rTransitionIndex)
                    .calcLogL();
                
                newSolution.D = sqmodel.getDsolution();
                newSolution.R = sqmodel.getRsolution();
                newSolution.B = sqmodel.getB();
                newSolution.A = sqmodel.getA();
                newSolution.sigmaB = sqmodel.getSigmaB();
                newSolution.sigmaA = sqmodel.getSigmaA();
                
                sqsolutions.add(newSolution);
            }
        }
        
        LogLcomparator logLcomparator = new LogLcomparator();
        Collections.sort(sqsolutions, logLcomparator);
        jocularMain.addSolutionPlotToCurrentObs(sqsolutions.get(0));
        return sqsolutions;     
    }
}

class LogLcomparator implements Comparator<SqSolution> {
    @Override
    public int compare(SqSolution one, SqSolution two) {
        // sort is largest to smallest
        return Double.compare(two.logL, one.logL);
    }
    
}
