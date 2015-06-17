package org.reactome.server.statistics.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of JSON-Object for chart
 */
public class DataSetSizeComparison {
    @JsonProperty("date")
    String date;
    @JsonProperty("totalDataSetSize")
    String totalDataSetSize;
    @JsonProperty("executedDataSetSize")
    String executedDataSetSize;
    @JsonProperty("cachedDataSetSize")
    String cachedDataSetSize;

    public DataSetSizeComparison() {
    }

    public DataSetSizeComparison(String date, String totalDataSetSize, String executedDataSetSize, String cachedDataSetSize) {
        this.date = date;
        this.totalDataSetSize = totalDataSetSize;
        this.executedDataSetSize = executedDataSetSize;
        this.cachedDataSetSize = cachedDataSetSize;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTotalDataSetSize() {
        return totalDataSetSize;
    }

    public void setTotalDataSetSize(String totalDataSetSize) {
        this.totalDataSetSize = totalDataSetSize;
    }

    public String getExecutedDataSetSize() {
        return executedDataSetSize;
    }

    public void setExecutedDataSetSize(String executedDataSetSize) {
        this.executedDataSetSize = executedDataSetSize;
    }

    public String getCachedDataSetSize() {
        return cachedDataSetSize;
    }

    public void setCachedDataSetSize(String cachedDataSetSize) {
        this.cachedDataSetSize = cachedDataSetSize;
    }

    @Override
    public String toString() {
        return "dataSetSizeComparison{" +
                "date='" + date + '\'' +
                ", totalDataSetSize='" + totalDataSetSize + '\'' +
                ", executedDataSetSize='" + executedDataSetSize + '\'' +
                ", cachedDataSetSize='" + cachedDataSetSize + '\'' +
                '}';
    }
}