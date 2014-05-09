package joculartest;

import java.nio.file.Paths;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Test;
import utils.Observation;
import utils.SampleDataGenerator;

public class ObservationTest {

    public ObservationTest() {
    }

    final double SOME_D_EVENT_VALUE = 43.75;
    final double SOME_R_EVENT_VALUE = 297.2;
    final double SOME_A_VALUE = 2.91;
    final double SOME_B_VALUE = 13.89;
    final double SOME_SIGMAA_VALUE = 1.0;
    final double SOME_SIGMAB_VALUE = 2.0;
    final int SOME_DATA_LENGTH = 1254;

    @Test
    public void demoToString() {
        Observation anObs = new Observation("demoOfToString",
                                            Paths.get("some-directory", "some-filename")
        );
        System.out.println(anObs);
    }

    @Test
    public void settingTrimPoints_shouldChangeObsDataLengthAndReadingNumbers() {

        // Create a non-trivial 'observation' using a SampleDataGenerator ...
        SampleDataGenerator dataGen = new SampleDataGenerator("testSetTwo");

        dataGen.setDevent(SOME_D_EVENT_VALUE)
                .setRevent(SOME_R_EVENT_VALUE)
                .setSigmaA(SOME_SIGMAA_VALUE)
                .setSigmaB(SOME_SIGMAB_VALUE)
                .setAintensity(SOME_A_VALUE)
                .setBintensity(SOME_B_VALUE)
                .setNumDataPoints(SOME_DATA_LENGTH)
                .setParams();

        Observation trialObs = dataGen.build();

        /**
         * We now have an Observation with 3 columns of data, each of length
         * SOME_DATA_LENGTH.
         *
         * We are going to apply two trim operations. The semantics of trim
         * positions is that they are 'inclusive'. So readingNumber values from
         * (and including) leftTrimPosition to (and including) rightTrimPosition
         * will be included in obsData[]
         */
        
        int firstRdgNbrValue = 0;
        int lastRdgNbrValue = trialObs.lengthOfDataColumns - 1;

        int leftTrimPosition = firstRdgNbrValue + 10;  // Trim 10 off the left end
        int rightTrimPosition = lastRdgNbrValue - 10; //  and 10 off the right end.

        int correctLengthOfObsData = rightTrimPosition - leftTrimPosition + 1;

        trialObs.setLeftTrimPoint(leftTrimPosition);
        trialObs.setRightTrimPoint(rightTrimPosition);

        int firstRdgNbrIndex = 0;
        int finalRdgNbrIndex = trialObs.readingNumbers.length - 1;

        // Check inclusivity.
        assertThat(trialObs.readingNumbers[firstRdgNbrIndex]).isEqualTo(leftTrimPosition);
        assertThat(trialObs.readingNumbers[finalRdgNbrIndex]).isEqualTo(rightTrimPosition);

        assertThat(trialObs.obsData.length).isEqualTo(correctLengthOfObsData);

        System.out.println(trialObs);
    }

}
