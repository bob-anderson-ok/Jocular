package jocularmain;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import utils.Observation;
import utils.RandUtils;

public class SqModel {

    private int dTranIndex = Integer.MIN_VALUE;
    private int rTranIndex = Integer.MAX_VALUE;

    private double logL = Double.NaN;
    private double dSolution = Double.NaN;
    private double rSolution = Double.NaN;

    private Observation obs;

    private double cumBaseline;
    private double cumEvent;

    private double B;
    private double A;

    private double sigmaB;
    private double sigmaA;

    private double[] baselinePoints;
    private double[] eventPoints;

    private int numBaselinePoints = 0;
    private int numEventPoints = 0;

    public SqModel(Observation obs) {
        this.obs = obs;
    }

    public SqModel setDtransition(int dTranIndex) {
        this.dTranIndex = dTranIndex;
        return this;
    }

    public SqModel setRtransition(int rTranIndex) {
        this.rTranIndex = rTranIndex;
        return this;
    }

    public double calcLogL() {
        resetCalculation();
        calculateNumberOfEventAndBaselinePoints();
        generateBaselineAndEventArrays();
        calculateBandA();
        calculateSigmas();
        double logLbaseline = calcBaselineLogL();
        //System.out.println("logLbaseline=" + logLbaseline);

        double logLevent = calcEventLogL();
        //System.out.println("logLevent=" + logLevent);

        TransitionData dTranData = calcDtranLogL();
        dSolution = dTranData.position;
        double logLdTran = dTranData.logLcontribution;
        //System.out.println("logLdTran=" + logLdTran);
        
        TransitionData rTranData = calcRtranLogL();
        rSolution = rTranData.position;
        double logLrTran = rTranData.logLcontribution;
        //System.out.println("logLrTran=" + logLrTran);

        double sumLogL = logLevent + logLbaseline + logLdTran + logLrTran;
        //System.out.println("sumLogL=" + sumLogL);
        
        return sumLogL;
    }

    private double logL(double value, double reference, double sigma) {

        double term1 = log(1.0 / sigma);
        double term2 = log(1.0 / sqrt(2 * PI));
        double term3 = (value - reference) / sigma;

        return term1 + term2 - term3 * term3 / 2.0;
    }

    class TransitionData {

        double position = Double.NaN;
        double logLcontribution = 0.0;
    }

    private TransitionData calcDtranLogL() {

        TransitionData ans = new TransitionData();

        if (!inRange(dTranIndex)) {
            return ans;
        }

        double obsValue = obs.obsData[dTranIndex];

        double logLagainstB = logL(obsValue, B, sigmaB);

        if (obsValue >= B) {
            ans.position = dTranIndex;
            ans.logLcontribution = logLagainstB;
            return ans;
        }

        double logLagainstA = logL(obsValue, A, sigmaA);

        if (obsValue <= A) {
            ans.position = dTranIndex - 1;
            ans.logLcontribution = logLagainstA;
            return ans;
        }

        double sigmaM = sigmaA + (sigmaB - sigmaA) * ((obsValue - A) / (B - A));
        double logLagainstM = logL(B, B, sigmaM);

        double margin = 1.0; // Minimum AIC margin to justify using M for sub-frame timing

        if (((logLagainstM - margin) > logLagainstA) && ((logLagainstM - margin) > logLagainstB)) {
            // We have an AIC validated mid-value (sub frame timing justified)
            ans.position = dTranIndex - 1 + ((obsValue - A) / (B - A));
            ans.logLcontribution = logLagainstM;
        } else if (logLagainstB > logLagainstA) {
            ans.position = dTranIndex;
            ans.logLcontribution = logLagainstB;
        } else {
            ans.position = dTranIndex - 1;
            ans.logLcontribution = logLagainstA;
        }
        return ans;
    }

    private TransitionData calcRtranLogL() {

        TransitionData ans = new TransitionData();

        if (!inRange(rTranIndex)) {
            return ans;
        }

        double obsValue = obs.obsData[rTranIndex];

        double logLagainstB = logL(obsValue, B, sigmaB);

        if (obsValue >= B) {
            ans.position = rTranIndex-1;
            ans.logLcontribution = logLagainstB;
            return ans;
        }

        double logLagainstA = logL(obsValue, A, sigmaA);

        if (obsValue <= A) {
            ans.position = rTranIndex;
            ans.logLcontribution = logLagainstA;
            return ans;
        }

        double sigmaM = sigmaA + (sigmaB - sigmaA) * ((obsValue - A) / (B - A));
        double logLagainstM = logL(B, B, sigmaM);

        double margin = 1.0; // Minimum AIC margin to justify using M for sub-frame timing.

        if (((logLagainstM - margin) > logLagainstA) && ((logLagainstM - margin) > logLagainstB)) {
            // We have an AIC validated mid-value (sub frame timing justified)
            ans.position = rTranIndex - ((obsValue - A) / (B - A));
            ans.logLcontribution = logLagainstM;
        } else if (logLagainstB > logLagainstA) {
            ans.position = rTranIndex-1;
            ans.logLcontribution = logLagainstB;
        } else {
            ans.position = rTranIndex;
            ans.logLcontribution = logLagainstA;
        }
        return ans;
    }

    
    private double calcBaselineLogL() {
        double result = 0.0;
        for (int i = 0; i < numBaselinePoints; i++) {
            result += logL(baselinePoints[i], B, sigmaB);
        }
        return result;
    }

    private double calcEventLogL() {
        double result = 0.0;
        for (int i = 0; i < numEventPoints; i++) {
            result += logL(eventPoints[i], A, sigmaA);
        }
        return result;
    }

    private void calculateSigmas() {
        if (numBaselinePoints < 2 || numEventPoints < 2) {
            throw new IllegalArgumentException(
                "in SqModel.calculateSigmas: numBaselinePoints=" + numBaselinePoints + "  numEventPoints=" + numEventPoints
            );
        }
        sigmaB = RandUtils.calcSigma(baselinePoints);
        sigmaA = RandUtils.calcSigma(eventPoints);
    }

    private void calculateBandA() {
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
        baselinePoints = new double[numBaselinePoints];
        eventPoints = new double[numEventPoints];

        int bPtr = 0;
        int ePtr = 0;

        for (int i = 0; i < obs.lengthOfDataColumns; i++) {
            if (i < dTranIndex) {
                baselinePoints[bPtr++] = obs.obsData[i];
            } else if (i > dTranIndex && i < rTranIndex) {
                eventPoints[ePtr++] = obs.obsData[i];
            } else if (i > rTranIndex) {
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
    }

    private void calculateNumberOfEventAndBaselinePoints() {
        if (inRange(dTranIndex) && inRange(rTranIndex)) {
            numEventPoints = rTranIndex - dTranIndex - 1;
        } else if (inRange(rTranIndex)) {
            numEventPoints = rTranIndex;
        } else if (inRange(dTranIndex)) {
            numEventPoints = obs.lengthOfDataColumns - dTranIndex - 1;
        } else {
            throw new IllegalArgumentException("In SqModel: both transition indices are out of range -- there is no event");
        }

        numBaselinePoints = obs.lengthOfDataColumns - numEventPoints;
        if (inRange(dTranIndex)) {
            numBaselinePoints--;
        }
        if (inRange(rTranIndex)) {
            numBaselinePoints--;
        }
    }

    private boolean inRange(int index) {
        if (index < 0) {
            return false;
        } else {
            return index < obs.lengthOfDataColumns;
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

    public double getSigmaB() {
        return sigmaB;
    }

    public double getSigmaA() {
        return sigmaA;
    }

}
