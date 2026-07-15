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

package models

object ReportSettings {

  def defaultConfiguration(): ReportSettings = {
    ReportSettings(enabled = false, fileNameExpressions = Map[Short, String](), timeZone = None)
  }

}

/**
 * The Test Bed-wide (system administration) settings used to determine report file names and the
 * display time zone.
 *
 * @param enabled Whether custom naming expressions, a custom time zone and/or custom date format
 *                patterns are in effect. When false the built-in defaults (see
 *                [[config.Configurations.REPORT_NAMING_EXPRESSIONS]]) and the environment/platform
 *                time zone and date formats always apply.
 * @param fileNameExpressions The naming expression to use per report type (keyed by the
 *                             [[models.Enums.ReportType]] id). A report type missing from this
 *                             map falls back to the built-in default even when enabled is true.
 * @param timeZone The IANA time zone ID to use for presentation and reporting purposes when enabled
 *                 is true. When not set, or when enabled is false, the environment/platform default
 *                 (see [[config.Configurations.resolveDefaultTimeZone]]) applies.
 * @param dateFormat The pattern to use when displaying dates (without a time component) when enabled
 *                    is true. When not set, or when enabled is false, the environment/platform default
 *                    (see [[config.Configurations.resolveDefaultDateFormat]]) applies.
 * @param dateTimeFormat The pattern to use when displaying dates with a time component when enabled
 *                        is true. When not set, or when enabled is false, the environment/platform
 *                        default (see [[config.Configurations.resolveDefaultDateTimeFormat]]) applies.
 * @param dateFileFormat The pattern to use for the DATE placeholder in report file names when enabled
 *                        is true. When not set, or when enabled is false, the environment/platform
 *                        default (see [[config.Configurations.resolveDefaultDateFileFormat]]) applies.
 */
case class ReportSettings(enabled: Boolean, fileNameExpressions: Map[Short, String], timeZone: Option[String] = None,
                           dateFormat: Option[String] = None, dateTimeFormat: Option[String] = None, dateFileFormat: Option[String] = None)
