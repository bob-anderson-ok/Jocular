package jocularmain;

import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import utils.JocularUtils;
import utils.SqSolution;

public class SolverService extends Service<List<SqSolution>> {

    // private variables go here
    int[] dTranCandidates;
    int[] rTranCandidates;
    int n;
    SqModel sqmodel;
    double sigmaB;
    double sigmaA;
    double solMagDrop;
    double minMagDrop;
    double maxMagDrop;
    int numValidTranPairs;
    // setters of private variable go here
    public final void setdTranCandidates(int[] dTran) {
        dTranCandidates = dTran;
    }
    public final void setrTranCandidates(int[] rTran) {
        rTranCandidates = rTran;
    }
    public final void setsqmodel( SqModel sqmodel) {
        this.sqmodel = sqmodel;
    }
    public final void setsigmaB(double sigmaB) {
        this.sigmaB = sigmaB;
    }
    public final void setsigmaA(double sigmaA){
        this.sigmaA= sigmaA;
    }
    public final void setn( int n) {
        this.n = n;
    }
    public final void setminMagDrop(double minMagDrop) {
        this.minMagDrop = minMagDrop;
    }
    public final void setmaxMagDrop(double maxMagDrop) {
        this.maxMagDrop = maxMagDrop;
    }
    // getters go here
    public final int getNumValidTranPairs() {
        return numValidTranPairs;
    }

    @Override
    protected Task<List<SqSolution>> createTask() {
        return new Task<List<SqSolution>>() {
            @Override
            protected List<SqSolution> call() {
                List<SqSolution> sqsolutions = new ArrayList<>();
                // work goes in here
                numValidTranPairs = 0;
                for (int i = 0; i < dTranCandidates.length; i++) {
                    for (int j = 0; j < rTranCandidates.length; j++) {
                        SqSolution newSolution = new SqSolution();
                        newSolution.dTransitionIndex = dTranCandidates[i];
                        newSolution.rTransitionIndex = rTranCandidates[j];

                        newSolution.logL = sqmodel
                            .setDtransition(newSolution.dTransitionIndex)
                            .setRtransition(newSolution.rTransitionIndex)
                            .calcLogL(sigmaB, sigmaA);

                        // We let sqmodel determine when a dTran:rTran
                        // combination is valid.  It lets us know the combo
                        // is invalid (too few event or baseline points) by
                        // returning NaN for logL.
                        if (Double.isNaN(newSolution.logL)) {
                            continue;
                        }

                        solMagDrop = JocularUtils.calcMagDrop(sqmodel.getB(), sqmodel.getA());

                        if (Double.isNaN(solMagDrop) || solMagDrop < minMagDrop || solMagDrop > maxMagDrop) {
                            continue;
                        }

                        numValidTranPairs++;

                        newSolution.D = sqmodel.getDsolution();
                        newSolution.R = sqmodel.getRsolution();
                        newSolution.B = sqmodel.getB();
                        newSolution.A = sqmodel.getA();
                        newSolution.magDrop = solMagDrop;
                        newSolution.sigmaB = sigmaB;
                        newSolution.sigmaA = sigmaA;
                        newSolution.kFactor = sqmodel.getkFactor();
                        newSolution.aicc = JocularUtils.aicc(newSolution.logL, newSolution.kFactor, n);

                        sqsolutions.add(newSolution);
                    }
                }
                return sqsolutions;
            }
        };
    }
}

