package utils;

public class HistStatItem {

    /**
     * Holds the number of trials that produced a solution at this position.
     */
    public int count;

    /**
     * The index into the underlying histogram array.
     *
     * The position (index) of the count.
     */
    public int position;

    /**
     * When HistStatItems have been sorted in descending order of count, the cumulative count is placed in this variable by the HDI (highest
     * density interval) during the confidence interval estimation.
     */
    public int cumCount;

}
