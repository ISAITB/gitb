package models

import java.util

class TestSuiteUploadResult {

  var success: Boolean = false
  var pendingTestSuiteFolderName: String = null
  var errorInformation: String = null

  val items = new util.ArrayList[TestSuiteUploadItemResult]()

}
