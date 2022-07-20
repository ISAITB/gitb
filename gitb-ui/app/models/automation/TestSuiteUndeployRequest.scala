package models.automation

case class TestSuiteUndeployRequest(
  specification: String,
  testSuite: String
)