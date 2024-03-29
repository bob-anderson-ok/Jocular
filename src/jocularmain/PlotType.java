package jocularmain;

import java.util.HashMap;

public enum PlotType {

    // If any entry is added to this list, it must be added
    // to the HashMap as well.
    // The first  parameter is the line color   ...
    // The second parameter is the symbol color ...
    // The third  parameter is the name for the series --- synchronize with DataType.
    STYLE_ObsData("gray", "black", "ObsData"),
    STYLE_ObsPoints("transparent", "black", "ObsPoints"),
    STYLE_obsData("lightgray", "gray", "obsData"),
    STYLE_obsPoints("transparent", "gray", "obsPoints"),
    STYLE_Sample("red", "transparent", "Sample"),
    STYLE_Solution("blue", "transparent", "Solution"),
    STYLE_SubframeBand("green","transparent","SubframeBand"),
    STYLE_Secondary("transparent", "red", "Secondary"),
    STYLE_UpperEnv("brown","transparent","UpperEnvelope"),
    STYLE_LowerEnv("brown","transparent","LowerEnvelope");

    private static final HashMap<String, PlotType> mapSeriesNameToPlotType;

    static {
        mapSeriesNameToPlotType = new HashMap<String, PlotType>();
        mapSeriesNameToPlotType.put("ObsData", STYLE_ObsData);
        mapSeriesNameToPlotType.put("ObsPoints", STYLE_ObsPoints);
        mapSeriesNameToPlotType.put("obsData", STYLE_obsData);
        mapSeriesNameToPlotType.put("obsPoints", STYLE_obsPoints);
        mapSeriesNameToPlotType.put("UnderlyingLightCurve", STYLE_Sample);
        mapSeriesNameToPlotType.put("Solution", STYLE_Solution);
        mapSeriesNameToPlotType.put("SubframeBand", STYLE_SubframeBand);
        mapSeriesNameToPlotType.put("Secondary", STYLE_Secondary);
        mapSeriesNameToPlotType.put("UpperEnvelope", STYLE_UpperEnv);
        mapSeriesNameToPlotType.put("LowerEnvelope", STYLE_LowerEnv);
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
