package managers.testsuite

import models.TestSuites

case class TestSuiteSaveResult(
  testSuite: TestSuites,
  updatedTestCases: Map[String, (Long, Boolean)],
  deletedTestCaseNames: List[String],
  pathInfo: TestSuitePaths,
  isLinked: Boolean = false
)
