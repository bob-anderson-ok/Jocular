package utils;


public class SqSolution {
    public double logL = Double.NaN;
    public double B = Double.NaN;
    public double A = Double.NaN;
    public double D = Double.NaN;
    public double R = Double.NaN;
    
    @Override
    public String toString() {
        return "SqSolution[ B=" + B + "  A=" + A + " D=" + D + " R=" + R + " logL=" + logL + "]";
    }
}
