/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
