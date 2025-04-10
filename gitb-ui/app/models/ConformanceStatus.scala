package models

import com.gitb.tr.TestResultType

import java.sql.Timestamp

class ConformanceStatus (
  var systemId: Long,
  var domainId: Long,
  var specificationId: Long,
  var actorId: Long,
  var failed: Int,
  var completed:Int,
  var undefined:Int,
  var failedOptional: Int,
  var completedOptional: Int,
  var undefinedOptional: Int,
  var failedToConsider: Int,
  var completedToConsider:Int,
  var undefinedToConsider:Int,
  var result: TestResultType,
  var updateTime: Option[Timestamp],
  var hasBadge: Boolean,
  var testSuites: Iterable[ConformanceTestSuite]
) {}
