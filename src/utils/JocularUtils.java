package utils;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Random;
import jdistlib.Normal;

public interface JocularUtils {

    Random randomGenerator = new Random();

    static void setGaussianGeneratorSeed(long newSeed) {
        randomGenerator.setSeed(newSeed);
    }

    static double gaussianVariate(double sigma) {
        return randomGenerator.nextGaussian() * sigma;
    }
    
    static double[] generateGaussianNoise(int length, double sigma) {

        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            result[i] = randomGenerator.nextGaussian() * sigma;
        }
        
        return result;
    }

    static double calcSigma(double[] values) {

        if (values.length < 2) {
            throw new IllegalArgumentException(
                "calcSigma needs at least 2 values, " + values.length + " given."
            );
        }

        double sumOfValues = 0.0;

        for (double v : values) {
            sumOfValues += v;
        }

        double averageValue = sumOfValues / values.length;
        double squaredDifference = 0.0;

        for (double v : values) {
            squaredDifference += (v - averageValue) * (v - averageValue);
        }

        return Math.sqrt(squaredDifference / (values.length - 1));
    }
    
    static double calcSigma(ArrayList<Double> values) {
        // Unbox the ArrayList<Double> into a double[]
        double[] pointsInEstimate = new double[values.size()];
        for (int i = 0; i < pointsInEstimate.length; i++) {
            pointsInEstimate[i] = (double) values.get(i);
        }
        double sigma = calcSigma(pointsInEstimate);
        return sigma;
    }

    static double aicc(double logL, int k, int n) {
        double aic = 2.0 * k - 2.0 * logL;
        double corr = 2.0 * k * (k + 1) / (n - k - 1);
        return aic + corr;
    }
    
    static double calcMagDrop(double B, double A) {
        if (B<=0.0||A<=0.0||A>B) {
            return Double.NaN;
        } else {
            return (Math.log(B)-Math.log(A))/Math.log(2.512);
        }
    }

    static double AicCorr(int n, int k) {
        return 2.0 * k +(2.0 * k * (k + 1)) / (n - k - 1);
    }

    static double calcbAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((B - M) / sigmaB) * ((B - M) / sigmaB);
        double term2 = 1.0 - (1.0 - (sigmaA / sigmaB)) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    static double calcaAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((A - M) / sigmaA) * ((A - M) / sigmaA);
        double term2 = (sigmaB / sigmaA) - ((sigmaB / sigmaA) - 1.0) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    static double calcBsideSubframeBoundary(int n,double sigmaB, double sigmaA, double B, double A) {
        double aiccDelta = AicCorr(n,2) - AicCorr(n,1);  // Use this for finite sample size
        // aiccDelta = 2.0; // Use this for infinite sample size
        double M = B;
        double delta = (B - A) * 0.0001;
        while (calcbAIC(sigmaB, sigmaA, B, A, M) < aiccDelta) {
            M -= delta;
            if (M <= A) {
                return A;
            }
        }
        return M;
    }

    static double calcAsideSubframeBoundary(int n, double sigmaB, double sigmaA, double B, double A) {
        double aiccDelta = AicCorr(n,2) - AicCorr(n,1);  // Use this for finite sample size
        // aiccDelta=2.0; // Use this for infinite sample size
        double M = A;
        double delta = (B - A) * 0.0001;
        while (calcaAIC(sigmaB, sigmaA, B, A, M) < aiccDelta) {
            M += delta;
            if (M >= B) {
                return B;
            }
        }
        return M;
    }
    
    static double LOG_SQRT_TWO_PI = log(sqrt(2 * PI));

    static double logL(double value, double reference, double sigma) {

        double term1 = -log(sigma);        // == log(1/sigma)
        double term2 = -LOG_SQRT_TWO_PI;   // == log(1/sqrt(2*PI))
        double term3 = (value - reference) / sigma;

        return term1 + term2 - term3 * term3 / 2.0;
    }
    
    static double falsePositiveProbability( double signal, double sigmaB, int dur, int numObsPoints) {
        double sdA = sigmaB / sqrt( dur);
        double sdB = sigmaB / sqrt(numObsPoints-dur);
        Normal normDist = new Normal(0.0, sdA);
        //NormalDistribution normDist = new NormalDistribution(0.0, sd);
        //double probabilityOfFindingSignalAsAverage = normDist.cumulativeProbability(signal);
        double probabilityOfFindingSignalAsAverage = normDist.cumulative(signal-sdB);
        return (1.0 - Math.pow(probabilityOfFindingSignalAsAverage,(double)(numObsPoints-dur)));
    }
    
}
