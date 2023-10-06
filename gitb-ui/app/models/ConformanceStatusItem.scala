package models

import java.sql.Timestamp

case class ConformanceStatusItem(
  testSuiteId: Long, testSuiteName: String, testSuiteDescription: Option[String], testSuiteHasDocumentation: Boolean,
  testCaseId: Long, testCaseName: String, testCaseDescription: Option[String], testCaseHasDocumentation: Boolean,
  result: String, outputMessage: Option[String], sessionId: Option[String], sessionTime: Option[Timestamp],
  testCaseOptional: Boolean, testCaseDisabled: Boolean, testCaseTags: Option[String]
)