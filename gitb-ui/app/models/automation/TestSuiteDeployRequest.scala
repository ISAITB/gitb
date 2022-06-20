package models.automation

case class TestSuiteDeployRequest(
  specification: String,
  testSuite: String,
  ignoreWarnings: Boolean,
  replaceTestHistory: Boolean,
  updateSpecification: Boolean
)