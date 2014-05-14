package utils;

import java.nio.file.Paths;
import static utils.RandUtils.gaussianVariate;

public class SampleDataGenerator {

    private final int INVALID_VALUE = -1;

    private double dEventTime = Double.NaN;
    private double rEventTime = Double.NaN;
    private double sigmaA = Double.NaN;
    private double sigmaB = Double.NaN;
    private double Aintensity = Double.NaN;
    private double Bintensity = Double.NaN;
    private int numDataPoints = INVALID_VALUE;

    private Observation filledObservation;

    public SampleDataGenerator(String testSetName) throws IllegalStateException {
        filledObservation = new Observation(
            testSetName,
            Paths.get("..", "internally-generated-data")
        );
    }

    public SampleDataGenerator setDevent(double dEventTime) {
        this.dEventTime = dEventTime;
        return this;
    }

    public SampleDataGenerator setRevent(double rEventTime) {
        this.rEventTime = rEventTime;
        return this;
    }

    public SampleDataGenerator setSigmaA(double sigmaA) {
        this.sigmaA = sigmaA;
        return this;
    }

    public SampleDataGenerator setAintensity(double Aintensity) {
        this.Aintensity = Aintensity;
        return this;
    }

    public SampleDataGenerator setBintensity(double Bintensity) {
        this.Bintensity = Bintensity;
        return this;
    }

    public SampleDataGenerator setSigmaB(double sigmaB) {
        this.sigmaB = sigmaB;
        return this;
    }

    public SampleDataGenerator setNumDataPoints(int numDataPoints) {
        this.numDataPoints = numDataPoints;
        filledObservation.readingNumbers = new int[numDataPoints];
        for (int i = 0; i < numDataPoints; i++) {
            filledObservation.readingNumbers[i] = i;
        }
        filledObservation.columnData = new double[3][numDataPoints];
        filledObservation.numberOfDataColumns = 3;
        filledObservation.lengthOfDataColumns = numDataPoints;
        filledObservation.obsData = new double[numDataPoints];
        filledObservation.setobsDataColumn(0);
        filledObservation.secData = new double[numDataPoints];
        filledObservation.setsecDataColumn(1);
        filledObservation.columnHeadings = new String[]{"modelData", "Blevel", "Alevel"};
        filledObservation.readingNumbers = new int[numDataPoints];
        for (int i = 0; i < numDataPoints; i++) {
            filledObservation.readingNumbers[i] = i;
        }
        return this;
    }

    public Observation setParams() {
        return filledObservation;
    }

    public Observation build() {
        if (Double.isNaN(sigmaA)) {
            throw new IllegalStateException("sigmaA is missing");
        }
        if (Double.isNaN(sigmaB)) {
            throw new IllegalStateException("sigmaB is missing");
        }
        if (Double.isNaN(Aintensity)) {
            throw new IllegalStateException("Aintensity is missing");
        }
        if (Double.isNaN(Bintensity)) {
            throw new IllegalStateException("Bintensity is missing");
        }
        if (Double.isNaN(dEventTime) && Double.isNaN(rEventTime)) {
            throw new IllegalStateException("dEventTime and rEventTime is missing");
        }
        if (numDataPoints < 5) {
            throw new IllegalStateException("numDataPoints is less than 5");
        }

        if (sigmaA < 0.0) {
            throw new IllegalStateException("sigmaA is negative");
        }
        if (sigmaB < 0.0) {
            throw new IllegalStateException("sigmaB is negative");
        }

        // All parameters needed are available and valid.
        computeSampleDataVectorsAndFillObsAndSecData();

        return filledObservation;
    }

    private void computeSampleDataVectorsAndFillObsAndSecData() {
        filledObservation.columnData[0] = RandUtils.generateGaussianNoise(numDataPoints, sigmaB);
        filledObservation.columnData[1] = RandUtils.generateGaussianNoise(numDataPoints, sigmaB);
        filledObservation.columnData[2] = RandUtils.generateGaussianNoise(numDataPoints, sigmaA);

        for (int i = 0; i < filledObservation.lengthOfDataColumns; i++) {
            filledObservation.columnData[0][i] += Bintensity;
            filledObservation.columnData[1][i] += Bintensity;
            filledObservation.columnData[2][i] += Aintensity;
        }

        // Fill obsData[] initially as though it contained no event or any transition points. Later
        // we'll fix that.
        System.arraycopy(filledObservation.columnData[0], 0,
                         filledObservation.obsData, 0,
                         numDataPoints);

        // The standard secondary data vector (for generated test data) is a B level straight line
        // with sigmaB noise.
        System.arraycopy(filledObservation.columnData[1], 0,
                         filledObservation.secData, 0,
                         numDataPoints);

        double[] eventData = RandUtils.generateGaussianNoise(numDataPoints, sigmaA);
        for (int i = 0; i < filledObservation.lengthOfDataColumns; i++) {
            eventData[i] += Aintensity;
        }

        int dTransitionIndex = -1;  // Set out-of-range to the left
        if (!Double.isNaN(dEventTime)) {
            dTransitionIndex = calculateTransitionIndex(dEventTime);
        }

        int rTransitionIndex = filledObservation.lengthOfDataColumns;  // Set out-of-range to the right
        if (!Double.isNaN(rEventTime)) {
            rTransitionIndex = calculateTransitionIndex(rEventTime);
        }

        for (int i = 0; i < filledObservation.lengthOfDataColumns; i++) {
            if (i > dTransitionIndex && i < rTransitionIndex) {
                filledObservation.obsData[i] = eventData[i];
            }
        }

        if (isInDataRange(dTransitionIndex)) {
            // Add D transition value (mValue) and corresponding noise level (mSigma)
            double factor = 1.0 - (dEventTime - Math.floor(dEventTime));
            double mValue = Bintensity - (Bintensity - Aintensity) * factor;
            double mSigma = sigmaB - (sigmaB - sigmaA) * factor;
            double dTransitionValue = gaussianVariate(mSigma) + mValue;
            filledObservation.obsData[dTransitionIndex] = dTransitionValue;
        }

        if (isInDataRange(rTransitionIndex)) {
            // Add R transition value (mValue) and corresponding noise level (mSigma)
            double factor = 1.0 - (rEventTime - Math.floor(rEventTime));
            double mValue = Aintensity + (Bintensity - Aintensity) * factor;
            double mSigma = sigmaA + (sigmaB - sigmaA) * factor;
            double rTransitionValue = gaussianVariate(mSigma) + mValue;
            filledObservation.obsData[rTransitionIndex] = rTransitionValue;
        }
        
        // Copy obsData[] to columnData[0][]
        System.arraycopy(filledObservation.obsData, 0,
                         filledObservation.columnData[0], 0,
                         numDataPoints);
    }

    private int calculateTransitionIndex(double eventTime) {
        int transitionIndex;
        if (eventTime == Math.ceil(eventTime)) {
            transitionIndex = (int) Math.ceil(eventTime) + 1;
        } else {
            transitionIndex = (int) Math.ceil(eventTime);
        }
        return transitionIndex;
    }

    private boolean isInDataRange(int index) {
        return index >= 0 && index < filledObservation.lengthOfDataColumns;
    }

}
