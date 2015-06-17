package org.reactome.server.statistics.producer;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.log4j.Logger;
import org.reactome.server.statistics.database.LogDAO;
import org.reactome.server.statistics.database.LogEntry;
import org.reactome.server.statistics.util.DateUtils;
import org.springframework.jdbc.BadSqlGrammarException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class LogEntryProducer extends Thread {    
    private static Logger logger = Logger.getLogger(LogEntryProducer.class.getName());
    
    private static final String LOG_NAME = "ReactomeAnalysis_Report";
    private static final Integer UPDATE_TIME = 600000;
    private final String logPath;
    private LogDAO logDAO;
    private boolean firstTime = true;

    public LogEntryProducer(String logPath) {
        this.logPath = logPath;
        initialise();
        start();
    }

    public static String getStaticLogName() {
        return LOG_NAME;
    }

    /**
     * Thread who is managing reading and writing the log. Every day after midnight it will
     * start the procedure again, but looking for the last modified files.
     */
    public void run() {
        while (true) {
            LogDAO logDAO = new LogDAO();
            String maxDBDate = logDAO.getMaxDate();
            File[] files = getFiles();
            sortFileArray(files);
            for (File file : files != null ? files : new File[0]) {
                logger.info("Reading file " + file.getName());

                if (maxDBDate != null) {
                    if (new Date(file.lastModified()).before(DateUtils.dbDateStringToDate(maxDBDate))) {
                        continue;
                    }
                }
                if (fileContains(file.getName())) {
                    if (file.isFile()) {
                        if (firstTime) {
                            readFileTopDown(file);
                        } else {
                            readFileBottomUp(file, maxDBDate);
                        }
                    }
                }
                logger.info("finished " + file.getName());
            }
            try {
                firstTime = false;
                logger.info("Finished process...");
                Thread.sleep(UPDATE_TIME);
                logger.info("Restart process!");
            } catch (InterruptedException e) {
                logDAO.closeConnection();
                e.printStackTrace();
            }
        }
    }

    private void readFileBottomUp(File file, String maxDBDate) {
        ReversedLinesFileReader bufferedReader = null;

        try {
            bufferedReader = new ReversedLinesFileReader(file);
            String row;
            while ((row = bufferedReader.readLine()) != null) {
                String[] logRowArray = row.split("\\s");
                Date logRowDate = LogEntry.createLogDate(logRowArray[0], logRowArray[1]);
                if (maxDBDate != null) {
                    if (logRowDate.before(DateUtils.dbDateStringToDate(maxDBDate))) {
                        break;
                    }
                }
                if (!logRowArray[3].equals("_NOTFOUND_")) { //dirty fix for strange log file issue
                    this.save(new LogEntry(
                            LogEntry.prepareLogDate(logRowDate),
                            logRowArray[2],
                            logRowArray[3],
                            logRowArray[4],
                            logRowArray[5],
                            logRowArray[6],
                            LogEntry.prepareDataSetSize(logRowArray[7]),
                            LogEntry.prepareFound(logRowArray[8]),
                            LogEntry.prepareNotFound(logRowArray[9]),
                            LogEntry.prepareTime(logRowArray[10])
                    ));
                    logDAO.closeConnection();
                }
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                logDAO.closeConnection();
                e.printStackTrace();
            }
        }
    }

    private void readFileTopDown(File file) {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String row;
            while ((row = bufferedReader.readLine()) != null) {
                String[] logRowArray = row.split("\\s");
                Date logRowDate = LogEntry.createLogDate(logRowArray[0], logRowArray[1]);
                if (!logRowArray[3].equals("_NOTFOUND_")) { //dirty fix for strange log file issue
                    this.save(new LogEntry(
                            LogEntry.prepareLogDate(logRowDate),
                            logRowArray[2],
                            logRowArray[3],
                            logRowArray[4],
                            logRowArray[5],
                            logRowArray[6],
                            LogEntry.prepareDataSetSize(logRowArray[7]),
                            LogEntry.prepareFound(logRowArray[8]),
                            LogEntry.prepareNotFound(logRowArray[9]),
                            LogEntry.prepareTime(logRowArray[10])
                    ));
                    logDAO.closeConnection();
                }
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                logDAO.closeConnection();
                e.printStackTrace();
            }
        }
    }

    private boolean fileContains(String fileName) {
        return fileName.contains(LogEntryProducer.getStaticLogName());
    }

    private File[] getFiles() {
        return new File(logPath).listFiles();
    }

    private void save(LogEntry logEntry) {
        if (!logDAO.logEntryExists(logEntry)) {
            logDAO.insertLogEntry(logEntry);
        }
    }


    private void sortFileArray(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified());
            }
        });
    }

    private void initialise() {
        this.logDAO = new LogDAO();
        try {
            /**
             * BadSqlGrammarException if table already exists
             */
            logDAO.createTable();
        } catch (BadSqlGrammarException e) {
            logger.info("Database already exists");
        }
    }
}