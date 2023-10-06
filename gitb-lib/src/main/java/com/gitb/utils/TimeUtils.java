package com.gitb.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by senan on 10/10/14.
 */
public class TimeUtils {
    private static final String FORMAT_UTC  = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String FORMAT_DATE = "yyyy-MM-dd";

    private static SimpleDateFormat getUTCFormat() {
        SimpleDateFormat format =  new SimpleDateFormat(FORMAT_UTC);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(FORMAT_DATE);
    }

    public static Date getCurrentDate() {
        Calendar dCal = Calendar.getInstance();
        dCal.setTime(new Date());
        dCal.set(Calendar.HOUR_OF_DAY, 0);
        dCal.set(Calendar.MINUTE, 0);
        dCal.set(Calendar.SECOND, 0);
        dCal.set(Calendar.MILLISECOND, 0);
        return dCal.getTime();
    }

    public static Date getCurrentTime() {
        Calendar dCal = Calendar.getInstance();
        dCal.setTime(new Date());
        dCal.set(Calendar.YEAR, 0);
        dCal.set(Calendar.MONTH, 0);
        dCal.set(Calendar.DATE, 0);
        return dCal.getTime();
    }

    public static Date getCurrentDateTime() {
        Calendar dCal = Calendar.getInstance();
        return dCal.getTime();
    }

    public static String serializeUTC(Date date){
        return getUTCFormat().format(date);
    }

    public static String serializeDate(Date date) {
        return getDateFormat().format(date);
    }
}
