package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Model {
    @JsonProperty("chart")
    private C3Chart c3Chart;
    @JsonProperty("chartInformation")
    private ChartInformation chartInformation;
    @JsonProperty("dataInformation")
    private List<DataInformation> dataInformationList;

    public Model() {
    }

    public Model(C3Chart c3Chart, ChartInformation chartInformation, List<DataInformation> dataInformationList) {
        this.c3Chart = c3Chart;
        this.chartInformation = chartInformation;
        this.dataInformationList = dataInformationList;
    }

    public C3Chart getC3Chart() {
        return c3Chart;
    }

    public void setC3Chart(C3Chart c3Chart) {
        this.c3Chart = c3Chart;
    }

    public ChartInformation getChartInformation() {
        return chartInformation;
    }

    public void setChartInformation(ChartInformation chartInformation) {
        this.chartInformation = chartInformation;
    }

    public List<DataInformation> getDataInformationList() {
        return dataInformationList;
    }

    public void setDataInformationList(List<DataInformation> dataInformationList) {
        this.dataInformationList = dataInformationList;
    }
}
