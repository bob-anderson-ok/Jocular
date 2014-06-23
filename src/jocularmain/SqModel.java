package jocularmain;

import utils.Observation;
import static utils.JocularUtils.logL;

public class SqModel {

    private int dTranNum = Integer.MIN_VALUE;
    private int rTranNum = Integer.MAX_VALUE;
    private int binSize = 1;

    private double logL = Double.NaN;
    private double dSolution = Double.NaN;
    private double rSolution = Double.NaN;

    private Observation obs;

    private double cumBaseline;
    private double cumEvent;

    private double B;
    private double A;

    private double[] baselinePoints;
    private double[] eventPoints;

    private int numBaselinePoints = 0;
    private int numEventPoints = 0;

    private int minEventSize = -1;
    private int maxEventSize = -1;

    private int trimOffset = 0;

    private int kFactor;

    public SqModel(Observation obs) {
        this.obs = obs;
    }

    public Observation getObs() {
        return obs;
    }

    public SqModel setDtransition(int dTranNum) {
        this.dTranNum = dTranNum;
        return this;
    }

    public SqModel setRtransition(int rTranNum) {
        this.rTranNum = rTranNum;
        return this;
    }

    public SqModel setbinSize(int binSize) {
        this.binSize = binSize;
        return this;
    }

    public boolean validTransitionPair() {
        resetCalculation();

        trimOffset = obs.readingNumbers[0];
        calculateNumberOfEventAndBaselinePoints();

        // The calculation of logL cannot be done when there are
        // insufficient baseline or event points to permit the
        // computation of A and B.  We return NaN and let
        // the caller test for this.
        if (numBaselinePoints < 1 || numEventPoints < 1) {
            return false;
        }

        if (numEventPoints < minEventSize) {
            return false;
        }

        if (minEventSize > 0 && numEventPoints < minEventSize) {
            return false;
        }

        if (maxEventSize > 0 && numEventPoints > maxEventSize) {
            return false;
        }

        if (dTranNum > rTranNum) {
            return false;
        }
        return true;
    }

    public double calcLogL(double sigmaB, double sigmaA) {

        if (!validTransitionPair()) {
            return Double.NaN;
        }

        generateBaselineAndEventArrays();
        calculateBandA();

        if (B < A) {
            return Double.NaN;
        }

        double logLbaseline = calcBaselineLogL(sigmaB);
        double logLevent = calcEventLogL(sigmaA);

        TransitionData dTranData = calcDtranLogL(sigmaB, sigmaA);
        dSolution = dTranData.position;
        double logLdTran = dTranData.logLcontribution;

        TransitionData rTranData = calcRtranLogL(sigmaB, sigmaA);
        rSolution = rTranData.position;
        double logLrTran = rTranData.logLcontribution;

        double sumLogL = logLevent + logLbaseline + logLdTran + logLrTran;

        return sumLogL;
    }

    public double straightLineLogL(double sigmaB) {
        double lineLevel = sumArray(obs.obsData) / obs.obsData.length;
        double ans = 0.0;
        for (int i = 0; i < obs.obsData.length; i++) {
            ans += logL(obs.obsData[i], lineLevel, sigmaB);
        }
        return ans;
    }

    class TransitionData {

        double position = Double.NaN;
        double logLcontribution = 0.0;
    }

    private TransitionData calcDtranLogL(double sigmaB, double sigmaA) {

        TransitionData ans = new TransitionData();

        if (!inRange(dTranNum)) {
            return ans;
        }

        int obsIndex = (dTranNum - obs.readingNumbers[0]) / binSize;
        //System.out.println("obsIndex: " + obsIndex);
        double obsValue = obs.obsData[obsIndex];

        double logLagainstB = logL(obsValue, B, sigmaB);

        //double bAvgBias = -binSize / 4.0;
        //double aAvgBias = -3 * binSize / 4.0;
        double bAvgBias = 0.0;
        double aAvgBias = -binSize;

        if (obsValue >= B) {
            ans.position = dTranNum + bAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstB;
            return ans;
        }

        double logLagainstA = logL(obsValue, A, sigmaA);

        if (obsValue <= A) {
            ans.position = dTranNum + aAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstA;
            return ans;
        }

        double sigmaM = sigmaA + (sigmaB - sigmaA) * ((obsValue - A) / (B - A));
        double logLagainstM = logL(B, B, sigmaM);

        double margin = 1.0; // Minimum AIC margin to justify using M for sub-frame timing

        if (((logLagainstM - margin) > logLagainstA) && ((logLagainstM - margin) > logLagainstB)) {
            // We have an AIC validated mid-value (sub frame timing justified)
            ans.position = dTranNum - binSize + ((obsValue - A) / (B - A)) * binSize;
            ans.logLcontribution = logLagainstM;
            kFactor += 2;
        } else if (logLagainstB > logLagainstA) {
            ans.position = dTranNum + bAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstB;
        } else {
            ans.position = dTranNum + aAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstA;
        }
        return ans;
    }

    private TransitionData calcRtranLogL(double sigmaB, double sigmaA) {

        TransitionData ans = new TransitionData();

        if (!inRange(rTranNum)) {
            return ans;
        }

        double obsValue = obs.obsData[(rTranNum - obs.readingNumbers[0]) / binSize];

        double logLagainstB = logL(obsValue, B, sigmaB);

        //double aAvgBias = -binSize / 4.0;
        //double bAvgBias = -3 * binSize / 4.0;
        double aAvgBias = 0.0;
        double bAvgBias = -binSize;

        if (obsValue >= B) {
            ans.position = rTranNum + bAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstB;
            return ans;
        }

        double logLagainstA = logL(obsValue, A, sigmaA);

        if (obsValue <= A) {
            ans.position = rTranNum + aAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstA;
            return ans;
        }

        double sigmaM = sigmaA + (sigmaB - sigmaA) * ((obsValue - A) / (B - A));
        double logLagainstM = logL(B, B, sigmaM);

        double margin = 1.0; // Minimum AIC margin to justify using M for sub-frame timing.

        if (((logLagainstM - margin) > logLagainstA) && ((logLagainstM - margin) > logLagainstB)) {
            // We have an AIC validated mid-value (sub frame timing justified)
            ans.position = rTranNum - ((obsValue - A) / (B - A)) * binSize;
            ans.logLcontribution = logLagainstM;
            kFactor += 2;
        } else if (logLagainstB > logLagainstA) {
            ans.position = rTranNum + bAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstB;
        } else {
            ans.position = rTranNum + aAvgBias;
            kFactor++;
            ans.logLcontribution = logLagainstA;
        }
        return ans;
    }

    private double calcBaselineLogL(double sigmaB) {
        double result = 0.0;
        for (int i = 0; i < numBaselinePoints; i++) {
            result += logL(baselinePoints[i], B, sigmaB);
        }
        return result;
    }

    private double calcEventLogL(double sigmaA) {
        double result = 0.0;
        for (int i = 0; i < numEventPoints; i++) {
            result += logL(eventPoints[i], A, sigmaA);
        }
        return result;
    }

    private void calculateBandA() {
        //System.out.println("in calculateBandA");
        if (numBaselinePoints == 0 || numEventPoints == 0) {
            throw new IllegalArgumentException(
                "in SqModel.calculateAandB: numBaselinePoints=" + numBaselinePoints + "  numEventPoints=" + numEventPoints
            );
        }
        B = sumArray(baselinePoints) / numBaselinePoints;
        A = sumArray(eventPoints) / numEventPoints;
    }

    private double sumArray(double[] array) {
        double result = 0.0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result;
    }

    private void generateBaselineAndEventArrays() {

        //System.out.println("in generateBaseLineAndEventArrays: dTranNum=" + dTranNum + " rTranNum=" + rTranNum);
        baselinePoints = new double[numBaselinePoints];
        eventPoints = new double[numEventPoints];

        int bPtr = 0;
        int ePtr = 0;

        for (int i = 0; i < obs.obsData.length; i++) {
            if (obs.readingNumbers[i] < dTranNum) {
                baselinePoints[bPtr++] = obs.obsData[i];
            } else if (obs.readingNumbers[i] > dTranNum && obs.readingNumbers[i] < rTranNum) {
                eventPoints[ePtr++] = obs.obsData[i];
            } else if (obs.readingNumbers[i] > rTranNum) {
                baselinePoints[bPtr++] = obs.obsData[i];
            }
        }
    }

    private void resetCalculation() {

        logL = Double.NaN;
        dSolution = Double.NaN;
        rSolution = Double.NaN;

        numBaselinePoints = 0;
        numEventPoints = 0;
        kFactor = 2;
        binSize = obs.readingNumbers[1] - obs.readingNumbers[0];
    }

    private void calculateNumberOfEventAndBaselinePoints() {
        if (inRange(dTranNum) && inRange(rTranNum)) {
            numEventPoints = (rTranNum - dTranNum) / binSize - 1;
        } else if (inRange(rTranNum)) {
            numEventPoints = (rTranNum - trimOffset) / binSize;
        } else if (inRange(dTranNum)) {
            numEventPoints = obs.obsData.length - (dTranNum - trimOffset) / binSize - 1;
        } else {
            // Force a NaN return to the caller.
            numEventPoints = 0;
        }

        numBaselinePoints = obs.obsData.length - numEventPoints;
        if (inRange(dTranNum)) {
            numBaselinePoints--;
        }
        if (inRange(rTranNum)) {
            numBaselinePoints--;
        }
        //System.out.println("nBase=" + numBaselinePoints + " nEvent=" + numEventPoints);
    }

    private boolean inRange(int index) {
        if (index < obs.readingNumbers[0]) {
            return false;
        } else {
            return index <= obs.readingNumbers[obs.readingNumbers.length - 1];
        }
    }

    public int getNumEventPoints() {
        return numEventPoints;
    }

    public int getNumBaselinePoints() {
        return numBaselinePoints;
    }

    public double getDsolution() {
        return dSolution;
    }

    public double getRsolution() {
        return rSolution;
    }

    public double getB() {
        return B;
    }

    public double getA() {
        return A;
    }

    public int getkFactor() {
        return kFactor;
    }

    public void setMinEventSize(int minEventSize) {
        this.minEventSize = minEventSize;
    }

    public void setMaxEventSize(int maxEventSize) {
        this.maxEventSize = maxEventSize;
    }

}
