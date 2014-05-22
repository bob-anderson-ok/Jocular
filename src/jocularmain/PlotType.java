package jocularmain;

import java.util.HashMap;

public enum PlotType {

    // If any entry is added to this list, it must be added
    // to the 
    STYLE_ObsData("gray", "black", "ObsData"),
    STYLE_ObsPoints("transparent", "black", "ObsPoints"),
    STYLE_obsdata("lightgray", "gray", "obsdata"),
    STYLE_obsPoints("transparent", "gray", "obsPoints"),
    STYLE_Sample("red", "transparent", "Sample"),
    STYLE_Solution("blue", "transparent", "Solution");

    private static final HashMap<String, PlotType> mapSeriesNameToPlotType;

    static {
        mapSeriesNameToPlotType = new HashMap<String, PlotType>();
        mapSeriesNameToPlotType.put("ObsData", STYLE_ObsData);
        mapSeriesNameToPlotType.put("ObsPoints", STYLE_ObsPoints);
        mapSeriesNameToPlotType.put("obsdata", STYLE_obsdata);
        mapSeriesNameToPlotType.put("obsPoints", STYLE_obsPoints);
        mapSeriesNameToPlotType.put("Sample", STYLE_Sample);
        mapSeriesNameToPlotType.put("Solution", STYLE_Solution);
    }

    private String lineColor;
    private String symbolColor;
    private String seriesName;

    PlotType(String lineColor, String symbolColor, String seriesName) {
        this.lineColor = lineColor;
        this.symbolColor = symbolColor;
        this.seriesName = seriesName;
    }

    public static PlotType lookup(String seriesName) {
        return mapSeriesNameToPlotType.get(seriesName);
    }

    public String lineColor() {
        return lineColor;
    }

    public String symbolColor() {
        return symbolColor;
    }

    public String seriesName() {
        return seriesName;
    }

}
