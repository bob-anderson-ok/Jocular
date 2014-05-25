package joculartest;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.offset;
import org.fest.assertions.data.Offset;
import org.junit.runner.RunWith;
import utils.JocularUtils;

@RunWith(JUnitParamsRunner.class)
public class RandUtilsTest {

    public RandUtilsTest() {
    }

    final int SOME_POSITIVE_INT = 167;
    final double SOME_SIGMA_VALUE = 2.789;

    @Test
    public void generateGaussianNoise_shouldReturnArrayOfSpecifiedLength() {
        double[] result = JocularUtils.generateGaussianNoise(SOME_POSITIVE_INT, SOME_SIGMA_VALUE);

        assertThat(result.length).isEqualTo(SOME_POSITIVE_INT);
    }

    @Test
    public void calcSigma_shouldApproximateSigmaGivenToGetGaussianNoise() {
        double[] gaussNoise = JocularUtils.generateGaussianNoise(100000, SOME_SIGMA_VALUE);

        double sigmaCalculated = JocularUtils.calcSigma(gaussNoise);

        Offset<Double> errorAllowed = offset(SOME_SIGMA_VALUE / 100.0);
        assertThat(sigmaCalculated).isEqualTo(SOME_SIGMA_VALUE, errorAllowed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calcSigma_shouldThrowExceptionIfNotEnoughValues() {
        double[] values = new double[1]; // At least 2 are needed.
        double sigmaCalculated = JocularUtils.calcSigma(values);
    }

    @Test
    public void setGaussianGeneratorSeed_shouldGiveRepeatableGaussianNoise() {
        
        int arraySize = 100;
        double sigma = 1.0;

        JocularUtils.setGaussianGeneratorSeed(1234L);
        double[] gaussNoiseOne = JocularUtils.generateGaussianNoise(arraySize, sigma);
        
        JocularUtils.setGaussianGeneratorSeed(1234l);
        double[] gaussNoiseTwo = JocularUtils.generateGaussianNoise(arraySize, sigma);

        boolean vectorsAreEqual = true;
        for (int i = 0; i < arraySize; i++) {
            if (gaussNoiseOne[i] != gaussNoiseTwo[i]) {
                vectorsAreEqual = false;
                break;
            }
        }
        
        assertThat(vectorsAreEqual).isTrue();
    }
    
    @Test
    public void calcMagDrop_shouldReturnOne_whenBandAareSpecialCase() {
        double B = 2.12345;
        double A = B / 2.512;
        
        assertThat(JocularUtils.calcMagDrop(B, A)).isEqualTo(1.0, offset(1e-7));
    }
    
    @Test
    public void calcMagDrop_shouldReturnNaN_whenBandOrAisInvalid() {
        double B = -1.0;
        double A = B / 2.512;
        
        assertThat(JocularUtils.calcMagDrop(B, A)).isEqualTo(Double.NaN);
        
        B = 1.0;
        assertThat(JocularUtils.calcMagDrop(B, A)).isEqualTo(Double.NaN);
        
        B = 1.0;
        A = B + 0.0001;
        assertThat(JocularUtils.calcMagDrop(B, A)).isEqualTo(Double.NaN);
    }

}
