package org.reactome.server.statistics.model;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Padding {
    @JsonProperty("top")
    private final Integer top = 0;
    @JsonProperty("bottom")
    private final Integer bottom = 0;

    public Integer getTop() {
        return top;
    }

    public Integer getBottom() {
        return bottom;
    }
}
