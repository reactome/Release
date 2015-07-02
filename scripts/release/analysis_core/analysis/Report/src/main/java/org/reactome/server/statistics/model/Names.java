package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class Names {
    @JsonProperty("namesMap")
    private Map<String, String> namesMap;

    public Map<String, String> getNamesMap() {
        return namesMap;
    }

    public void setNamesMap(Map<String, String> namesMap) {
        this.namesMap = namesMap;
    }

    @Override
    public String toString() {
        return "Names{" +
                "namesMap=" + namesMap +
                '}';
    }
}