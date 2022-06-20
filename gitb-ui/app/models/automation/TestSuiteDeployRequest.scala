package models.automation

case class TestSuiteDeployRequest(
  specification: String,
  ignoreWarnings: Boolean,
  replaceTestHistory: Boolean,
  updateSpecification: Boolean
)