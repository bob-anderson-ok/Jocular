package jocularmain;

import utils.Observation;

public class SqSolver {

    private int[] dTranCandidates;
    private int[] rTranCandidates;

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

    static public void computeCandidates(JocularMain jocularMain,
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
        
        if ( dRight < dLeft) {
            String errMsg = "D limits reversed: dLeft=" + dLeft + "  dRight=" + dRight;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        if ( rRight < rLeft) {
            String errMsg = "R limits reversed: rLeft=" + rLeft + "  rRight=" + rRight;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        if ( dRight >= rLeft) {
            String errMsg = "R limits overlap D limits: dRight=" + dRight + "  rLeft=" + rLeft;
            jocularMain.showErrorDialog(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        
        System.out.println("dLeft: " + dLeft + "  dRight: " + dRight +
            "  rLeft: " + rLeft + "  rRight: " + rRight);

    }

}
