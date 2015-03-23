package org.reactome.server.statistics.properties;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * All chart types
 */
public enum  ChartTypes {
    //TODO need to define all chart types
    REQUEST_COMPARISON("chart1"),
    DATA_SET_SIZE_COMPARISON("chart2"),
    PROCESSING_TIME_COMPARISON("chart3"),
    ANALYSIS_COMPARISON("chart4");

    private final String chartType;

    private ChartTypes(String chartType) {
        this.chartType = chartType;
    }

    public String getChartType() {
        return chartType;
    }
}
