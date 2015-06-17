package org.reactome.server.statistics.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of JSON-Object for chart
 */
public class ProcessingTimeComparisonData {
    @JsonProperty("date")
    private String date;
    @JsonProperty("analysisExecution")
    private String analysisExecution;
    @JsonProperty("analysisCached")
    private String analysisCached;
    @JsonProperty("downloads")
    private String downloads;

    public ProcessingTimeComparisonData() {
    }

    public ProcessingTimeComparisonData(String date, String analysisExecution, String analysisCached, String downloads) {
        this.date = date;
        this.analysisExecution = analysisExecution;
        this.analysisCached = analysisCached;
        this.downloads = downloads;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAnalysisExecution() {
        return analysisExecution;
    }

    public void setAnalysisExecution(String analysisExecution) {
        this.analysisExecution = analysisExecution;
    }

    public String getAnalysisCached() {
        return analysisCached;
    }

    public void setAnalysisCached(String analysisCached) {
        this.analysisCached = analysisCached;
    }

    public String getDownloads() {
        return downloads;
    }

    public void setDownloads(String downloads) {
        this.downloads = downloads;
    }

    @Override
    public String toString() {
        return "ProcessingTimeComparisonData{" +
                "date='" + date + '\'' +
                ", analysisExecution='" + analysisExecution + '\'' +
                ", analysisCached='" + analysisCached + '\'' +
                ", downloads='" + downloads + '\'' +
                '}';
    }
}