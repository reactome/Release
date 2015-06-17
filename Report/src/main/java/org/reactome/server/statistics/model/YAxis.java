package org.reactome.server.statistics.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Part of JSON-Object for chart
 */
public class YAxis {
    @JsonProperty("min")
    private final Integer min = 0;
    @JsonProperty("tick")
    private Tick tick;
    @JsonProperty("label")
    private Label label;
    @JsonProperty("padding")
    private Padding padding;

    public Tick getTick() {
        return tick;
    }

    public void setTick(Tick tick) {
        this.tick = tick;
    }

    public Integer getMin() {
        return min;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public Padding getPadding() {
        return padding;
    }

    public void setPadding(Padding padding) {
        this.padding = padding;
    }
}