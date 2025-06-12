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
