package jocularmain;

import utils.Observation;

public class SqModel {

    private int dTranIndex = Integer.MIN_VALUE;
    private int rTranIndex = Integer.MAX_VALUE;

    private double logL = Double.NaN;
    private double dSolution = Double.NaN;
    private double rSolution = Double.NaN;

    private Observation obs;

    private double cumBaseline;
    private double cumEvent;

    private double baseline;
    private double event;

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
        return 0.0;
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

    private void addToBaseline(double value) {
        if (numBaselinePoints == 0) {
            cumBaseline = value;
        } else {
            cumBaseline += value;
        }
        numBaselinePoints++;
    }

    private void addToEvent(double value) {
        if (numEventPoints == 0) {
            cumEvent = value;
        } else {
            cumEvent += value;
        }
        numEventPoints++;
    }

}
