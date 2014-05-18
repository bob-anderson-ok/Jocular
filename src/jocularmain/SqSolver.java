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

        int leftLimit = jocularMain.getOutOfRangeOfObsOnTheLeft();
        int rightLimit = jocularMain.getOutOfRangeOfObsOnTheRight();

        if (dLeftMarker.isInUse() != dRightMarker.isInUse()) {
            String errMsg = "D marker usage must be paired";
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        if (rLeftMarker.isInUse() != rRightMarker.isInUse()) {
            String errMsg = "R marker usage must be paired";
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (dLeftMarker.isInUse()) {
            dLeft = (int) Math.ceil(dLeftMarker.getXValue());
            dLeft = Math.max(dLeft, leftLimit);
        } else {
            dLeft = leftLimit;
        }

        if (dRightMarker.isInUse()) {
            dRight = (int) Math.floor(dRightMarker.getXValue());
            dRight = Math.max(dRight, leftLimit);
        } else {
            dRight = leftLimit;
        }

        if (rLeftMarker.isInUse()) {
            rLeft = (int) Math.ceil(rLeftMarker.getXValue());
            rLeft = Math.min(rLeft, rightLimit);
        } else {
            rLeft = rightLimit;
        }

        if (rRightMarker.isInUse()) {
            rRight = (int) Math.floor(rRightMarker.getXValue());
            rRight = Math.min(rRight, rightLimit);
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
        
        if ( Math.max(rLeft,rRight) < Math.min(dLeft, dRight)) {
            String errMsg = "Invalid marker settings: reappearance is before disappearance";
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

//        if (dRight >= rLeft) {
//            String errMsg = "R limits overlap D limits: dRight=" + dRight + "  rLeft=" + rLeft;
//            jocularMain.showErrorDialog(errMsg);
//            throw new IllegalArgumentException(errMsg);
//        }
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
            for (int j = 0; j < rTranCandidates.length; j++) {

                //if (validEvent(dTranCandidates[i], rTranCandidates[j], leftLimit, rightLimit)) {
                SqSolution newSolution = new SqSolution();
                newSolution.dTransitionIndex = dTranCandidates[i];
                newSolution.rTransitionIndex = rTranCandidates[j];

                newSolution.logL = sqmodel
                    .setDtransition(newSolution.dTransitionIndex)
                    .setRtransition(newSolution.rTransitionIndex)
                    .calcLogL();

                if (Double.isNaN(newSolution.logL)) {
                    continue;
                }

                newSolution.D = sqmodel.getDsolution();
                newSolution.R = sqmodel.getRsolution();
                newSolution.B = sqmodel.getB();
                newSolution.A = sqmodel.getA();
                newSolution.sigmaB = sqmodel.getSigmaB();
                newSolution.sigmaA = sqmodel.getSigmaA();

                sqsolutions.add(newSolution);
                //}
            }
        }

        if (sqsolutions.size() > 0) {
            LogLcomparator logLcomparator = new LogLcomparator();
            Collections.sort(sqsolutions, logLcomparator);
            jocularMain.addSolutionPlotToCurrentObs(sqsolutions.get(0));
        }

        return sqsolutions;

    }

    private static boolean validEvent(int dPos, int rPos, int leftLim, int rightLim) {
        if (dPos == leftLim) {
            return (rPos > leftLim + 2) && (rPos < rightLim - 2);
        }
        if (rPos == rightLim) {
            return (dPos > leftLim + 2) && (dPos < rightLim - 2);
        }
        return (rPos - dPos > 2) && (rPos - dPos < rightLim - leftLim - 3);
    }
}

class LogLcomparator implements Comparator<SqSolution> {

    @Override
    public int compare(SqSolution one, SqSolution two) {
        // sort is largest to smallest
        return Double.compare(two.logL, one.logL);
    }

}
