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

package models

import models.statement.ResultHolder

class ConformanceStatementResultData(
                                      var result: String,
                                      var completedTests: Long,
                                      var failedTests: Long,
                                      var undefinedTests: Long,
                                      var completedOptionalTests: Long,
                                      var failedOptionalTests: Long,
                                      var undefinedOptionalTests: Long,
                                      var completedTestsToConsider: Long,
                                      var failedTestsToConsider: Long,
                                      var undefinedTestsToConsider: Long,
                                    ) extends ResultHolder {

  def this() = {
    this("UNDEFINED", 0, 0, 0, 0, 0, 0, 0, 0, 0)
  }

  override def resultStatus(): String = {
    result
  }

  def copy(): ConformanceStatementResultData = {
    new ConformanceStatementResultData(
      result,
      completedTests, failedTests, undefinedTests,
      completedOptionalTests, failedOptionalTests, undefinedOptionalTests,
      completedTestsToConsider, failedTestsToConsider, undefinedTestsToConsider
    )
  }

}
