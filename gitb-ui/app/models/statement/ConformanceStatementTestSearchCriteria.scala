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

import models.ConformanceTestCase
import models.Enums.TestResultStatus
import models.ConformanceTestSuite
import com.gitb.tr.TestResultType

case class ConformanceStatementTestSearchCriteria(succeeded: Boolean, failed: Boolean, incomplete: Boolean, optional: Boolean, disabled: Boolean, testSuiteId: Option[Long], testSuiteFilterText: Option[String], testCaseFilterText: Option[String]) {

    val testCaseFilterTextToUse = testCaseFilterText.map(_.trim.toLowerCase()).filter(_.nonEmpty)

    def matchesTestSuite(testSuite: ConformanceTestSuite): Boolean = {
        testSuiteId.isEmpty || testSuiteId.contains(testSuite.id)
    }

    def matchesTestCase(testCase: ConformanceTestCase): Boolean = {
        var matches = (optional || !testCase.optional) && (disabled || !testCase.disabled)
        if (matches) {
            matches = testCase.result match {
                case TestResultType.SUCCESS => succeeded
                case TestResultType.WARNING => succeeded
                case TestResultType.FAILURE => failed
                case _ => incomplete
            }
            if (matches) {
                matches = testCaseFilterTextToUse.isEmpty || testCase.name.toLowerCase.contains(testCaseFilterTextToUse.get.toLowerCase)
            }
        }
        matches
    }

}