package jocularmain;

import utils.Observation;

public class SqModel {

    private int dTranIndex = Integer.MIN_VALUE;
    private int rTranIndex = Integer.MAX_VALUE;
    
    private double logL = Double.NaN;
    private double dSolution = Double.NaN;
    private double rSolution = Double.NaN;

    public SqModel(Observation obs) {

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
        return 0.0;
    }
    
    public double getDsolution() {
        return dSolution;
    }
    
    public double getRsolution() {
        return rSolution;
    }
}
