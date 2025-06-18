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

	public static XMLGregorianCalendar getXMLGregorianCalendarDateTime(Date date) throws DatatypeConfigurationException {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
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
