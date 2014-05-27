package jocularmain;

public enum DataType { 
    OBSDATA("ObsData"),       // Observation data points
    SAMPLE("Sample"),        // Light curve underlying artificially generated sample data
    SOLUTION("Solution"),      // Solution light curve
    SECONDARY("Secondary"),     // Secondary observation data points
    SUBFRAME_BAND("SubframeBand");  // Outline of subframe-timing band
    
    private String dataName;
    
    DataType(String dataName) {
        this.dataName = dataName;
    }
    
    public String getName() {
        return dataName;
    }
}
