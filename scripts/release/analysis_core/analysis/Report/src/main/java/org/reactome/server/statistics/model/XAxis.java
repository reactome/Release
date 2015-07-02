package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class XAxis {
    @JsonProperty("type")
    private String type;
    @JsonProperty("tick")
    private Tick tick;
    @JsonProperty("label")
    private Label label;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Tick getTick() {
        return tick;
    }

    public void setTick(Tick tick) {
        this.tick = tick;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XAxis XAxis = (org.reactome.server.statistics.model.XAxis) o;

        if (tick != null ? !tick.equals(XAxis.tick) : XAxis.tick != null) return false;
        if (type != null ? !type.equals(XAxis.type) : XAxis.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (tick != null ? tick.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "X{" +
                "type='" + type + '\'' +
                ", tick=" + tick +
                '}';
    }
}