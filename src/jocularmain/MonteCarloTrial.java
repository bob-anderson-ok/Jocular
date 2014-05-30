package jocularmain;

import static java.lang.Math.log;
import utils.JocularUtils;
import static utils.JocularUtils.gaussianVariate;
import static utils.JocularUtils.logL;
import static utils.JocularUtils.aicc;
import utils.MonteCarloResult;

public class MonteCarloTrial {

    private TrialParams trialParams;

    public MonteCarloTrial(TrialParams trialParams) {
        this.trialParams = trialParams;
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

    public MonteCarloResult calcHistogram() {
        // Initialize the output vector
        int[] histogram = new int[trialParams.sampleWidth];

        double[] logLB = new double[trialParams.sampleWidth];
        double[] logLA = new double[trialParams.sampleWidth];
        double[] logLM = new double[trialParams.sampleWidth];
        double[] logLT = new double[trialParams.sampleWidth];
        double[] rdgs = new double[trialParams.sampleWidth];
        int[] offset = new int[trialParams.sampleWidth];

        double logLstraightLine = 0.0;
        int rejectedSolutions = 0;

        int centerIndex = trialParams.sampleWidth / 2;

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] = 0;
        }

        for (int k = 0; k < trialParams.numTrials; k++) {

            // Make a new sample
            for (int i = 0; i < centerIndex; i++) {
                rdgs[i] = gaussianVariate(trialParams.sigmaA) + trialParams.eventLevel;
            }

            if (trialParams.mode == MonteCarloMode.LEFT_EDGE) {
                rdgs[centerIndex] = gaussianVariate(trialParams.sigmaA) + trialParams.eventLevel;
            } else if (trialParams.mode == MonteCarloMode.RIGHT_EDGE) {
                rdgs[centerIndex] = gaussianVariate(trialParams.sigmaB) + trialParams.baselineLevel;
            } else if (trialParams.mode == MonteCarloMode.MID_POINT) {
                rdgs[centerIndex] = gaussianVariate((trialParams.sigmaA + trialParams.sigmaB) / 2.0)
                    + (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
            } else if (trialParams.mode == MonteCarloMode.RANDOM) {
                double frac = JocularUtils.linearVariateZeroToOne();
                double level = trialParams.baselineLevel * (1.0 - frac) + trialParams.eventLevel * frac;
                double noiseAtLevel = trialParams.sigmaB * (1.0 - frac) + trialParams.sigmaA * frac;
                rdgs[centerIndex] = gaussianVariate(noiseAtLevel) + level;
            } else {
                throw new InternalError("Design error: MonteCarlo mode not implemented.");
            }

            for (int i = centerIndex + 1; i < rdgs.length; i++) {
                rdgs[i] = gaussianVariate(trialParams.sigmaB) + trialParams.baselineLevel;
            }

            // The test case has now been created.  Calculate the various arrays that depend on this case.
            double midLevel = (trialParams.baselineLevel + trialParams.eventLevel) / 2.0;
            double midSigma = (trialParams.sigmaA + trialParams.sigmaB) / 2.0;

            logLstraightLine = 0.0;
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
                logLstraightLine += logL(rdgs[i], midLevel, midSigma);
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
            
            // We will discard any trail that result in a detected edge that is more 'probable'
            // than a simple straight line by at least 50
            double aiccMarginOfEdgeOverLine = log(50.0);

            if (aicc(logLstraightLine, 1, trialParams.sampleWidth) < 
                aicc(logLT[indexOfMaxLogL], 3, trialParams.sampleWidth) + aiccMarginOfEdgeOverLine) {
                // The trial does not contain a valid edge.
                rejectedSolutions += 1;
                k--; // Force a repeat try
                if ( rejectedSolutions >= trialParams.numTrials) {
                    throw new IllegalArgumentException("In MonteCarloTrial: too many trial rejections --- possibly noise too high" + 
                        " or number of points in trial (sample width) is too small.");
                }
            } else {
                histogram[transitionIndex]++;
            }

            //System.out.println("logLmax=" + logLT[indexOfMaxLogL] + "  logLSL=" + logLstraightLine);
            //histogram[transitionIndex]++;
        }

        System.out.println("Rejected solutions: " + rejectedSolutions);
        MonteCarloResult ans = new MonteCarloResult();
        ans.histogram = histogram;
        ans.numRejections = rejectedSolutions;
        return ans;
    }
}
