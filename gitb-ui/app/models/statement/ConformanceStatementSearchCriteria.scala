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

package models.statement

import models.ConformanceStatementItem
import models.Enums.TestResultStatus

case class ConformanceStatementSearchCriteria(filterText: Option[String], succeeded: Boolean, failed: Boolean, incomplete: Boolean) {

  private val filterTextToApply = filterText.map(_.trim.toLowerCase)

  def filteringNeeded(): Boolean = {
    filterTextToApply.isDefined || !succeeded || !failed || !incomplete
  }

  def checkItem(item: ConformanceStatementItem, onlyCheckStatus: Boolean): Boolean = {
    var matches = if (!onlyCheckStatus && filterTextToApply.isDefined) {
      filterTextToApply.exists(item.name.toLowerCase.contains)
    } else {
      true
    }
    if (matches && item.results.isDefined) {
      matches = if (!succeeded || !failed || !incomplete) {
        val result = itemResult(item)
        (succeeded || result != TestResultStatus.SUCCESS) && (failed || result != TestResultStatus.FAILURE) && (incomplete || result != TestResultStatus.UNDEFINED)
      } else {
        true
      }
    }
    matches
  }

  private def itemResult(item: ConformanceStatementItem): TestResultStatus.Value = {
    item.results.map { result =>
      if (result.failedTestsToConsider > 0) {
        TestResultStatus.FAILURE
      } else if (result.undefinedTestsToConsider > 0) {
        TestResultStatus.UNDEFINED
      } else if (result.completedTestsToConsider > 0) {
        TestResultStatus.SUCCESS
      } else {
        TestResultStatus.UNDEFINED
      }
    }.getOrElse(TestResultStatus.UNDEFINED)
  }

}
