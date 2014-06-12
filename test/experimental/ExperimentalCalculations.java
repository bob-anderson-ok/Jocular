package experimental;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.offset;

public class ExperimentalCalculations {

    public ExperimentalCalculations() {
    }

    @Test
    public void showThreadCount() {
        System.out.println("Current thread count is " + Thread.activeCount());
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of available processors is " + processors);
    }
    
    @Test
    public void chiSquaredCum() {
        ChiSquaredDistribution chi2Dist = new ChiSquaredDistribution(6);
        double chi2cum = chi2Dist.cumulativeProbability(4);
        System.out.println("chi2cum: " + chi2cum);
    }
    
    @Test
    public void chiSquare() {
        ChiSquaredDistribution chi2Dist = new ChiSquaredDistribution(100);
        double bob1 = chi2Dist.density(100);
        double bob2 = chi2Dist.density(99);
        System.out.println("bob1: " + bob1);
        System.out.println("bob2: " + bob2);
    }
    
    @Test
    public void calculateSubframeTimingWindow() {
        double B = 12.0;
        double A = 2.0;
        int n = 100;

        double sigmaB = (B - A) / (2.0 * Math.sqrt(2.0)) / 2.0;
        double sigmaA = sigmaB;

        double MB = calcBsideSubframeBoundary(n, sigmaB, sigmaA, B, A);
        double MA = calcAsideSubframeBoundary(n, sigmaB, sigmaA, B, A);

        System.out.println(String.format("MB = %.3f  MA = %.3f", MB, MA));
        System.out.println(String.format("subframe timing window = %.2f", MB - MA));

        assertThat(MB).isEqualTo(9.448, offset(0.0005));
        assertThat(MA).isEqualTo(4.552, offset(0.0005));
    }

    @Test
    public void sampleCorr() {
        int n = 100;
        System.out.println(String.format("k=1: %.3f", AicCorr(n,1)));
        System.out.println(String.format("k=2: %.3f", AicCorr(n,2)));
        System.out.println(String.format("delta: %.3f", AicCorr(n,2)-AicCorr(n,1)));
    }

    private double AicCorr(int n, int k) {
        return 2.0 * k +(2.0 * k * (k + 1)) / (n - k - 1);
    }

    private double calcbAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((B - M) / sigmaB) * ((B - M) / sigmaB);
        double term2 = 1.0 - (1.0 - (sigmaA / sigmaB)) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    private double calcaAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((A - M) / sigmaA) * ((A - M) / sigmaA);
        double term2 = (sigmaB / sigmaA) - ((sigmaB / sigmaA) - 1.0) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    public double calcBsideSubframeBoundary(int n,double sigmaB, double sigmaA, double B, double A) {
        double aiccDelta = AicCorr(n,2) - AicCorr(n,1);  // Use this for finite sample size
        // aiccDelta = 2.0; // Use this for infinite sample size
        double M = B;
        double delta = (B - A) * 0.0001;
        while (calcbAIC(sigmaB, sigmaA, B, A, M) < aiccDelta) {
            M -= delta;
            if (M <= A) {
                return A;
            }
        }
        return M;
    }

    public double calcAsideSubframeBoundary(int n, double sigmaB, double sigmaA, double B, double A) {
        double aiccDelta = AicCorr(n,2) - AicCorr(n,1);  // Use this for finite sample size
        // aiccDelta=2.0; // Use this for infinite sample size
        double M = A;
        double delta = (B - A) * 0.0001;
        while (calcaAIC(sigmaB, sigmaA, B, A, M) < aiccDelta) {
            M += delta;
            if (M >= B) {
                return B;
            }
        }
        return M;
    }
}
