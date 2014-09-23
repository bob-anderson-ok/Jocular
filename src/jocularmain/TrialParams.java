package jocularmain;

public class TrialParams {
    public int numTrials = 0;
    public int sampleWidth = 0;
    public double sigmaB = Double.NaN;
    public double sigmaA = Double.NaN;
    public double baselineLevel = Double.NaN;
    public double eventLevel = Double.NaN;
    public MonteCarloMode mode;
    public double acf2;       // Pearson r (acf coeffient 2)
    public double acfn;       // acf coefficent n
    public int coherenceTime; // number of acf terms > acfn
}
