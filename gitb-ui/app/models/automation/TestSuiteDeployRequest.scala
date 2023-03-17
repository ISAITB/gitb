package models.automation

import models.TestCaseDeploymentAction

case class TestSuiteDeployRequest(
  specification: Option[String],
  ignoreWarnings: Boolean,
  replaceTestHistory: Boolean,
  updateSpecification: Boolean,
  testCaseUpdates: Map[String, TestCaseDeploymentAction],
  sharedTestSuite: Boolean
)