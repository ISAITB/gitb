package models

import com.gitb.tr.TestResultType

class ConformanceTestSuite(
  var id: Long,
  var name: String,
  var description: Option[String],
  var hasDocumentation: Boolean,
  var result: TestResultType,
  var failed: Int,
  var completed: Int,
  var undefined: Int,
  var failedOptional: Int,
  var completedOptional: Int,
  var undefinedOptional: Int,
  var testCases: Iterable[ConformanceTestCase]
) {}
