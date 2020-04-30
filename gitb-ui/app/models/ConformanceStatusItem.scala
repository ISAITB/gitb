package models

case class ConformanceStatusItem(testSuiteId: Long, testSuiteName: String, testSuiteDescription: Option[String], testSuiteHasDocumentation: Boolean, testCaseId: Long, testCaseName: String, testCaseDescription: Option[String], testCaseHasDocumentation: Boolean, result: String, sessionId: Option[String])