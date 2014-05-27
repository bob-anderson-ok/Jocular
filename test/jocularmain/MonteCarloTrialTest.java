package jocularmain;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;

public class MonteCarloTrialTest {
    
    public MonteCarloTrialTest() {
    }

    private TrialParams trialParams = new TrialParams();
    
    @Test
    public void calcHistogram_shouldReturnHistogramOfCorrectLength() {        
        trialParams.sampleWidth = 100;
        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        int[] histogram = monteCarloTrial.calcHistogram();
        assertThat(histogram.length).isEqualTo(100);
    }
    
    @Test
    public void debugCalcHistogram() {
        trialParams.sampleWidth = 20;
        trialParams.baselineLevel = 10;
        trialParams.eventLevel = 0.0;
        trialParams.sigmaB = 4;
        trialParams.sigmaA = 4;
        trialParams.numTrials = 40000;
        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        int[] histogram = monteCarloTrial.calcHistogram();
    } 
    
}
