package models.statement

import java.sql.Timestamp

case class ConformanceStatementResults(
  updateTime: Option[Timestamp],
  completedTests: Long,
  failedTests: Long,
  undefinedTests: Long,
  completedOptionalTests: Long,
  failedOptionalTests: Long,
  undefinedOptionalTests: Long
)
