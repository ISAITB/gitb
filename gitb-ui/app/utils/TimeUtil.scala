/*
 * Copyright (C) 2026 European Union
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

package utils

import config.Configurations
import models.Constants

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.{Calendar, Date, TimeZone}

object TimeUtil {

  private val MS_IN_A_SECOND = 1000L
  private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern(Constants.FilterDateFormat)

  private val formatUTC: SimpleDateFormat = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.setTimeZone(TimeZone.getTimeZone("UTC"))
    format
  }

  /**
   * Parses a date/time string (as submitted by the UI as a filter bound) as wall-clock time in the application's
   * configured/default timezone (see [[Configurations.TIME_ZONE]]).
   */
  def dateFromFilterString(dateStr: Option[String]): Option[Date] = {
    if (dateStr.isEmpty) {
      None
    } else {
      Some(Date.from(LocalDateTime.parse(dateStr.get, DISPLAY_FORMATTER).atZone(Configurations.TIME_ZONE).toInstant))
    }
  }

  /**
   * Formats the provided instant for display in the application's configured/default timezone
   * (see [[Configurations.TIME_ZONE]]).
   */
  def serializeTimestamp(t:Timestamp): String = {
    t.toInstant.atZone(Configurations.TIME_ZONE).format(DISPLAY_FORMATTER)
  }

  /**
   * Formats the provided date (an instant) for display in the application's configured/default timezone
   * (see [[Configurations.TIME_ZONE]]), using the given pattern.
   */
  def formatDate(date: Date, pattern: String): String = {
    DateTimeFormatter.ofPattern(pattern).withZone(Configurations.TIME_ZONE).format(date.toInstant)
  }

  /**
   * Parses a date/time string (as submitted by the UI, e.g. as a filter bound) as wall-clock time in the
   * application's configured/default timezone (see [[Configurations.TIME_ZONE]]).
   */
  def parseTimestamp(timestamp:String): Timestamp = {
    Timestamp.from(LocalDateTime.parse(timestamp, DISPLAY_FORMATTER).atZone(Configurations.TIME_ZONE).toInstant)
  }

  def getCurrentTimestamp(): Timestamp = {
    new Timestamp(System.currentTimeMillis)
  }

  def copyTimestamp(source: Option[Timestamp]): Option[Timestamp] = {
    if (source.isDefined) {
      Some(new Timestamp(source.get.getTime))
    } else {
      None
    }
  }

  def getTimeDifferenceInSeconds(timestamp:String):Long = {
    getTimeDifference(timestamp) / MS_IN_A_SECOND
  }

  def getTimeDifferenceInSeconds(timestamp:Timestamp):Long = {
    (getCurrentTimestamp().getTime - timestamp.getTime) / MS_IN_A_SECOND
  }

  private def getTimeDifference(timestamp:String): Long = {
    val d = formatUTC.parse(timestamp)
    val curr = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime
    curr.getTime - d.getTime
  }

}
