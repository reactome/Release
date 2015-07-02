package org.reactome.server.statistics.database;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.reactome.server.statistics.properties.Granularity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Class for access the DB. All queries are located here.
 */
public class LogDAO {
    private static Logger logger = Logger.getLogger(LogDAO.class.getName());


    private final JdbcTemplate jdbcTemplate;

    public LogDAO() {
        BasicDataSource dataSource = DataSourceFactory.getDatabaseConnection();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Insert logEntry into DB
     */
    public void insertLogEntry(LogEntry logEntry) {
        String query = "INSERT INTO logs values (?,?,?,?,?,?,?,?,?,?);";
        try {
            jdbcTemplate.update(query,
                    logEntry.getDate(),
                    logEntry.getTypeOfRequest(),
                    logEntry.getTypeOfExecution(),
                    logEntry.getAnalysisType(),
                    logEntry.getSampleName(),
                    logEntry.getProjection(),
                    logEntry.getDataSetSize(),
                    logEntry.getFound(),
                    logEntry.getNotFound(),
                    logEntry.getProcessingTime());
        } catch (Exception e){
            logger.error("Error on inserting log entry");
        }
    }

    /**
     * Checks for if given logEntry is already in the db.
     * TODO duplicates!
     */
    public boolean logEntryExists(LogEntry logEntry) {
        String query = "SELECT * FROM logs WHERE " +
                "datetime = ? AND t" +
                "ypeOfRequest = ? AND " +
                "typeOfExecution = ? AND " +
                "analysisType = ? AND " +
                "sampleName = ? AND " +
                "projection = ? AND " +
                "dataSetSize = ? AND " +
                "foundResult = ? AND " +
                "notFoundResult = ? AND " +
                "processingTime = ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query,
                logEntry.getDate(),
                logEntry.getTypeOfRequest(),
                logEntry.getTypeOfExecution(),
                logEntry.getAnalysisType(),
                logEntry.getSampleName(),
                logEntry.getProjection(),
                logEntry.getDataSetSize(),
                logEntry.getFound(),
                logEntry.getNotFound(),
                logEntry.getProcessingTime());
        if (rowSet.next()) {
            return true;
        }
        return false;
    }

    /**
     * create a new table
     */
    public void createTable() {
        String query = "CREATE TABLE logs (datetime VARCHAR(255)," +
                "typeOfRequest VARCHAR(255)," +
                "typeOfExecution VARCHAR(255)," +
                "analysisType VARCHAR(255)," +
                "sampleName VARCHAR(255)," +
                "projection VARCHAR(255)," +
                "dataSetSize INTEGER," +
                "foundResult INTEGER," +
                "notFoundResult INTEGER," +
                "processingTime INTEGER)";
        jdbcTemplate.execute(query);
    }


    /**
     * Returns a map containing the total amount of analysis which were executed and cached, per time unit
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getTotalAnalysisByDates(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT COUNT (*) RESULT, " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    /**
     * Returns a map containing the amount of analysis which were executed, per time unit
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getExecutedAnalysisByDates(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT COUNT (*) RESULT, " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_EXEC_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    /**
     * Returns a map containing the amount of analysis which were cached, because they have been already executed, per time unit
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getCachedAnalysisByDates(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT COUNT (*), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    //CHARTS
    /**
     * Returns a map containing the amount of downloads, per time unit
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getDownloadsByDates(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT COUNT (dataSetSize), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_DOWNLOAD_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    /**
     * Returns the average data set size by a given granularity.
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getAVGDataSetSizeTOTAL(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (dataSetSize), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    /**
     * Returns the average data set size by a given granularity.
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getAVGDataSetSizeExecuted(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (dataSetSize), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_EXEC_'  AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    /**
     * Returns the average data set size by a given granularity.
     *
     * @param startDate
     * @param endDate
     * @param granularity
     * @return
     */
    public Map<Date, String> getAVGDataSetSizeCached(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (dataSetSize), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_'  AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }


    public Map<Date, String> getAVGTimeNewAnalysis(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (processingTime), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_EXEC_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    public Map<Date, String> getAVGTimeCached(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (processingTime), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;
    }

    public Map<Date, String> getAVGTimeDownsloads(String startDate, String endDate, String granularity) {
        Map<Date, String> dateStringMap = new HashMap<>();
        String extractCommand = getExtractCommand(granularity);
        String groupByCommand = getGroupByCommand(granularity);
        String orderByCommand = getOrderByCommand(granularity);
        String query = "SELECT AVG (processingTime), " + extractCommand + " FROM logs " +
                " WHERE typeOfRequest = '_DOWNLOAD_' AND datetime BETWEEN ? AND ? " +
                groupByCommand + orderByCommand;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);

        while (rowSet.next()) {
            Date date = prepareDate(rowSet, granularity);
            dateStringMap.put(date, rowSet.getString(1));
        }
        return dateStringMap;

    }

    //Info's

    public String getTotalNumberAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT COUNT(typeOfRequest) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getExecutedNumberAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT COUNT(*) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_EXEC_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getCachedNumberAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT COUNT(*) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getAVGTimeExecutedAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT AVG(processingTime) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_EXEC_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getAVGTimeCachedAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT SELECT AVG(processingTime) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getAVGTimeDownloadsAnalysis(String startDate, String endDate) {
        String result = null;
        String query = "SELECT AVG(processingTime) FROM logs WHERE typeOfRequest = '_DOWNLOAD_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getTotalCached(String startDate, String endDate) {
        String result = null;
        String query = "SELECT COUNT(*) FROM logs WHERE typeOfRequest = '_ANALYSIS_' AND typeOfExecution = '_CACHE_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getTotalDownloads(String startDate, String endDate) {
        String result = null;
        String query = "SELECT COUNT(*) FROM logs WHERE typeOfRequest = '_DOWNLOAD_' AND datetime BETWEEN ? AND ?";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, startDate, endDate);
        while (rowSet.next()) {
            result = rowSet.getString(1);
        }
        return result;
    }

    public String getMaxDate() {
        String maxDate = null;
        String query = "SELECT MAX(datetime) FROM logs";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query);
        while (rowSet.next()) {
            maxDate = rowSet.getString(1);
        }
        return maxDate;
    }

    //TODO Think JDBCTemplate is handling this
    public void closeConnection() {
        try {
            jdbcTemplate.getDataSource().getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getGroupByCommand(String granularity) {
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            return " GROUP BY YEAR, MONTH, DAY, HOUR";
        } else if (granularity.equals(Granularity.DAYS.getGranularity())) {
            return " GROUP BY YEAR, MONTH, DAY";
        } else if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            return " GROUP BY YEAR, MONTH";
        } else if (granularity.equals(Granularity.YEARS.getGranularity())) {
            return " GROUP BY YEAR";
        }
        return null;
    }

    private String getOrderByCommand(String granularity) {
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            return " ORDER BY YEAR, MONTH, DAY, HOUR";
        } else if (granularity.equals(Granularity.DAYS.getGranularity())) {
            return " ORDER BY YEAR, MONTH, DAY";
        } else if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            return " ORDER BY YEAR, MONTH";
        } else if (granularity.equals(Granularity.YEARS.getGranularity())) {
            return " ORDER BY YEAR";
        }
        return null;
    }

    private String getExtractCommand(String granularity) {
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            return " EXTRACT (HOUR FROM datetime) AS HOUR, " +
                    "EXTRACT (DAY FROM datetime) AS DAY, " +
                    "EXTRACT (MONTH FROM datetime) AS MONTH, " +
                    "EXTRACT (YEAR FROM datetime) AS YEAR";
        } else if (granularity.equals(Granularity.DAYS.getGranularity())) {
            return " EXTRACT (DAY FROM datetime) AS DAY, " +
                    "EXTRACT (MONTH FROM datetime) AS MONTH, " +
                    "EXTRACT (YEAR FROM datetime) AS YEAR";
        } else if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            return " EXTRACT (MONTH FROM datetime) AS MONTH, " +
                    "EXTRACT (YEAR FROM datetime) AS YEAR";
        } else if (granularity.equals(Granularity.YEARS.getGranularity())) {
            return " EXTRACT (YEAR FROM datetime) AS YEAR";
        }
        return null;
    }

    private Date prepareDate(SqlRowSet rowSet, String granularity) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (granularity.equals(Granularity.HOURS.getGranularity())) {
            String date = rowSet.getString(5) + "-" + rowSet.getString(4) + "-" + rowSet.getString(3) + " " + rowSet.getString(2) + ":" + "00" + ":" + "00";
            try {
                return dateFormat.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (granularity.equals(Granularity.DAYS.getGranularity())) {
            String date = rowSet.getString(4) + "-" + rowSet.getString(3) + "-" + rowSet.getString(2) + " " + "00" + ":" + "00" + ":" + "00";
            try {
                return dateFormat.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (granularity.equals(Granularity.MONTHS.getGranularity())) {
            String date = rowSet.getString(3) + "-" + rowSet.getString(2) + "-" + "01" + " " + "00" + ":" + "00" + ":" + "00";
            try {
                return dateFormat.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (granularity.equals(Granularity.YEARS.getGranularity())) {
            String date = rowSet.getString(2) + "-" + "01" + "-" + "01" + " " + "00" + ":" + "00" + ":" + "00";
            try {
                return dateFormat.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}