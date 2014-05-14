package joculartest;

import utils.Observation;

public class Solver {

    private Observation obs;

    /**
     * is the left limit (inclusive) of candidate d transition indices.
     * <p>
     * It can be negative to indicate that the leftmost d transition is outside of the observation data range.
     */
    private int dLeft;

    /**
     * is the right limit (inclusive) of candidate d transition indices.
     * <p>
     * It must be less than rLeft and >= dLeft
     * <p>
     * It can be negative to indicate that there is no candidate d transition in the observation data range.
     */
    private int dRight;

    /**
     * is the left limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than dRight.
     * <p>
     * It can be outside the observation data range (> data length) to indicate that there is no candidate r transition in the observation.
     */
    private int rLeft;

    /**
     * is the right limit (inclusive) of candidate r transition indices.
     * <p>
     * It must be greater than rLeft.
     * <p>
     * It can be outside the observation data range (> data length) to indicate that there is no righthand r transition in the observation.
     * */
     private int rRight;
     
     }
