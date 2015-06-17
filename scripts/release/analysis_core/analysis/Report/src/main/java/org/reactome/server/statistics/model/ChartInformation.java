package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ChartInformation {
    @JsonProperty("title")
    String title;
    @JsonProperty("description")
    String description;

    public ChartInformation() {
    }

    public ChartInformation(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
