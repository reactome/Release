package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Label {
    @JsonProperty("text")
    private String labelText;
    @JsonProperty("position")
    private String labelPosition;

    public Label() {
    }

    public Label(String labelText, String labelPosition) {
        this.labelText = labelText;
        this.labelPosition = labelPosition;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public String getLabelPosition() {
        return labelPosition;
    }

    public void setLabelPosition(String labelPosition) {
        this.labelPosition = labelPosition;
    }
}
