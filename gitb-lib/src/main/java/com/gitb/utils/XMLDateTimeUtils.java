package com.gitb.utils;

import com.gitb.types.DataTypeFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by serbay.
 */
public class XMLDateTimeUtils {
    public static XMLGregorianCalendar getXMLGregorianCalendarDateTime() throws DatatypeConfigurationException {
        GregorianCalendar calendar = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    }

	public static XMLGregorianCalendar getXMLGregorianCalendarDate(Date date) throws DatatypeConfigurationException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
			calendar.get(Calendar.YEAR),
			calendar.get(Calendar.MONTH)+1,
			calendar.get(Calendar.DAY_OF_MONTH),
			getTimeZone(calendar));
	}

	public static XMLGregorianCalendar getXMLGregorianCalendarDate(Calendar calendar) throws DatatypeConfigurationException {
		return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
			calendar.get(Calendar.YEAR),
			calendar.get(Calendar.MONTH)+1,
			calendar.get(Calendar.DAY_OF_MONTH),
			getTimeZone(calendar));
	}

	public static XMLGregorianCalendar getXMLGregorianCalendarTime(Date date) throws DatatypeConfigurationException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return DatatypeFactory.newInstance().newXMLGregorianCalendarTime(
			calendar.get(Calendar.HOUR),
			calendar.get(Calendar.MINUTE),
			calendar.get(Calendar.SECOND),
			calendar.get(Calendar.MILLISECOND),
			getTimeZone(calendar));
	}

	public static XMLGregorianCalendar getXMLGregorianCalendarTime(Calendar calendar) throws DatatypeConfigurationException {
		return DatatypeFactory.newInstance().newXMLGregorianCalendarTime(
			calendar.get(Calendar.HOUR),
			calendar.get(Calendar.MINUTE),
			calendar.get(Calendar.SECOND),
			calendar.get(Calendar.MILLISECOND),
			getTimeZone(calendar));
	}

	public static int getTimeZone(Calendar calendar) {
		return (int) TimeUnit.MINUTES.convert(calendar.get(Calendar.ZONE_OFFSET), TimeUnit.MILLISECONDS);
	}
}
