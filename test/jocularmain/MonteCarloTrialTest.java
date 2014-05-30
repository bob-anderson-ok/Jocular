package jocularmain;

import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;
import utils.MonteCarloResult;

public class MonteCarloTrialTest {
    
    public MonteCarloTrialTest() {
    }

    private TrialParams trialParams = new TrialParams();
    
    @Test
    public void calcHistogram_shouldReturnHistogramOfCorrectLength() {        
        trialParams.sampleWidth = 100;
        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        MonteCarloResult result =monteCarloTrial.calcHistogram();
        assertThat(result.histogram.length).isEqualTo(100);
    }
    
    @Test
    public void debugCalcHistogram() {
        trialParams.sampleWidth = 100;
        trialParams.baselineLevel = 10;
        trialParams.eventLevel = 0.0;
        //trialParams.sigmaB = 3.54;  // Threshold of subframe timing
        //trialParams.sigmaA = 3.54;  // Threshold of subframe timing
        //trialParams.sigmaB = 2.5; // SNR 4
        //trialParams.sigmaA = 2.5; // SNR 4
        trialParams.sigmaB = 6.0;
        trialParams.sigmaA = 2.0;
        trialParams.numTrials = 400000;
        trialParams.mode = MonteCarloMode.LEFT_EDGE;
        
        MonteCarloTrial monteCarloTrial = new MonteCarloTrial(trialParams);
        MonteCarloResult result =monteCarloTrial.calcHistogram();
        
        System.out.println("histogram: ");
        for ( int i = 0; i<result.histogram.length;i++) {
            System.out.println(String.format("%3d %,8d", i, result.histogram[i]));
        }
        
    } 
    
}
