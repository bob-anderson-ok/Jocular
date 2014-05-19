package utils;

import java.util.Random;

public interface JocularUtils {
    
    Random randomGenerator = new Random();
    
    static void setGaussianGeneratorSeed(long newSeed) {
        randomGenerator.setSeed(newSeed);
    }
    
    static double gaussianVariate(double sigma) {
        return randomGenerator.nextGaussian() * sigma;
    }

    static double[] generateGaussianNoise(int length, double sigma) {

        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            result[i] = randomGenerator.nextGaussian() * sigma;
        }

        return result;
    }

    static double calcSigma(double[] values) {

        if (values.length < 2) {
            throw new IllegalArgumentException(
                    "calcSigma needs at least 2 values, " + values.length + " given."
            );
        }

        double sumOfValues = 0.0;

        for (double v : values) {
            sumOfValues += v;
        }

        double averageValue = sumOfValues / values.length;
        double squaredDifference = 0.0;

        for (double v : values) {
            squaredDifference += (v - averageValue) * (v - averageValue);
        }

        return Math.sqrt(squaredDifference / (values.length - 1));
    }

}