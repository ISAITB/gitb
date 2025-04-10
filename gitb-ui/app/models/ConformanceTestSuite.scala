package models

import com.gitb.tr.TestResultType
import models.statement.ResultCountHolder

class ConformanceTestSuite(
  var id: Long,
  var name: String,
  var description: Option[String],
  var version: Option[String],
  var hasDocumentation: Boolean,
  var specReference: Option[String],
  var specDescription: Option[String],
  var specLink: Option[String],
  var result: TestResultType,
  var failed: Long,
  var completed: Long,
  var undefined: Long,
  var failedOptional: Long,
  var completedOptional: Long,
  var undefinedOptional: Long,
  var failedToConsider: Long,
  var completedToConsider:Long,
  var undefinedToConsider:Long,
  var testCases: Iterable[ConformanceTestCase],
  var testCaseGroups: Iterable[TestCaseGroup]
) extends ResultCountHolder {

  override def completedCount(): Long = completed

  override def failedCount(): Long = failed

  override def otherCount(): Long = undefined
}
