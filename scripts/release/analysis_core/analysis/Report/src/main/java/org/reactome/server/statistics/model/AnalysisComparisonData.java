package org.reactome.server.statistics.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of JSON-Object for chart
 */
public class AnalysisComparisonData {
    @JsonProperty("date")
    private String date;
    @JsonProperty("total")
    private String total;
    @JsonProperty("executed")
    private String executed;
    @JsonProperty("cached")
    private String cached;

    public AnalysisComparisonData() {
    }

    public AnalysisComparisonData(String date, String total, String executed, String cached) {
        this.date = date;
        this.total = total;
        this.executed = executed;
        this.cached = cached;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getExecuted() {
        return executed;
    }

    public void setExecuted(String executed) {
        this.executed = executed;
    }

    public String getCached() {
        return cached;
    }

    public void setCached(String cached) {
        this.cached = cached;
    }

    @Override
    public String toString() {
        return "AnalysisComparisonData{" +
                "date='" + date + '\'' +
                ", total='" + total + '\'' +
                ", executed='" + executed + '\'' +
                ", cached='" + cached + '\'' +
                '}';
    }
}