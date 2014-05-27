package jocularmain;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static utils.JocularUtils.gaussianVariate;

public class MonteCarloTrial {

    private TrialParams trialParams;

    public MonteCarloTrial(TrialParams trialParams) {
        this.trialParams = trialParams;
    }

    private static double LOG_SQRT_TWO_PI = log(sqrt(2 * PI));

    private double logL(double value, double reference, double sigma) {

        double term1 = -log(sigma);        // == log(1/sigma)
        double term2 = -LOG_SQRT_TWO_PI;   // == log(1/sqrt(2*PI))
        double term3 = (value - reference) / sigma;

        return term1 + term2 - term3 * term3 / 2.0;
    }

    private int getIndexOfMaxValue(double[] values) {
        double highest = values[0];
        int index = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > highest) {
                highest = values[i];
                index = i;
            }
        }
        return index;
    }

    public int[] calcHistogram() {
        int[] histogram = new int[trialParams.sampleWidth];  // Output

        double[] logLB = new double[trialParams.sampleWidth];
        double[] logLA = new double[trialParams.sampleWidth];
        double[] logLM = new double[trialParams.sampleWidth];
        double[] logLT = new double[trialParams.sampleWidth];
        double[] rdgs = new double[trialParams.sampleWidth];
        int[] offset = new int[trialParams.sampleWidth];

        int centerIndex = trialParams.sampleWidth / 2;

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] = 0;
        }

        for (int k = 0; k < trialParams.numTrials; k++) {

            // Make a new sample
            for (int i = 0; i < centerIndex; i++) {
                rdgs[i] = gaussianVariate(trialParams.sigmaA) + trialParams.eventLevel;
            }
//            rdgs[centerIndex] = gaussianVariate((trialParams.sigmaA + trialParams.sigmaB) / 2.0)
//                + (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
            rdgs[centerIndex] = gaussianVariate(trialParams.sigmaA)
                + trialParams.eventLevel;
            for (int i = centerIndex + 1; i < rdgs.length; i++) {
                rdgs[i] = gaussianVariate(trialParams.sigmaB) + trialParams.baselineLevel;
            }

            for (int i = 0; i < trialParams.sampleWidth; i++) {
                logLB[i] = logL(rdgs[i], trialParams.baselineLevel, trialParams.sigmaB);
                logLA[i] = logL(rdgs[i], trialParams.eventLevel, trialParams.sigmaA);
                if (logLB[i] > logLA[i]) {
                    logLM[i] = logLB[i];
                    offset[i] = -1;
                } else {
                    logLM[i] = logLA[i];
                    offset[i] = 0;
                }
            }

            // Initialize first cum logL term to begin iterative computation
            logLT[0] = logLM[0];
            for (int i = 1; i < trialParams.sampleWidth; i++) {
                logLT[0] += logLB[i];
            }

            for (int i = 1; i < trialParams.sampleWidth; i++) {
                logLT[i] = logLT[i - 1] - logLM[i - 1] - logLB[i] + logLA[i - 1] + logLM[i];
            }

            int indexOfMaxLogL = getIndexOfMaxValue(logLT);
            int transitionIndex = indexOfMaxLogL + offset[indexOfMaxLogL];
            if (transitionIndex < 0) {
                transitionIndex = 0;
            }
            
            histogram[transitionIndex]++;

        }
        return histogram;
    }
}
