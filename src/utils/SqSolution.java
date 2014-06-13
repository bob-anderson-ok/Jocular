package utils;

public class SqSolution {

    public double logL = Double.NaN;
    public double B = Double.NaN;
    public double A = Double.NaN;
    public double D = Double.NaN;
    public double R = Double.NaN;
    public double magDrop = Double.NaN;
    public double sigmaB = Double.NaN;
    public double sigmaA = Double.NaN;
    public int dTransitionIndex;
    public int rTransitionIndex;
    public int kFactor;
    public double aicc = Double.NaN;
    public double relLikelihood = Double.NaN;
    public int numBaselinePoints;
    public int numEventPoints;

    @Override
    public String toString() {
        //String tidy = String.format("[%5d,%5d] relLike=%5.2f AICc=%-11.2f logL=%-11.2f D=%-8.2f R=%-8.2f B=%-9.2f A=%-9.2f magDrop=%-7.2f k=%d", 
        //                            dTransitionIndex, rTransitionIndex, relLikelihood, aicc, logL, D, R, B, A, magDrop, kFactor);
        String tidy = String.format("[%4d,%4d] rLike=%5.2f D=%-7.2f R=%-7.2f B=%-7.2f  A=%-7.2f magDrop=%-5.2f k=%d", 
                                    dTransitionIndex, rTransitionIndex, relLikelihood, D, R, B, A, magDrop, kFactor);
        return tidy;
    }
}
