package models.automation

case class TestSuiteUndeployRequest(
  specification: Option[String],
  testSuite: String,
  sharedTestSuite: Boolean
)