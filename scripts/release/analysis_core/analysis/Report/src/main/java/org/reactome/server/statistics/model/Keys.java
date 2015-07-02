package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class Keys {
    @JsonProperty("x")
    String x;
    @JsonProperty("value")
    List<String> value = new ArrayList<String>();

    public Keys() {
    }

    public Keys(String x, List<String> value) {
        this.x = x;
        this.value = value;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Keys{" +
                "x='" + x + '\'' +
                ", value=" + value +
                '}';
    }
}