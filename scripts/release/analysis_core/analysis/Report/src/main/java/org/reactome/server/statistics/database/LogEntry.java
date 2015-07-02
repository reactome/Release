package org.reactome.server.statistics.database;

import org.apache.log4j.Logger;
import org.reactome.server.statistics.util.DateUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * LogEntry represents the columns from the given logfile.
 */
public class LogEntry {
    private static Logger logger = Logger.getLogger(LogEntry.class.getName());

    private String date;
    private String typeOfRequest;
    private String typeOfExecution;
    private String analysisType;
    private String sampleName;
    private String projection;
    private int dataSetSize;
    private int found;
    private int notFound;
    private int processingTime;

    public LogEntry() {
    }

    public LogEntry(String date, String typeOfRequest, String typeOfExecution, String analysisType, String sampleName, String projection, int dataSetSize, int found, int notFound, int processingTime) {
        this.date = date;
        this.typeOfRequest = typeOfRequest;
        this.typeOfExecution = typeOfExecution;
        this.analysisType = analysisType;
        this.sampleName = sampleName;
        this.projection = projection;
        this.dataSetSize = dataSetSize;
        this.found = found;
        this.notFound = notFound;
        this.processingTime = processingTime;
    }

    /**
     * Merges date and time and returns a date object
     */
    public static Date createLogDate(String date, String time) {
        try {
            return DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").parse(date + " " + time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Merges date and time and returns a date object
     */
    public static String prepareLogDate(Date date) {
        return DateUtils.getdateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }


    /**
     * Deletes unnecessary string elements
     */
    public static Integer prepareDataSetSize(String dataSetSize) {
        dataSetSize = dataSetSize.replace("size:", "");
        return Integer.valueOf(dataSetSize);
    }

    /**
     * Deletes unnecessary string elements
     */
    public static Integer prepareFound(String found) {
        found = found.replace("found:", "");
        return Integer.valueOf(found);
    }

    /**
     * Deletes unnecessary string elements
     */
    public static Integer prepareNotFound(String notFound) {
        notFound = notFound.replace("notFound:", "");
        return Integer.valueOf(notFound);
    }

    /**
     * Deletes unnecessary string elements
     */
    public static Integer prepareTime(String time) {
        time = time.replace("time:", "").replace("ms", "");
        return Integer.valueOf(time);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTypeOfRequest() {
        return typeOfRequest;
    }

    public void setTypeOfRequest(String typeOfRequest) {
        this.typeOfRequest = typeOfRequest;
    }

    public String getTypeOfExecution() {
        return typeOfExecution;
    }

    public void setTypeOfExecution(String typeOfExecution) {
        this.typeOfExecution = typeOfExecution;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public int getDataSetSize() {
        return dataSetSize;
    }

    public void setDataSetSize(int dataSetSize) {
        this.dataSetSize = dataSetSize;
    }

    public int getFound() {
        return found;
    }

    public void setFound(int found) {
        this.found = found;
    }

    public int getNotFound() {
        return notFound;
    }

    public void setNotFound(int notFound) {
        this.notFound = notFound;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(int processingTime) {
        this.processingTime = processingTime;
    }

    @Override
    public String toString() {
        return "org.reactome.server.statistics.database.LogEntry{" +
                "date=" + date +
                ", typeOfRequest='" + typeOfRequest + '\'' +
                ", typeOfExecution='" + typeOfExecution + '\'' +
                ", analysisType='" + analysisType + '\'' +
                ", sampleName='" + sampleName + '\'' +
                ", projection='" + projection + '\'' +
                ", dataSetSize=" + dataSetSize +
                ", found=" + found +
                ", notFound=" + notFound +
                '}';
    }
}