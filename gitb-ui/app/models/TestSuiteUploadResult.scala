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

import com.gitb.tr.TAR

import java.util

object TestSuiteUploadResult {

  def success(): TestSuiteUploadResult = {
    TestSuiteUploadResult(success = true)
  }

  def success(items: List[TestSuiteUploadItemResult]): TestSuiteUploadResult = {
    TestSuiteUploadResult(success = true, items = Some(items))
  }

  def success(items: List[TestSuiteUploadItemResult], validationReport: TAR): TestSuiteUploadResult = {
    TestSuiteUploadResult(success = true, items = Some(items), validationReport = Some(validationReport))
  }

  def failure(errorInformation: String): TestSuiteUploadResult = {
    TestSuiteUploadResult(errorInformation = Some(errorInformation))
  }

  def confirm(report: TAR): TestSuiteUploadResult = {
    TestSuiteUploadResult(needsConfirmation = true, validationReport = Some(report))
  }

  def sharedTestSuiteExists(existsForSpecs: List[(Long, Boolean)], validationReport: TAR): TestSuiteUploadResult = {
    TestSuiteUploadResult(needsConfirmation = true, existsForSpecs = Some(existsForSpecs), validationReport = Some(validationReport))
  }

}

case class TestSuiteUploadResult(validationReport: Option[TAR] = None,
                                 success: Boolean = false,
                                 pendingTestSuiteFolderName: Option[String] = None,
                                 errorInformation: Option[String] = None,
                                 needsConfirmation: Boolean = false,
                                 matchingDataExists: Option[List[Long]] = None,
                                 existsForSpecs: Option[List[(Long, Boolean)]] = None,
                                 items: Option[List[TestSuiteUploadItemResult]] = None,
                                 testCases: Option[Map[Long, List[TestSuiteUploadTestCase]]] = None,
                                 sharedTestSuiteId: Option[Long] = None,
                                 sharedTestCases: Option[List[TestSuiteUploadTestCase]] = None,
                                 updateMetadata: Boolean = false,
                                 updateSpecification: Boolean = false,
                                 testSuite: Option[TestSuite] = None) {

  def withPendingFolder(pendingTestSuiteFolderName: String): TestSuiteUploadResult = {
    this.copy(pendingTestSuiteFolderName = Some(pendingTestSuiteFolderName))
  }

  def withItems(items: List[TestSuiteUploadItemResult]): TestSuiteUploadResult = {
    this.copy(
      items = Some(items),
      success = true
    )
  }

}
