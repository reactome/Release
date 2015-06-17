package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class DataInformation {

    @JsonProperty("name")
    private String name;
    @JsonProperty("result")
    private String result;

    public DataInformation() {
    }

    public DataInformation(String name, String result) {
        this.name = name;
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
