package jocularmain;

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
        return 0.0;
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
        } else {
            numEventPoints = obs.lengthOfDataColumns - dTranIndex - 1;
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
