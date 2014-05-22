package experimental;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExperimentalCalculations {

    public ExperimentalCalculations() {
    }

    @Test
    public void calculateSubframeTimingWindow() {
        double B = 12.0;
        double A = 2.0;

        double sigmaB = (B-A) / (2.0 * Math.sqrt(2.0));
        double sigmaA = sigmaB/100.0;

        double MB = calculateMB(sigmaB, sigmaA, B, A);
        double MA = calculateMA(sigmaB, sigmaA, B, A);
        
        System.out.println(String.format("MB = %.3f  MA = %.3f", MB, MA));
        System.out.println(String.format("subframe timing window = %.2f", MB - MA));
    }

    public double calcbAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((B - M) / sigmaB) * ((B - M) / sigmaB);
        double term2 = 1.0 - (1.0 - (sigmaA / sigmaB)) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    public double calcaAIC(double sigmaB, double sigmaA, double B, double A, double M) {
        double term1 = ((A - M) / sigmaA) * ((A - M) / sigmaA);
        double term2 = (sigmaB / sigmaA) - ((sigmaB / sigmaA) - 1.0) * ((B - M) / (B - A));
        return term1 - 2.0 * Math.log(term2);
    }

    public double calculateMB(double sigmaB, double sigmaA, double B, double A) {
        double M = B;
        double delta = 0.0001;
        while (calcbAIC(sigmaB, sigmaA, B, A, M) < 2.0) {
            M -= delta;
            if (M <= A) {
                return A;
            }
        }
        return M;
    }
    
    public double calculateMA(double sigmaB, double sigmaA, double B, double A) {
        double M = A;
        double delta = 0.0001;
        while (calcaAIC(sigmaB, sigmaA, B, A, M) < 2.0) {
            M += delta;
            if (M >= B) {
                return B;
            }
        }
        return M;
    }
}
