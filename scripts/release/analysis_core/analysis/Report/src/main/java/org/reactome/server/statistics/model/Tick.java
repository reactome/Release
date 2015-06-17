package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class Tick {
    @JsonProperty("format")
    private String format;
    @JsonProperty("culling")
    private Culling culling;

    public Tick() {
    }

    public Tick(String format, Culling culling) {
        this.format = format;
        this.culling = culling;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Culling getCulling() {
        return culling;
    }

    public void setCulling(Culling culling) {
        this.culling = culling;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tick tick = (Tick) o;

        if (format != null ? !format.equals(tick.format) : tick.format != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return format != null ? format.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Tick{" +
                "format='" + format + '\'' +
                '}';
    }
}