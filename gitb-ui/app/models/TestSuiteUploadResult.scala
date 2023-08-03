package models

import com.gitb.tr.TAR

import java.util

class TestSuiteUploadResult {

  var validationReport: TAR = null
  var success: Boolean = false
  var pendingTestSuiteFolderName: String = null
  var errorInformation: String = null
  var needsConfirmation: Boolean = false
  var matchingDataExists: Option[List[Long]] = None
  var existsForSpecs: Option[List[(Long, Boolean)]] = None
  val items = new util.ArrayList[TestSuiteUploadItemResult]()
  var testCases: Option[Map[Long, List[TestSuiteUploadTestCase]]] = None
  var sharedTestSuiteId: Option[Long] = None
  var sharedTestCases: Option[List[TestSuiteUploadTestCase]] = None
  var updateMetadata: Boolean = false
  var updateSpecification: Boolean = false

}
