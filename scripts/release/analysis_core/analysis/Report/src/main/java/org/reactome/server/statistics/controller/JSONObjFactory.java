package org.reactome.server.statistics.controller;

import org.apache.log4j.Logger;
import org.reactome.server.statistics.model.*;
import org.reactome.server.statistics.properties.Granularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class JSONObjFactory {
    private static Logger logger = Logger.getLogger(JSONObjFactory.class.getName());

    public static Model getRequestComparisonModel(String granularity,
                                                  Integer chartHeight,
                                                  Integer chartWidth,
                                                  boolean subchart,
                                                  Integer amountOfTicks,
                                                  List<Object> objectList,
                                                  String executedNumberAnalysis,
                                                  String cachedNumberAnalysis,
                                                  String downloadNumber) {
        Model model = new Model();
        C3Chart c3Chart = new C3Chart();
        DataInformation totalNumberExecutedAnalysis = new DataInformation();
        DataInformation totalNumberCachedAnalysis = new DataInformation();
        DataInformation totalNumberDownloads = new DataInformation();
        ChartInformation chartInformation = new ChartInformation();
        Data data = new Data();
        Axis axis = new Axis();
        SubChart subChart = new SubChart();
        Keys keys = new Keys();
        Names names = new Names();
        XAxis xAxis = new XAxis();
        YAxis yAxis = new YAxis();
        Padding padding = new Padding();
        Size size = new Size();
        Label xLabel = new Label();
        Label yLabel = new Label();
        Culling xCulling = new Culling();
        List<DataInformation> dataInformationList = new ArrayList<DataInformation>();
        data.setJsonObjectList(objectList);
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("analysisExecution", "Executed Analysis");
        nameMap.put("analysisCached", "Cached Analysis");
        nameMap.put("downloads", "Downloads");
        names.setNamesMap(nameMap);
        data.setNames(nameMap);
        List<String> keyList = new ArrayList<String>();
        keyList.add("analysisExecution");
        keyList.add("analysisCached");
        keyList.add("downloads");
        keys.setValue(keyList);
        keys.setX("date");
        data.setKeys(keys);
        Tick xTick = getTick(granularity);
        Tick yTick = new Tick();
        xCulling.setMaxTicks(amountOfTicks);
        xTick.setCulling(xCulling);
        xAxis.setTick(xTick);
        xAxis.getTick().getCulling();
        xAxis.setType("timeseries");
        yAxis.setTick(yTick);
        axis.setXAxis(xAxis);
        axis.setyAxis(yAxis);
        xLabel.setLabelText("Time");
        yLabel.setLabelText("Number of analysis");
        xLabel.setLabelPosition("outer-center");
        yLabel.setLabelPosition("outer-middle");
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);
        yAxis.setPadding(padding);
        subChart.setShow(subchart);
        size.setWidth(chartWidth);
        size.setHeight(chartHeight);
        c3Chart.setSize(size);
        c3Chart.setSubChart(subChart);
        c3Chart.setAxis(axis);
        c3Chart.setData(data);
        c3Chart.setBindto(".requestComparison.chart");
        totalNumberExecutedAnalysis.setName("Total number of executed analysis");
        totalNumberExecutedAnalysis.setResult(executedNumberAnalysis);
        totalNumberCachedAnalysis.setName("Total number of cached analysis");
        totalNumberCachedAnalysis.setResult(cachedNumberAnalysis);
        totalNumberDownloads.setName("Total number of downloads");
        totalNumberDownloads.setResult(downloadNumber);
        dataInformationList.add(totalNumberExecutedAnalysis);
        dataInformationList.add(totalNumberCachedAnalysis);
        dataInformationList.add(totalNumberDownloads);
        chartInformation.setTitle("Total (Analysis & Download) Requests Comparison");
        chartInformation.setDescription("The following chart illustrates the number of analysis and download requests received over the specified time period.");
        model.setChartInformation(chartInformation);
        model.setDataInformationList(dataInformationList);
        model.setC3Chart(c3Chart);
        return model;
    }

    public static Model getDataSetSizeAnalysisModel(String granularity,
                                                    Integer chartHeight,
                                                    Integer chartWidth,
                                                    boolean subchart,
                                                    Integer amountOfTicks,
                                                    List<Object> objectList) {
        String totalNumberDataSetSize;
        String avgNumberDataSetSize;
        Model model = new Model();
        C3Chart c3Chart = new C3Chart();
        DataInformation dataInformationTotal = new DataInformation();
        DataInformation dataInformationAVG = new DataInformation();
        ChartInformation chartInformation = new ChartInformation();
        Data data = new Data();
        Axis axis = new Axis();
        SubChart subChart = new SubChart();
        Keys keys = new Keys();
        Names names = new Names();
        XAxis xAxis = new XAxis();
        YAxis yAxis = new YAxis();
        Padding padding = new Padding();
        Size size = new Size();
        Label xLabel = new Label();
        Label yLabel = new Label();
        Culling xCulling = new Culling();
        List<DataInformation> dataInformationList = new ArrayList<DataInformation>();
        data.setJsonObjectList(objectList);
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("totalDataSetSize", "Total average dataset Size");
        nameMap.put("executedDataSetSize", "Executed average dataset Size");
        nameMap.put("cachedDataSetSize", "Cached average dataset size");
        names.setNamesMap(nameMap);
        data.setNames(nameMap);
        List<String> keyList = new ArrayList<String>();
        keyList.add("totalDataSetSize");
        keyList.add("executedDataSetSize");
        keyList.add("cachedDataSetSize");
        keys.setValue(keyList);
        keys.setX("date");
        data.setKeys(keys);
        Tick xTick = getTick(granularity);
        Tick yTick = new Tick();
        xCulling.setMaxTicks(amountOfTicks);
        xTick.setCulling(xCulling);
        xAxis.setTick(xTick);
        xAxis.setType("timeseries");
        yAxis.setTick(yTick);
        axis.setXAxis(xAxis);
        axis.setyAxis(yAxis);
        xLabel.setLabelText("Time");
        yLabel.setLabelText("Average dataset size");
        xLabel.setLabelPosition("outer-center");
        yLabel.setLabelPosition("outer-middle");
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);
        yAxis.setPadding(padding);
        size.setHeight(chartHeight);
        size.setWidth(chartWidth);
        subChart.setShow(subchart);
        c3Chart.setSize(size);
        c3Chart.setSubChart(subChart);
        c3Chart.setAxis(axis);
        c3Chart.setData(data);
        c3Chart.setBindto(".dateSetSizeAnalysis.chart");
        chartInformation.setTitle("Dataset Size Comparison");
        chartInformation.setDescription("The following chart presents the average size of the datasets processed by the analysis service, over the specified time period. ");
        model.setChartInformation(chartInformation);
        model.setC3Chart(c3Chart);
        return model;
    }

    public static Model getProcessingTimeComparisonModel(String granularity,
                                                         Integer chartHeight,
                                                         Integer chartWidth,
                                                         boolean subchart,
                                                         Integer amountOfTicks,
                                                         List<Object> objectList,
                                                         String avgTimeExecutedAnalysisTotal,
                                                         String avgTimeCachedAnalysisTotal,
                                                         String avgTimeDownloadsTotal) {
        Model model = new Model();
        C3Chart c3Chart = new C3Chart();
        DataInformation dataInformationExecutedTime = new DataInformation();
        DataInformation dataInformationCachedTime = new DataInformation();
        DataInformation dataInformationDownloadTime = new DataInformation();
        ChartInformation chartInformation = new ChartInformation();
        Data data = new Data();
        Axis axis = new Axis();
        SubChart subChart = new SubChart();
        Keys keys = new Keys();
        Names names = new Names();
        XAxis xAxis = new XAxis();
        YAxis yAxis = new YAxis();
        Padding padding = new Padding();
        Size size = new Size();
        Label xLabel = new Label();
        Label yLabel = new Label();
        Culling xCulling = new Culling();
        List<DataInformation> dataInformationList = new ArrayList<DataInformation>();
        data.setJsonObjectList(objectList);
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("analysisExecution", "Executed Analysis");
        nameMap.put("analysisCached", "Cached Analysis");
        nameMap.put("downloads", "Downloads");
        names.setNamesMap(nameMap);
        data.setNames(nameMap);
        List<String> keyList = new ArrayList<String>();
        keyList.add("analysisExecution");
        keyList.add("analysisCached");
        keyList.add("downloads");
        keys.setValue(keyList);
        keys.setX("date");
        data.setKeys(keys);
        Tick xTick = getTick(granularity);
        Tick yTick = new Tick();
        xCulling.setMaxTicks(amountOfTicks);
        xTick.setCulling(xCulling);
        xAxis.setTick(xTick);
        xAxis.setType("timeseries");
        yAxis.setTick(yTick);
        axis.setXAxis(xAxis);
        axis.setyAxis(yAxis);
        xLabel.setLabelText("Time");
        yLabel.setLabelText("Amount of time (ms)");
        xLabel.setLabelPosition("outer-center");
        yLabel.setLabelPosition("outer-middle");
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);
        yAxis.setPadding(padding);
        size.setHeight(chartHeight);
        size.setWidth(chartWidth);
        c3Chart.setSize(size);
        subChart.setShow(subchart);
        c3Chart.setSubChart(subChart);
        c3Chart.setAxis(axis);
        c3Chart.setData(data);
        c3Chart.setBindto(".processingTimeComparison.chart");
        dataInformationExecutedTime.setName("Total average time of executed analysis in ms");
        dataInformationExecutedTime.setResult(avgTimeExecutedAnalysisTotal);
        dataInformationCachedTime.setName("Total average time of cached analysis in ms");
        dataInformationCachedTime.setResult(avgTimeCachedAnalysisTotal);
        dataInformationDownloadTime.setName("Total average time of downloads in ms");
        dataInformationDownloadTime.setResult(avgTimeDownloadsTotal);
        dataInformationList.add(dataInformationExecutedTime);
        dataInformationList.add(dataInformationCachedTime);
        dataInformationList.add(dataInformationDownloadTime);
        chartInformation.setTitle("Processing Time Comparison");
        chartInformation.setDescription("This chart features the average processing time required by the analysis service, over the specified time period.");
        model.setChartInformation(chartInformation);
        model.setC3Chart(c3Chart);
        model.setDataInformationList(dataInformationList);
        return model;
    }

    public static Model getAnalysisComparisonModel(String granularity,
                                                   Integer chartHeight,
                                                   Integer chartWidth,
                                                   boolean subchart,
                                                   Integer amountOfTicks,
                                                   List<Object> objectList,
                                                   String totalNumberAnalysis,
                                                   String executedNumberAnalysis,
                                                   String cachedNumberAnalysis) {
        Model model = new Model();
        C3Chart c3Chart = new C3Chart();
        DataInformation dataInformationTotal = new DataInformation();
        DataInformation dataInformationExecuted = new DataInformation();
        DataInformation dataInformationCached = new DataInformation();
        ChartInformation chartInformation = new ChartInformation();
        Data data = new Data();
        Axis axis = new Axis();
        SubChart subChart = new SubChart();
        Keys keys = new Keys();
        Names names = new Names();
        XAxis xAxis = new XAxis();
        YAxis yAxis = new YAxis();
        Size size = new Size();
        Label xLabel = new Label();
        Label yLabel = new Label();
        Padding padding = new Padding();
        Culling xCulling = new Culling();
        List<DataInformation> dataInformationList = new ArrayList<DataInformation>();
        data.setJsonObjectList(objectList);
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("total", "Total analysis");
        nameMap.put("executed", "Executed analysis");
        nameMap.put("cached", "Cached analysis");
        names.setNamesMap(nameMap);
        data.setNames(nameMap);
        List<String> keyList = new ArrayList<String>();
        keyList.add("total");
        keyList.add("executed");
        keyList.add("cached");
        keys.setValue(keyList);
        keys.setX("date");
        data.setKeys(keys);
        Tick xTick = getTick(granularity);
        Tick yTick = new Tick();
        xCulling.setMaxTicks(amountOfTicks);
        xTick.setCulling(xCulling);
        xAxis.setTick(xTick);
        xAxis.setType("timeseries");
        yAxis.setTick(yTick);
        xLabel.setLabelText("Time");
        yLabel.setLabelText("Number of Analysis");
        xLabel.setLabelPosition("outer-center");
        yLabel.setLabelPosition("outer-middle");
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);
        yAxis.setPadding(padding);
        axis.setXAxis(xAxis);
        axis.setyAxis(yAxis);
        size.setHeight(chartHeight);
        size.setWidth(chartWidth);
        subChart.setShow(subchart);
        c3Chart.setSize(size);
        c3Chart.setSubChart(subChart);
        c3Chart.setAxis(axis);
        c3Chart.setData(data);
        c3Chart.setBindto(".analysisComparison.chart");
        dataInformationTotal.setName("Total number of analysis");
        dataInformationTotal.setResult(totalNumberAnalysis);
        dataInformationExecuted.setName("Total number of executed analysis");
        dataInformationExecuted.setResult(executedNumberAnalysis);
        dataInformationCached.setName("Total number of cached analysis");
        dataInformationCached.setResult(cachedNumberAnalysis);
        dataInformationList.add(dataInformationTotal);
        dataInformationList.add(dataInformationExecuted);
        dataInformationList.add(dataInformationCached);
        chartInformation.setTitle("Analysis Requests Comparison");
        chartInformation.setDescription("The following chart presents the number of analyses requests received, both executed and cached, over the specified time period.");
        model.setChartInformation(chartInformation);
        model.setC3Chart(c3Chart);
        model.setDataInformationList(dataInformationList);
        return model;
    }


    private static Tick getTick(String granularity) {
        Tick tick = new Tick();
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            tick.setFormat("%Y-%m-%d %H:%M:%S");
        }
        if (granularity.equals(Granularity.DAYS.getGranularity())) {
            tick.setFormat("%Y-%m-%d");

        }
        if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            tick.setFormat("%Y-%m");

        }
        if (granularity.equals(Granularity.YEARS.getGranularity())) {
            tick.setFormat("%Y");
        }
        return tick;
    }
}
