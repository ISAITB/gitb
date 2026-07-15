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

import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

object TimeUtil {

  private val MS_IN_A_SECOND = 1000L

  /**
   * Parses a date/time string (as submitted by the UI as a filter bound) as wall-clock time in the application's
   * configured/default timezone and date/time format (see [[Configurations.DATE_TIME_FORMATTER]]).
   */
  def dateFromFilterString(dateStr: Option[String]): Option[Date] = {
    dateStr.map(str => Date.from(Instant.from(Configurations.DATE_TIME_FORMATTER.parse(str))))
  }

  /**
   * Formats the provided instant for display using the application's configured/default timezone and
   * date/time format (see [[Configurations.DATE_TIME_FORMATTER]]).
   */
  def serializeTimestamp(t: Timestamp): String = {
    Configurations.DATE_TIME_FORMATTER.format(t.toInstant)
  }

  /**
   * Formats the provided date (an instant) for display in the application's configured/default timezone
   * (see [[Configurations.TIME_ZONE]]), using the given (arbitrary, not necessarily the configured)
   * pattern. Prefer [[formatDateTime]] or [[formatFileDate]] when formatting using one of the currently
   * configured patterns, to avoid re-parsing the pattern string on every call.
   */
  def formatDate(date: Date, pattern: String): String = {
    DateTimeFormatter.ofPattern(pattern).withZone(Configurations.TIME_ZONE).format(date.toInstant)
  }

  /**
   * Formats the provided date (an instant) for display using the application's configured/default
   * timezone and date/time format (see [[Configurations.DATE_TIME_FORMATTER]]).
   */
  def formatDateTime(date: Date): String = {
    Configurations.DATE_TIME_FORMATTER.format(date.toInstant)
  }

  /**
   * Formats the provided date (an instant) for use in report file names, using the application's
   * configured/default timezone and file name date format (see [[Configurations.DATE_FILE_FORMATTER]]).
   */
  def formatFileDate(date: Date): String = {
    Configurations.DATE_FILE_FORMATTER.format(date.toInstant)
  }

  /**
   * Parses a date/time string (as submitted by the UI, e.g. as a filter bound) as wall-clock time in the
   * application's configured/default timezone and date/time format (see [[Configurations.DATE_TIME_FORMATTER]]).
   */
  def parseTimestamp(timestamp: String): Timestamp = {
    Timestamp.from(Instant.from(Configurations.DATE_TIME_FORMATTER.parse(timestamp)))
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

  def getTimeDifferenceInSeconds(timestamp:Timestamp):Long = {
    (getCurrentTimestamp().getTime - timestamp.getTime) / MS_IN_A_SECOND
  }

}
