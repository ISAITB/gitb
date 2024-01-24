package models

import java.sql.Timestamp

case class ConformanceStatusItem(
  testSuiteId: Long, testSuiteName: String, testSuiteDescription: Option[String], testSuiteHasDocumentation: Boolean, testSuiteSpecReference: Option[String], testSuiteSpecDescription: Option[String], testSuiteSpecLink: Option[String],
  testCaseId: Long, testCaseName: String, testCaseDescription: Option[String], testCaseHasDocumentation: Boolean, testCaseSpecReference: Option[String], testCaseSpecDescription: Option[String], testCaseSpecLink: Option[String],
  result: String, outputMessage: Option[String], sessionId: Option[String], sessionTime: Option[Timestamp],
  testCaseOptional: Boolean, testCaseDisabled: Boolean, testCaseTags: Option[String]
)