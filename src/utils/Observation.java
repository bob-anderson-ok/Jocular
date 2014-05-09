package utils;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * An occultation observation, as recorded by Tangra or Limovie, begins with a
 * set of comment lines, followed by a line containing column heading
 * (identified by a unique leading string), followed by lines with comma
 * separated values.
 * <p>
 * One column (in the case of Tangra) or three columns (in the case of Limovie)
 * may sometimes hold timestamp values for that reading. When present, these are
 * extracted and placed in <b>readingTimes</b> by the <b>load()</b> method.
 * <p>
 * During processing (or even by manual edits), special <b>addedInfo</b> lines
 * can be added to the file in the comment area. These lines are identified by a
 * leading character of # followed by a parameter name and a value and are used
 * to allow the user (or Jocular) to 'annotate' the file with important
 * processing parameters such as the time delta between readings, the date/time
 * for the first reading, trim points, asteroid speed parameters, etc. It makes
 * these values 'sticky' between sessions.
 *
 */
public class Observation {

    public Observation(String observationName, Path filePath) {
        this.observationName = observationName;
        this.filePath = filePath;
        this.fileName = this.filePath.getFileName();
    }

    private String observationName;
    private Path fileName;
    private Path filePath;

    /**
     * Holds data from all columns in the input file that hold valid numeric
     * values.
     */
    public double[][] columnData;        // clients: must treat as read-only
    public int numberOfDataColumns = 0;  // clients: must treat as read-only
    public int lengthOfDataColumns = 0;  // clients: must treat as read-only

    /**
     * Holds data from the column designated by the user as the observation data
     * (specified in <b>obsDataColumn</b>).
     * <p>
     * If left and/or right trim settings are present, they will have been
     * applied so that this array only holds readings that the user wants to be
     * included in the analysis.
     * <p>
     * If the user runs a detrending or filter process on this observation, the
     * resulting values overwrite this array. As a result, this array always
     * holds the the effect of most recent trimming, detrending, and filtering
     * processes.
     * <p>
     * To rollback a detrend or filter operation, call the
     * <b>reloadObsAndSecData()</b>
     * method. It will reload <b>obsData</b> and <b>secData</b> using the
     * current
     * <b>obsDataColumn</b> and <b>secDataColumn</b> values, applying the
     * current trim position values as it does so.
     * <p>
     * Setting a value into <b>obsDataColumn</b> (via its 'setter') will also
     * cause previous detrend or filter operation results to be overwritten.
     * <p>
     * Changing a trim setting will cause the loss of detrend or filter
     * operations. If observation data has been so modified, this is detected by
     * the trim 'setters' and causes a warning exception to be thrown.
     *
     */
    public double[] obsData;  // y axis values, users sometimes modify
    private int obsDataColumn;

    /**
     * index into the untrimmed data. Provides x axis values for <b>obsData</b>
     * and <b>secData</b>
     */
    public int[] readingNumbers;   // x axis values for obsData and secData

    public double[] secData;       // y axis values, users sometimes modify  
    private int secDataColumn = 0;
    private boolean secDataColumnHasBeenSelected = false;

    /**
     * these are the headings that correspond to the columns in the input file
     * that contained numeric values.
     */
    public String[] columnHeadings;  // users: must treat as read-only

    private ArrayList<String> readingTimes = new ArrayList<>();
    private ArrayList<String> comments = new ArrayList<>();
    public ArrayList<String> addedInfo = new ArrayList<>();

    /**
     * has a setter so that and <b>addedInfo</b> can be added whenever the user
     * sets or changes this value.
     */
    private double timePerReading = Double.NaN; // getter and setter

    /**
     * has a setter so that and <b>addedInfo</b> can be added whenever the user
     * sets or changes this value.
     */
    private double firstReadingTimestamp = Double.NaN;

    private int leftTrimPoint = Integer.MIN_VALUE;
    private int rightTrimPoint = Integer.MAX_VALUE;

    @Override
    public String toString() {
        String ans = "observationName: " + observationName + "\n";
        if (columnData == null) {
            ans = ans.concat("  ... contains no input data\n");
        } else {
            ans = ans.concat("  number of data columns: " + columnData.length + "\n");
            ans = ans.concat("  length of data columns: " + columnData[0].length + "\n");
            if (readingTimes.isEmpty()) {
                ans = ans.concat("  no reading times present\n");
            } else {
                ans = ans.concat("  readings times are present\n");

            }
            ans = ans.concat("  number of comment lines: " + comments.size() + "\n");
            ans = ans.concat("  number of addInfo lines: " + addedInfo.size() + "\n");
        }
        if (leftTrimPoint != Integer.MIN_VALUE) {
            ans = ans.concat("  leftTrimPoint: " + leftTrimPoint + "\n");
        }
        if (rightTrimPoint != Integer.MAX_VALUE) {
            ans = ans.concat("  rightTrimPoint: " + rightTrimPoint + "\n");
        }
        if (obsData == null) {
            ans = ans.concat("  obsData is empty\n");
        } else {
            ans = ans.concat("  obsData has length: " + obsData.length);
            ans = ans.concat(", labelled: " + columnHeadings[obsDataColumn]);
            if (obsDataUnchanged()) {
                ans = ans.concat(" and is unchanged\n");
            } else {
                ans = ans.concat(" and has been modified!\n");
            }
        }
        if (secData == null) {
            ans = ans.concat("  secData is empty\n");
        } else {
            if (secDataColumnHasBeenSelected) {
                ans = ans.concat("  secData has length: " + secData.length);
                ans = ans.concat(", labelled: " + columnHeadings[secDataColumn]);
                if (secDataUnchanged()) {
                    ans = ans.concat(" and is unchanged\n");
                } else {
                    ans = ans.concat(" and has been modified!\n");
                }
            } else {
                ans = ans.concat("  no column selection for secData is available\n");
            }
        }
        if (!Double.isNaN(timePerReading)) {
            ans = ans.concat("  timePerReading: " + timePerReading + "\n");
        }
        if (!Double.isNaN(firstReadingTimestamp)) {
            ans = ans.concat("  firstReadingTimestamp: " + firstReadingTimestamp + "\n");
        }
        if (filePath == null) {
            ans = ans.concat("  fileName: <not present>\n");
            ans = ans.concat("  filePath: <not present>\n");
        } else {
            ans = ans.concat("  fileName: " + fileName.toString() + "\n");
            ans = ans.concat("  filePath: " + filePath.toString() + "\n");
        }

        return ans;
    }

    /**
     *
     * @return true if obsData has not been modified by the client. It contains
     * the values provided by the loaded data set unchanged.
     */
    public boolean obsDataUnchanged() {
        if (obsData == null) {
            throw new NullPointerException("obsData not available yet");
        }
        for (int i = 0; i < readingNumbers.length; i++) {
            if (obsData[i]
                    != columnData[obsDataColumn][readingNumbers[i]]) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return true if secData has not been modified by the client. It contains
     * the values provided by the loaded data set unchanged.
     */
    public boolean secDataUnchanged() {
        if (secData == null) {
            throw new NullPointerException("secData not available yet");
        }
        for (int i = 0; i < readingNumbers.length; i++) {
            if (secData[i]
                    != columnData[secDataColumn][readingNumbers[i]]) {
                return false;
            }
        }
        return true;
    }

    public String getfilePath() {
        if (filePath == null) {
            return "<not present>";
        } else {
            return filePath.toString();
        }
    }

    public String getfileName() {
        if (fileName == null) {
            return "<not present>";
        } else {
            return fileName.toString();
        }
    }

    public String getobservationName() {
        return observationName;
    }

    /**
     * sets the column of input data that is being used by <b>obsData[]</b>
     * and copies that data from the input (<b>obsData[][]</b>). This can be
     * used to remove modifications of <b>obsData[]</b> by invoking this method
     * with the current value of <b>obsDataColumn</b>
     */
    public void setobsDataColumn(int column) {
        if (column < 0 || column > numberOfDataColumns) {
            throw new IllegalArgumentException(
                    "obsDataColumn value of " + column + " is out of range"
            );
        }
        obsDataColumn = column;
        obsData = new double[readingNumbers.length];
        for (int i = 0; i < readingNumbers.length; i++) {
            obsData[i] = columnData[obsDataColumn][readingNumbers[i]];
        }
    }

    public int getobsDataColumn() {
        return obsDataColumn;
    }

    /**
     * sets the column of input data that is being used by <b>osecData[]</b>
     * and copies that data from the input (<b>obsData[][]</b>). This can be
     * used to remove modifications of <b>secData[]</b> by invoking this method
     * with the current value of <b>secDataColumn</b>
     */
    public void setsecDataColumn(int column) {
        if (column < 0 || column > numberOfDataColumns) {
            throw new IllegalArgumentException(
                    "secDataColumn value of " + column + " is out of range"
            );
        }
        secDataColumn = column;
        secDataColumnHasBeenSelected = true;
        secData = new double[readingNumbers.length];
        for (int i = 0; i < readingNumbers.length; i++) {
            secData[i] = columnData[secDataColumn][readingNumbers[i]];
        }
    }

    public void deselectSecDataColumn() {
        secDataColumnHasBeenSelected = false;
    }

    public int getsecDataColumn() {
        return secDataColumn;
    }

    private void validateTrimValue(int trimValue) {
        if (trimValue < 0 || trimValue > lengthOfDataColumns) {
            throw new IllegalArgumentException("trim point value = "
                    + trimValue + " is outside range of 0:" + lengthOfDataColumns);
        }
    }

    public void setLeftTrimPoint(int newTrimValue) {
        validateTrimValue(newTrimValue);
        leftTrimPoint = newTrimValue;
        reloadObsAndSecData();
    }

    public void setRightTrimPoint(int newTrimValue) {
        validateTrimValue(newTrimValue);
        rightTrimPoint = newTrimValue;
        reloadObsAndSecData();
    }

    public void reloadObsAndSecData() {
        // Trim changes are applied to readingNumbers first.
        int begin = leftTrimPoint;
        if (begin == Integer.MIN_VALUE) { // not set by client
            begin = 0;
        }

        int end = rightTrimPoint;
        if (end == Integer.MAX_VALUE) { // not set by client
            end = lengthOfDataColumns - 1;
        }

        readingNumbers = new int[end - begin + 1];
        for (int i = 0; i < readingNumbers.length; i++) {
            readingNumbers[i] = i + begin;
        }

        setobsDataColumn(obsDataColumn);
        setsecDataColumn(secDataColumn);

    }

    public void setTimePerReading(double timePerReading) {
        this.timePerReading = timePerReading;
        // TODO Handle addedInfo update
        throw new UnsupportedOperationException("Not completely implemented");
    }

    public void setFirstReadingTimestamp(double firstReadingTimestamp) {
        this.firstReadingTimestamp = firstReadingTimestamp;
        // TODO Handle addedInfo update
        throw new UnsupportedOperationException("setFirstReadingTimestamp() not completely implemented");
    }
    
    public void loadDataFromFile() {
        // TODO implement
        throw new UnsupportedOperationException("loadDataFromFile() not implemented");
    }
}
