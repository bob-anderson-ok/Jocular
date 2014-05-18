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
        String tidy = String.format("[%5d,%5d]   logL=%11.2f   D=%8.2f  R=%8.2f  B=%9.2f  A=%9.2f  sigmaB=%8.2f  sigmaA=%8.2f", 
                                    dTransitionIndex, rTransitionIndex, logL, D, R, B, A, sigmaB, sigmaA);
        return tidy;
    }
}
