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
        String tidy = String.format("SqSolution: [%d,%d] logL=%.2f D=%.2f R=%.2f B=%.2f A=%.2f sigmaB=%.2f sigmaA=%.2f", 
                                    dTransitionIndex, rTransitionIndex, logL, D, R, B, A, sigmaB, sigmaA);
        return tidy;
    }
}
