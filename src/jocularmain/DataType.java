package jocularmain;

public enum DataType { 
    
    // The names here must match the names in PlotType enum because that's how
    // we lookup the line and symbol color for a DataType
    
    OBSDATA("ObsData"),               // Observation data points
    SAMPLE("Sample"),                 // Light curve underlying artificially generated sample data
    SOLUTION("Solution"),             // Solution light curve
    SECONDARY("Secondary"),           // Secondary observation data points
    SUBFRAME_BAND("SubframeBand"),    // Outline of subframe-timing band
    UPPER_ENVELOPE("UpperEnvelope"),
    LOWER_ENVELOPE("LowerEnvelope");
    
    private String dataName;
    
    DataType(String dataName) {
        this.dataName = dataName;
    }
    
    public String getName() {
        return dataName;
    }
}
