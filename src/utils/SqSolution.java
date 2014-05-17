package utils;

public class SqSolution {

    public double logL = Double.NaN;
    public double B = Double.NaN;
    public double A = Double.NaN;
    public double D = Double.NaN;
    public double R = Double.NaN;
    public double sigmaB = Double.NaN;
    public double sigmaA = Double.NaN;
    public int dTransitionIndex;
    public int rTransitionIndex;

    @Override
    public String toString() {
        return "SqSolution: [" + dTransitionIndex + "," + rTransitionIndex
            + "]  logL=" + logL + " D=" + D + " R=" + R + " B=" + B + " A=" + A
            + " sigmaB=" + sigmaB + " sigmaA=" + sigmaA;
    }
}
