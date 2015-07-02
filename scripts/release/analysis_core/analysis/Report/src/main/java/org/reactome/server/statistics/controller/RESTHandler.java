package org.reactome.server.statistics.controller;

import org.apache.log4j.Logger;
import org.reactome.server.statistics.database.LogDAO;
import org.reactome.server.statistics.model.*;
import org.reactome.server.statistics.properties.Granularity;
import org.reactome.server.statistics.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * This class is between the LogDAO and the LogChartsController and manages the upcoming http-requests.
 */
public class RESTHandler {
    private static Logger logger = Logger.getLogger(RESTHandler.class.getName());

    public static Model getRequestComparisonData(String startDate, String endDate, String granularity, Integer chartHeight, Integer chartWidth, boolean subchart, Integer amountOfTicks) {
        LogDAO logDAO = new LogDAO();
        List<Object> objectList = new ArrayList<>();
        Map<Date, String> executedAnalysisByDates = logDAO.getExecutedAnalysisByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> cachedAnalysisByDates = logDAO.getCachedAnalysisByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> downloadsByDates = logDAO.getDownloadsByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            for (Date date : DateUtils.getDatesHourly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createRequestComparisonData(executedAnalysisByDates, cachedAnalysisByDates, downloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.DAYS.getGranularity())) {
            for (Date date : DateUtils.getDatesDaily(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createRequestComparisonData(executedAnalysisByDates, cachedAnalysisByDates, downloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            for (Date date : DateUtils.getDatesMonthly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createRequestComparisonData(executedAnalysisByDates, cachedAnalysisByDates, downloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.YEARS.getGranularity())) {
            for (Date date : DateUtils.getDatesYearly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createRequestComparisonData(executedAnalysisByDates, cachedAnalysisByDates, downloadsByDates, date));
            }
        }
        String executedNumberAnalysis = logDAO.getExecutedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getExecutedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        logDAO.closeConnection();
        String cachedNumberAnalysis = logDAO.getTotalCached(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getTotalCached(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        logDAO.closeConnection();
        String downloadNumber = logDAO.getTotalDownloads(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getTotalDownloads(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        logDAO.closeConnection();
        return JSONObjFactory.getRequestComparisonModel(granularity, chartHeight, chartWidth, subchart, amountOfTicks, objectList, executedNumberAnalysis, cachedNumberAnalysis, downloadNumber);
    }

    public static Model getAnalysisComparisonData(String startDate, String endDate, String granularity, Integer chartHeight, Integer chartWidth, boolean subchart, Integer amountOfTicks) {
        LogDAO logDAO = new LogDAO();
        List<Object> objectList = new ArrayList<>();
        Map<Date, String> totalAnalysisByDates = logDAO.getTotalAnalysisByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> executedAnalysisByDates = logDAO.getExecutedAnalysisByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> cachedAnalysisByDates = logDAO.getCachedAnalysisByDates(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            for (Date date : DateUtils.getDatesHourly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createAnalysisComparisonData(totalAnalysisByDates, executedAnalysisByDates, cachedAnalysisByDates, date));
            }
        }
        if (granularity.equals(Granularity.DAYS.getGranularity())) {
            for (Date date : DateUtils.getDatesDaily(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createAnalysisComparisonData(totalAnalysisByDates, executedAnalysisByDates, cachedAnalysisByDates, date));
            }
        }
        if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            for (Date date : DateUtils.getDatesMonthly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createAnalysisComparisonData(totalAnalysisByDates, executedAnalysisByDates, cachedAnalysisByDates, date));
            }
        }
        if (granularity.equals(Granularity.YEARS.getGranularity())) {
            for (Date date : DateUtils.getDatesYearly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createAnalysisComparisonData(totalAnalysisByDates, executedAnalysisByDates, cachedAnalysisByDates, date));
            }
        }
        String totalNumberAnalysis = logDAO.getTotalNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getTotalNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        String executedNumberAnalysis = logDAO.getExecutedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getExecutedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        String cachedNumberAnalysis = logDAO.getCachedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getCachedNumberAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        return JSONObjFactory.getAnalysisComparisonModel(granularity, chartHeight, chartWidth, subchart, amountOfTicks, objectList, totalNumberAnalysis, executedNumberAnalysis, cachedNumberAnalysis);
    }

    public static Model getDataSetSizeAnalysisData(String startDate, String endDate, String granularity, Integer chartHeight, Integer chartWidth, boolean subchart, Integer amountOfTicks) {
        LogDAO logDAO = new LogDAO();
        List<Object> objectList = new ArrayList<>();
        Map<Date, String> avgDataSetSizeTotalByDates = logDAO.getAVGDataSetSizeTOTAL(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> avgDataSetSizeExecutedByDates = logDAO.getAVGDataSetSizeExecuted(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> avgDataSetSizeCachedByDates = logDAO.getAVGDataSetSizeCached(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            for (Date date : DateUtils.getDatesHourly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createDataSetSizeAnalysisData(avgDataSetSizeTotalByDates, avgDataSetSizeExecutedByDates, avgDataSetSizeCachedByDates, date));
            }
        }
        if (granularity.equals(Granularity.DAYS.getGranularity())) {
            for (Date date : DateUtils.getDatesDaily(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createDataSetSizeAnalysisData(avgDataSetSizeTotalByDates, avgDataSetSizeExecutedByDates, avgDataSetSizeCachedByDates, date));
            }
        }
        if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            for (Date date : DateUtils.getDatesMonthly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createDataSetSizeAnalysisData(avgDataSetSizeTotalByDates, avgDataSetSizeExecutedByDates, avgDataSetSizeCachedByDates, date));
            }
        }
        if (granularity.equals(Granularity.YEARS.getGranularity())) {
            for (Date date : DateUtils.getDatesYearly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createDataSetSizeAnalysisData(avgDataSetSizeTotalByDates, avgDataSetSizeExecutedByDates, avgDataSetSizeCachedByDates, date));
            }
        }
        return JSONObjFactory.getDataSetSizeAnalysisModel(granularity, chartHeight, chartWidth, subchart, amountOfTicks, objectList);
    }

    public static Model getProcessingTimeComparisonData(String startDate, String endDate, String granularity, Integer chartHeight, Integer chartWidth, boolean subchart, Integer amountOfTicks) {
        LogDAO logDAO = new LogDAO();
        List<Object> objectList = new ArrayList<>();
        Map<Date, String> averageTimeNewAnalysisByDates = logDAO.getAVGTimeNewAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> averageAverageTimeCachedByDates = logDAO.getAVGTimeCached(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        Map<Date, String> averageAverageTimeDownsloadsByDates = logDAO.getAVGTimeDownsloads(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate), granularity);
        logDAO.closeConnection();
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            for (Date date : DateUtils.getDatesHourly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createProcessingTimeComparisonData(averageTimeNewAnalysisByDates, averageAverageTimeCachedByDates, averageAverageTimeDownsloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.DAYS.getGranularity())) {
            for (Date date : DateUtils.getDatesDaily(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createProcessingTimeComparisonData(averageTimeNewAnalysisByDates, averageAverageTimeCachedByDates, averageAverageTimeDownsloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            for (Date date : DateUtils.getDatesMonthly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createProcessingTimeComparisonData(averageTimeNewAnalysisByDates, averageAverageTimeCachedByDates, averageAverageTimeDownsloadsByDates, date));
            }
        }
        if (granularity.equals(Granularity.YEARS.getGranularity())) {
            for (Date date : DateUtils.getDatesYearly(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate))) {
                objectList.add(createProcessingTimeComparisonData(averageTimeNewAnalysisByDates, averageAverageTimeCachedByDates, averageAverageTimeDownsloadsByDates, date));
            }
        }
        String avgTimeExecutedAnalysisTotal = logDAO.getAVGTimeExecutedAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getAVGTimeExecutedAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        String avgTimeCachedAnalysisTotal = logDAO.getAVGTimeCachedAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getAVGTimeCachedAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        String avgTimeDownloadsTotal = logDAO.getAVGTimeDownloadsAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) != null ? logDAO.getAVGTimeDownloadsAnalysis(DateUtils.formatStartDate(startDate), DateUtils.formatEndDate(endDate)) : "0";
        return JSONObjFactory.getProcessingTimeComparisonModel(granularity, chartHeight, chartWidth, subchart, amountOfTicks, objectList, avgTimeExecutedAnalysisTotal, avgTimeCachedAnalysisTotal, avgTimeDownloadsTotal);
    }


    private static RequestComparisonData createRequestComparisonData(Map<Date, String> executedAnalysisByDates,
                                                                     Map<Date, String> cachedAnalysisByDates,
                                                                     Map<Date, String> downloadsByDates,
                                                                     Date date) {
        RequestComparisonData requestComparisonData = new RequestComparisonData();
        requestComparisonData.setDate(DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        requestComparisonData.setAnalysisExecution(executedAnalysisByDates.get(date) != null ? executedAnalysisByDates.get(date) : "0");
        requestComparisonData.setAnalysisCached(cachedAnalysisByDates.get(date) != null ? cachedAnalysisByDates.get(date) : "0");
        requestComparisonData.setDownloads(downloadsByDates.get(date) != null ? downloadsByDates.get(date) : "0");
        return requestComparisonData;
    }

    private static AnalysisComparisonData createAnalysisComparisonData(Map<Date, String> totalAnalysisByDates,
                                                                       Map<Date, String> executedAnalysisByDates,
                                                                       Map<Date, String> cachedAnalysisByDates,
                                                                       Date date) {
        AnalysisComparisonData analysisComparisonData = new AnalysisComparisonData();
        analysisComparisonData.setDate(DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        analysisComparisonData.setTotal(totalAnalysisByDates.get(date) != null ? totalAnalysisByDates.get(date) : "0");
        analysisComparisonData.setExecuted(executedAnalysisByDates.get(date) != null ? executedAnalysisByDates.get(date) : "0");
        analysisComparisonData.setCached(cachedAnalysisByDates.get(date) != null ? cachedAnalysisByDates.get(date) : "0");
        return analysisComparisonData;

    }

    private static Object createDataSetSizeAnalysisData(Map<Date, String> avgDataSetSizeTotalByDates, Map<Date, String> avgDataSetSizeExecutedByDates, Map<Date, String> avgDataSetSizeCachedByDates, Date date) {
        DataSetSizeComparison dataSetSizeComparison = new DataSetSizeComparison();
        dataSetSizeComparison.setDate(DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        dataSetSizeComparison.setTotalDataSetSize(avgDataSetSizeTotalByDates.get(date) != null ? avgDataSetSizeTotalByDates.get(date) : "0");
        dataSetSizeComparison.setExecutedDataSetSize(avgDataSetSizeExecutedByDates.get(date) != null ? avgDataSetSizeExecutedByDates.get(date) : "0");
        dataSetSizeComparison.setCachedDataSetSize(avgDataSetSizeCachedByDates.get(date) != null ? avgDataSetSizeCachedByDates.get(date) : "0");
        return dataSetSizeComparison;
    }

    private static ProcessingTimeComparisonData createProcessingTimeComparisonData(Map<Date, String> averageTimeNewAnalysisByDates,
                                                                                   Map<Date, String> averageAverageTimeCachedByDates,
                                                                                   Map<Date, String> averageAverageTimeDownsloadsByDates,
                                                                                   Date date) {
        ProcessingTimeComparisonData processingTimeComparisonData = new ProcessingTimeComparisonData();
        processingTimeComparisonData.setDate(DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        processingTimeComparisonData.setAnalysisExecution(averageTimeNewAnalysisByDates.get(date) != null ? averageTimeNewAnalysisByDates.get(date) : "0");
        processingTimeComparisonData.setAnalysisCached(averageAverageTimeCachedByDates.get(date) != null ? averageAverageTimeCachedByDates.get(date) : "0");
        processingTimeComparisonData.setDownloads(averageAverageTimeDownsloadsByDates.get(date) != null ? averageAverageTimeDownsloadsByDates.get(date) : "0");
        return processingTimeComparisonData;
    }
}