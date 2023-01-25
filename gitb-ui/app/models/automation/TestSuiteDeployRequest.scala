package models.automation

import models.TestCaseDeploymentAction

case class TestSuiteDeployRequest(
  specification: String,
  ignoreWarnings: Boolean,
  replaceTestHistory: Boolean,
  updateSpecification: Boolean,
  testCaseUpdates: Map[String, TestCaseDeploymentAction]
)