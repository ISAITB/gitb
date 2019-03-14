package models

import java.util

import com.gitb.tr.TAR

class TestSuiteUploadResult {

  var validationReport: TAR = null
  var success: Boolean = false
  var pendingTestSuiteFolderName: String = null
  var errorInformation: String = null
  var exists: Boolean = false

  val items = new util.ArrayList[TestSuiteUploadItemResult]()

}
