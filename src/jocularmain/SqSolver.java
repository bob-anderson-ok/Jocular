package jocularmain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import utils.JocularUtils;
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
                                                     SolutionStats solutionStats,
                                                     double sigmaB, double sigmaA,
                                                     double minMagDrop, double maxMagDrop,
                                                     int minEventSize, int maxEventSize,
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
            dRight = Math.min(dRight, rightLimit);
        } else {
            dRight = rightLimit;
        }

        if (rLeftMarker.isInUse()) {
            rLeft = (int) Math.ceil(rLeftMarker.getXValue());
            rLeft = Math.max(rLeft, leftLimit);
        } else {
            rLeft = leftLimit;
        }

        if (rRightMarker.isInUse()) {
            rRight = (int) Math.floor(rRightMarker.getXValue());
            rRight = Math.min(rRight, rightLimit);
        } else {
            rRight = rightLimit;
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

        if (Math.max(rLeft, rRight) < Math.min(dLeft, dRight)) {
            String errMsg = "Invalid marker settings: specifies reappearance before disappearance";
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

        int numTranPairsConsidered = dTranCandidates.length * rTranCandidates.length;
        int numValidTranPairs = 0;

        List<SqSolution> sqsolutions = new ArrayList<>();

        SqModel sqmodel = new SqModel(jocularMain.getCurrentObservation());
        sqmodel.setMinEventSize(minEventSize);
        sqmodel.setMaxEventSize(maxEventSize);

        double straightLineLogL = sqmodel.straightLineLogL(sigmaB);

        int n = jocularMain.getCurrentObservation().obsData.length;

        double solMagDrop;

        for (int i = 0; i < dTranCandidates.length; i++) {
            for (int j = 0; j < rTranCandidates.length; j++) {
                SqSolution newSolution = new SqSolution();
                newSolution.dTransitionIndex = dTranCandidates[i];
                newSolution.rTransitionIndex = rTranCandidates[j];

                newSolution.logL = sqmodel
                    .setDtransition(newSolution.dTransitionIndex)
                    .setRtransition(newSolution.rTransitionIndex)
                    .calcLogL(sigmaB, sigmaA);

                // We let sqmodel determine when a dTran:rTran
                // combination is valid.  It lets us know the combo
                // is invalid (too few event or baseline points) by
                // returning NaN for logL.
                if (Double.isNaN(newSolution.logL)) {
                    continue;
                }

                solMagDrop = JocularUtils.calcMagDrop(sqmodel.getB(), sqmodel.getA());

                if (Double.isNaN(solMagDrop) || solMagDrop < minMagDrop || solMagDrop > maxMagDrop) {
                    continue;
                }

                numValidTranPairs++;

                newSolution.D = sqmodel.getDsolution();
                newSolution.R = sqmodel.getRsolution();
                newSolution.B = sqmodel.getB();
                newSolution.A = sqmodel.getA();
                newSolution.magDrop = solMagDrop;
                newSolution.sigmaB = sigmaB;
                newSolution.sigmaA = sigmaA;
                newSolution.kFactor = sqmodel.getkFactor();
                newSolution.aicc = JocularUtils.aicc(newSolution.logL, newSolution.kFactor, n);

                sqsolutions.add(newSolution);
            }
        }

        if (sqsolutions.size() > 0) {
            LogLcomparator logLcomparator = new LogLcomparator();
            Collections.sort(sqsolutions, logLcomparator);
            jocularMain.addSolutionCurveToMainPlot(sqsolutions.get(0));
        }

        solutionStats.numTransitionPairsConsidered = numTranPairsConsidered;
        solutionStats.numValidTransitionPairs = numValidTranPairs;
        solutionStats.straightLineLogL = straightLineLogL;
        solutionStats.straightLineAICc = JocularUtils.aicc(straightLineLogL, 1, n);

        return sqsolutions;
    }
}

class LogLcomparator implements Comparator<SqSolution> {

    @Override
    public int compare(SqSolution one, SqSolution two) {
        // sort is smallest to largest
        return Double.compare(one.aicc, two.aicc);
    }

}
