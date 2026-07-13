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

import java.text.SimpleDateFormat
import java.util.Date
import scala.util.matching.Regex

/**
 * Resolves the file name to use when producing a report, applying (in order of precedence):
 *   1. A community-specific naming expression override (if provided).
 *   2. The Test Bed-wide naming expression, when custom system-wide report settings are enabled
 *      (see [[config.Configurations.REPORT_SETTINGS]]).
 *   3. The built-in default naming expression (see [[config.Configurations.REPORT_NAMING_EXPRESSIONS]]).
 *
 * Naming expressions may reference placeholders in the form `${TOKEN}` (e.g. `${ORGANISATION}`).
 * These are distinct from the `$TOKEN{...}` placeholders supported in report content (see
 * the `replace*Placeholders` methods in [[managers.ReportManager]]).
 */
object ReportNameResolver {

  private val FileDateFormat = "yyyy-MM-dd"
  // Characters that are unsafe/reserved in file names on common operating systems, plus control characters.
  private val UnsafeCharacters: Regex = "[\\\\/:*?\"<>|\\x00-\\x1F]".r
  private val WhitespaceRun: Regex = "\\s+".r

  /**
   * A context of values already loaded as part of report generation, used to resolve placeholders.
   * Fields left undefined resolve their placeholder to an empty string.
   */
  case class ReportNameContext(organisation: Option[String] = None,
                                system: Option[String] = None,
                                conformanceTarget: Option[String] = None,
                                testCaseName: Option[String] = None,
                                date: Date = new Date())

  /**
   * The naming expression that currently applies by default for the given report type, ignoring
   * any community-specific override. Tolerant of report types missing from the persisted system
   * settings (falls back to the built-in default), to ease future additions of report types.
   */
  def effectiveDefault(reportType: Short): String = {
    if (Configurations.REPORT_SETTINGS.enabled) {
      Configurations.REPORT_SETTINGS.fileNameExpressions.getOrElse(reportType, builtInDefault(reportType))
    } else {
      builtInDefault(reportType)
    }
  }

  private def builtInDefault(reportType: Short): String = {
    Configurations.REPORT_NAMING_EXPRESSIONS.getOrElse(reportType, "report")
  }

  /**
   * Whether the naming expression that would apply (the community override if provided, otherwise the
   * effective default) references the CONFORMANCE_TARGET placeholder. Used to decide whether it's worth
   * resolving the more expensive "should the actor be displayed" data purely for naming purposes.
   */
  def usesConformanceTarget(reportType: Short, communityOverride: Option[String]): Boolean = {
    communityOverride.filter(_.nonEmpty).getOrElse(effectiveDefault(reportType)).contains("${CONFORMANCE_TARGET}")
  }

  /**
   * Resolves the final file name (including extension) to use for a generated report.
   *
   * @param reportType The report type (see [[models.Enums.ReportType]]).
   * @param communityOverride The community-specific naming expression override, if any.
   * @param ctx The values to use to replace supported placeholders.
   * @param extension The file extension to append (without the leading dot).
   */
  def resolve(reportType: Short, communityOverride: Option[String], ctx: ReportNameContext, extension: String): String = {
    val expression = communityOverride.filter(_.nonEmpty).getOrElse(effectiveDefault(reportType))
    val resolved = sanitise(replacePlaceholders(expression, ctx))
    val nameToUse = if (resolved.isEmpty) sanitise(builtInDefault(reportType)) else resolved
    nameToUse + "." + extension
  }

  private def replacePlaceholders(expression: String, ctx: ReportNameContext): String = {
    val tokenValues = Map(
      "ORGANISATION" -> ctx.organisation.getOrElse(""),
      "SYSTEM" -> ctx.system.getOrElse(""),
      "CONFORMANCE_TARGET" -> ctx.conformanceTarget.getOrElse(""),
      "TEST_CASE_NAME" -> ctx.testCaseName.getOrElse(""),
      "DATE" -> new SimpleDateFormat(FileDateFormat).format(ctx.date)
    )
    tokenValues.foldLeft(expression) { case (current, (token, value)) =>
      current.replace("${"+token+"}", value)
    }
  }

  private def sanitise(name: String): String = {
    val withoutUnsafeCharacters = UnsafeCharacters.replaceAllIn(name, "_").trim
    WhitespaceRun.replaceAllIn(withoutUnsafeCharacters, " ").stripPrefix(".").stripSuffix(".").trim
  }

}
