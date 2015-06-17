package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class Axis {
    @JsonProperty("x")
    private org.reactome.server.statistics.model.XAxis XAxis;
    @JsonProperty("y")
    private YAxis yAxis;


    @JsonProperty("x")
    public org.reactome.server.statistics.model.XAxis getXAxis() {
        return XAxis;
    }

    @JsonProperty("x")
    public void setXAxis(org.reactome.server.statistics.model.XAxis XAxis) {
        this.XAxis = XAxis;
    }

    @JsonProperty("y")
    public YAxis getyAxis() {
        return yAxis;
    }

    @JsonProperty("y")
    public void setyAxis(YAxis yAxis) {
        this.yAxis = yAxis;
    }

    @Override
    public String toString() {
        return "Axis{" +
                "x=" + XAxis +
                '}';
    }
}