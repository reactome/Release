package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * This class is the top level class for creating the JSON-Object for the C3-Chart.
 */
public class C3Chart {
    @JsonProperty("data")
    private Data data;
    @JsonProperty("axis")
    private Axis axis;
    @JsonProperty("bindto")
    private String bindto;
    @JsonProperty("subchart")
    private SubChart subChart;
    @JsonProperty("size")
    private Size size;


    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Axis getAxis() {
        return axis;
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    public String getBindto() {
        return bindto;
    }

    public void setBindto(String bindto) {
        this.bindto = bindto;
    }

    public SubChart getSubChart() {
        return subChart;
    }

    public void setSubChart(SubChart subChart) {
        this.subChart = subChart;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }
}