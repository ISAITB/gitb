package models

class TestSuiteUploadItemResult(var itemName: String, var itemType: String, var actionType: String, var specification: Long) {
}

object TestSuiteUploadItemResult {

  val ITEM_TYPE_ACTOR = "actor"
  val ITEM_TYPE_ENDPOINT = "endpoint"
  val ITEM_TYPE_PARAMETER = "parameter"
  val ITEM_TYPE_TEST_CASE = "testCase"
  val ITEM_TYPE_TEST_SUITE = "testSuite"

  val ACTION_TYPE_ADD = "add"
  val ACTION_TYPE_UPDATE = "update"
  val ACTION_TYPE_REMOVE = "remove"
  val ACTION_TYPE_UNCHANGED = "unchanged"

}