package org.reactome.server.statistics.util;

import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class DateUtils {
    private static Logger logger = Logger.getLogger(DateUtils.class.getName());

    /**
     * Get on start and end date all timestamps, by hour
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static List<Date> getDatesHourly(String startDate, String endDate) {
        List<Date> dateList = new ArrayList<Date>();
        Date start = null;
        Date end = null;
        try {
            start = getdateFormat("yyyy-MM-dd").parse(startDate);
            end = getdateFormat("yyyy-MM-dd").parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendarStartDate = Calendar.getInstance();
        Calendar calendarEndDate = Calendar.getInstance();
        calendarStartDate.setTime(start);
        calendarEndDate.setTime(end);
        calendarStartDate.set(Calendar.HOUR_OF_DAY, 0);
        calendarStartDate.set(Calendar.MINUTE, 0);
        calendarStartDate.set(Calendar.SECOND, 0);
        calendarEndDate.set(Calendar.HOUR_OF_DAY, 23);
        calendarEndDate.set(Calendar.MINUTE, 0);
        calendarEndDate.set(Calendar.SECOND, 0);
        dateList.add(calendarStartDate.getTime());
        while (true) {
            if (calendarStartDate.equals(calendarEndDate)) {
                break;
            }
            calendarStartDate.add(Calendar.HOUR_OF_DAY, 1);
            dateList.add(calendarStartDate.getTime());
        }
        return dateList;
    }

    /**
     * Get on start and end date all timestamps, by day
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static List<Date> getDatesDaily(String startDate, String endDate) {
        List<Date> dateList = new ArrayList<Date>();
        Date start = null;
        Date end = null;
        try {
            start = getdateFormat("yyyy-MM-dd").parse(startDate);
            end = getdateFormat("yyyy-MM-dd").parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendarStartDate = Calendar.getInstance();
        Calendar calendarEndDate = Calendar.getInstance();
        calendarStartDate.setTime(start);
        calendarEndDate.setTime(end);
        calendarStartDate.set(Calendar.HOUR_OF_DAY, 0);
        calendarStartDate.set(Calendar.MINUTE, 0);
        calendarStartDate.set(Calendar.SECOND, 0);
        calendarEndDate.set(Calendar.HOUR_OF_DAY, 00);
        calendarEndDate.set(Calendar.MINUTE, 0);
        calendarEndDate.set(Calendar.SECOND, 0);
        dateList.add(calendarStartDate.getTime());
        while (true) {
            if (calendarStartDate.equals(calendarEndDate)) {
                break;
            }
            calendarStartDate.add(Calendar.DAY_OF_MONTH, 1);
            dateList.add(calendarStartDate.getTime());
        }
        return dateList;
    }

    /**
     * Get on start and end date all timestamps, by month
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static List<Date> getDatesMonthly(String startDate, String endDate) {
        List<Date> dateList = new ArrayList<Date>();
        Date start = null;
        Date end = null;
        try {
            start = getdateFormat("yyyy-MM-dd").parse(startDate);
            end = getdateFormat("yyyy-MM-dd").parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendarStartDate = Calendar.getInstance();
        Calendar calendarEndDate = Calendar.getInstance();
        calendarStartDate.setTime(start);
        calendarEndDate.setTime(end);
        calendarStartDate.set(Calendar.DAY_OF_MONTH, 1);
        calendarEndDate.set(Calendar.DAY_OF_MONTH, 1);
        dateList.add(calendarStartDate.getTime());
        while (true) {
            if (calendarStartDate.equals(calendarEndDate)) {
                break;
            }
            calendarStartDate.add(Calendar.MONTH, 1);
            dateList.add(calendarStartDate.getTime());
        }
        return dateList;
    }

    /**
     * Get on start and end date all timestamps, by year
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static List<Date> getDatesYearly(String startDate, String endDate) {
        List<Date> dateList = new ArrayList<Date>();
        Date start = null;
        Date end = null;
        try {
            start = getdateFormat("yyyy-MM-dd").parse(startDate);
            end = getdateFormat("yyyy-MM-dd").parse(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendarStartDate = Calendar.getInstance();
        Calendar calendarEndDate = Calendar.getInstance();
        calendarStartDate.setTime(start);
        calendarEndDate.setTime(end);
        calendarStartDate.set(Calendar.MONTH, 0);
        calendarStartDate.set(Calendar.DAY_OF_MONTH, 1);
        calendarEndDate.set(Calendar.MONTH, 0);
        calendarEndDate.set(Calendar.DAY_OF_MONTH, 1);
        dateList.add(calendarStartDate.getTime());
        while (true) {
            if (calendarStartDate.equals(calendarEndDate)) {
                break;
            }
            calendarStartDate.add(Calendar.YEAR, 1);
            dateList.add(calendarStartDate.getTime());
        }
        return dateList;
    }

    public static String formatStartDate(String startDate){
        Date date = formatDate(startDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return getdateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
    }

    public static String formatEndDate(String startDate){
        Date date = formatDate(startDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return getdateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
    }

    /**
     * Helper Method for formatting 8 digits to date YYYY-MM-DD
     */
    private static Date formatDate(String compromisedDate) {
        Date date = null;
        String year;
        String month;
        String day;

        year = compromisedDate.substring(0, 4);
        month = compromisedDate.substring(4, 6);
        day = compromisedDate.substring(6, 8);

        String dateString = year + "-" + month + "-" + day;
        try {
            date = getdateFormat("yyyy-MM-dd").parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date dbDateStringToDate(String dbDateString) {
        Date dbDate = null;
        try {
            dbDate = getdateFormat("yyyy-MM-dd HH:mm:ss").parse(dbDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dbDate;
    }

    public static DateFormat getdateFormat(String dateFormat){
        if(dateFormat.equals("yyyy-MM-dd HH:mm:ss")){
            return new SimpleDateFormat(dateFormat);
        }
        if(dateFormat.equals("yyyy-MM-dd")){
            return new SimpleDateFormat(dateFormat);
        }
        return null;
    }
}
