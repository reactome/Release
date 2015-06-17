package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Culling {
    @JsonProperty("max")
    private Integer maxTicks;

    public Culling() {
    }

    public Culling(Integer maxTicks) {
        this.maxTicks = maxTicks;
    }

    public Integer getMaxTicks() {
        return maxTicks;
    }

    public void setMaxTicks(Integer maxTicks) {
        this.maxTicks = maxTicks;
    }

    @Override
    public String toString() {
        return "Culling{" +
                "maxTicks=" + maxTicks +
                '}';
    }
}
