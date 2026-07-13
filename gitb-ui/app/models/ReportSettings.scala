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
    ReportSettings(enabled = false, fileNameExpressions = Map[Short, String]())
  }

}

/**
 * The Test Bed-wide (system administration) settings used to determine report file names.
 *
 * @param enabled Whether custom naming expressions are in effect. When false the built-in
 *                defaults (see [[config.Configurations.REPORT_NAMING_EXPRESSIONS]]) always apply.
 * @param fileNameExpressions The naming expression to use per report type (keyed by the
 *                             [[models.Enums.ReportType]] id). A report type missing from this
 *                             map falls back to the built-in default even when enabled is true.
 */
case class ReportSettings(enabled: Boolean, fileNameExpressions: Map[Short, String])
