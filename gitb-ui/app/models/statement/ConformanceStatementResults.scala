package models.statement

import models.ConformanceTestSuite

import java.sql.Timestamp

case class ConformanceStatementResults(
  updateTime: Option[Timestamp],
  completedTests: Long,
  failedTests: Long,
  undefinedTests: Long,
  completedOptionalTests: Long,
  failedOptionalTests: Long,
  undefinedOptionalTests: Long,
  completedTestsToConsider: Long,
  failedTestsToConsider: Long,
  undefinedTestsToConsider: Long,
  testSuites: Option[Iterable[ConformanceTestSuite]] = None
)
