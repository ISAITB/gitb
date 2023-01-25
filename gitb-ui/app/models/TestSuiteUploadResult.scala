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
  var existsForSpecs: Option[List[Long]] = None
  val items = new util.ArrayList[TestSuiteUploadItemResult]()
  var testCases: Option[Map[Long, List[TestSuiteUploadTestCase]]] = None

}
