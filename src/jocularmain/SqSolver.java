package jocularmain;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import utils.JocularUtils;
import utils.Observation;
import utils.SqSolution;

public class SqSolver {

    public static int[] dTranCandidates;  // made public to simplify testing
    public static int[] rTranCandidates;  // made public to simplify testing

    /* dLeft, dRight, rLeft, and rRight are made public to make it easier to
     * test the class.
     */
    /**
     * is the left limit (inclusive) of candidate d transition indices.
     * <p>
     * It can be negative to indicate that the leftmost d transition is outside
     * of the observation data range.
     */
    public static int dLeft;

    /**
     * is the right limit (inclusive) of candidate d transition indices.
     * <p>
     * It must be less than rLeft and >= dLeft
     * <p>
     * It can be negative to indicate that there is no candidate d transition in
     * the observation data range.
     */
    public static int dRight;

    /**
     * is the left limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than dRight.
     * <p>
     * It can be outside the observation data range (> data length) to indicate
     * that there is no candidate r transition in the observation.
     */
    public static int rLeft;

    /**
     * is the right limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than rLeft.
     * <p>
     * It can be outside the observation data range (> data length) to indicate
     * that there is no righthand r transition in the observation.
     *
     */
    public static int rRight;

    static public void computeCandidates(DoubleProperty progressProperty,
                                         EventHandler<WorkerStateEvent> successHandler,
                                         EventHandler<WorkerStateEvent> cancelledHandler,
                                         EventHandler<WorkerStateEvent> failedHandler,
                                         JocularMain jocularMain,
                                         SolutionStats solutionStats,
                                         double sigmaB, double sigmaA,
                                         double minMagDrop, double maxMagDrop,
                                         int minEventSize, int maxEventSize,
                                         XYChartMarker dLeftMarker,
                                         XYChartMarker dRightMarker,
                                         XYChartMarker rLeftMarker,
                                         XYChartMarker rRightMarker
    ) {

        // Figure out the parameters of any 'binning' that has happened.
        Observation curObs = jocularMain.getCurrentObservation();
        int binSize = curObs.readingNumbers[1] - curObs.readingNumbers[0];
        int offset = curObs.readingNumbers[0];
        
        //int leftLimit = jocularMain.getOutOfRangeOfObsOnTheLeft();
        //int rightLimit = jocularMain.getOutOfRangeOfObsOnTheRight();
        
        int leftLimit = curObs.readingNumbers[0] - binSize;
        int rightLimit = curObs.readingNumbers[curObs.readingNumbers.length-1] + binSize;

        if (dLeftMarker.isInUse() != dRightMarker.isInUse()) {
            String errMsg = "D marker usage must be paired";
            jocularMain.showErrorDialog(errMsg, jocularMain.primaryStage);
            throw new IllegalArgumentException(errMsg);
        }

        if (rLeftMarker.isInUse() != rRightMarker.isInUse()) {
            String errMsg = "R marker usage must be paired";
            jocularMain.showErrorDialog(errMsg, jocularMain.primaryStage);
            throw new IllegalArgumentException(errMsg);
        }

        if (dLeftMarker.isInUse()) {
            dLeft = (int) Math.ceil(dLeftMarker.getXValue());
            dLeft = Math.min(dLeft,rightLimit);
            dLeft = Math.max(dLeft, leftLimit);
        } else {
            dLeft = leftLimit;
        }

        if (dRightMarker.isInUse()) {
            dRight = (int) Math.floor(dRightMarker.getXValue());
            dRight = Math.min(dRight, rightLimit);
            dRight = Math.max(dRight, leftLimit);
        } else {
            dRight = rightLimit;
        }

        if (rLeftMarker.isInUse()) {
            rLeft = (int) Math.ceil(rLeftMarker.getXValue());
            rLeft = Math.min(rLeft,rightLimit);
            rLeft = Math.max(rLeft, leftLimit);
        } else {
            rLeft = leftLimit;
        }

        if (rRightMarker.isInUse()) {
            rRight = (int) Math.floor(rRightMarker.getXValue());
            rRight = Math.min(rRight, rightLimit);
            rRight = Math.max(rRight, leftLimit);
        } else {
            rRight = rightLimit;
        }

        if (dRight < dLeft) {
            String errMsg = "D limits reversed: dLeft=" + dLeft + "  dRight=" + dRight;
            jocularMain.showErrorDialog(errMsg, jocularMain.primaryStage);
            throw new IllegalArgumentException(errMsg);
        }

        if (rRight < rLeft) {
            String errMsg = "R limits reversed: rLeft=" + rLeft + "  rRight=" + rRight;
            jocularMain.showErrorDialog(errMsg, jocularMain.primaryStage);
            throw new IllegalArgumentException(errMsg);
        }

        if (Math.max(rLeft, rRight) < Math.min(dLeft, dRight)) {
            String errMsg = "Invalid marker settings: specifies reappearance before disappearance";
            jocularMain.showErrorDialog(errMsg, jocularMain.primaryStage);
            throw new IllegalArgumentException(errMsg);
        }

        //dTranCandidates = new int[dRight - dLeft + 1];
        //rTranCandidates = new int[rRight - rLeft + 1];

        ArrayList<Integer> dTranBinned = new ArrayList<>();
        if ( dLeft == leftLimit) {
            dTranBinned.add(leftLimit);
        }
        for ( int i = 0; i < curObs.readingNumbers.length; i++) {
            if ( (dRight >= curObs.readingNumbers[i]) && (curObs.readingNumbers[i] >= dLeft) ) {
                dTranBinned.add(curObs.readingNumbers[i]);
            }
        }
        if ( dRight == rightLimit) {
            dTranBinned.add(rightLimit);
        }
        dTranCandidates = new int[dTranBinned.size()];
        for (int i=0; i<dTranBinned.size(); i++) {
            dTranCandidates[i] = dTranBinned.get(i);
        }
        
        ArrayList<Integer> rTranBinned = new ArrayList<>();
        if ( rLeft == leftLimit) {
            rTranBinned.add(leftLimit);
        }
        for ( int i = 0; i < curObs.readingNumbers.length; i++) {
            if ( (rRight >= curObs.readingNumbers[i]) && (curObs.readingNumbers[i] >= rLeft) ) {
                rTranBinned.add(curObs.readingNumbers[i]);
            }
        }
        if ( rRight == rightLimit) {
            rTranBinned.add(rightLimit);
        }
        rTranCandidates = new int[rTranBinned.size()];
        for (int i=0; i<rTranBinned.size(); i++) {
            rTranCandidates[i] = rTranBinned.get(i);
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

        solutionStats.numTransitionPairsConsidered = numTranPairsConsidered;
        solutionStats.numValidTransitionPairs = numValidTranPairs;
        solutionStats.straightLineLogL = straightLineLogL;
        solutionStats.straightLineAICc = JocularUtils.aicc(straightLineLogL, 1, n);

        jocularMain.setupSolverService(
            successHandler,
            cancelledHandler,
            failedHandler,
            solutionStats,
            dTranCandidates,
            rTranCandidates,
            sqmodel,
            sigmaB,
            sigmaA,
            n,
            minMagDrop,
            maxMagDrop,
            progressProperty
        );
    }
}
