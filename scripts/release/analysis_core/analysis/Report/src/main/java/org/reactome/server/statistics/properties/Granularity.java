package org.reactome.server.statistics.properties;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * All possible time units
 */
public enum Granularity {
    HOURS("hours"),
    DAYS("days"),
    MONTHS("months"),
    YEARS("years");

    private final String granularity;

    private Granularity(final String namespace) {
        this.granularity = namespace;
    }

    public String getGranularity() {
        return granularity;
    }
}
