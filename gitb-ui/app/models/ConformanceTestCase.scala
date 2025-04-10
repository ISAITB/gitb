package models

import com.gitb.tr.TestResultType

import java.sql.Timestamp

class ConformanceTestCase(
  var id: Long,
  var name: String,
  var description: Option[String],
  var version: Option[String],
  var sessionId: Option[String],
  var updateTime: Option[Timestamp],
  var outputMessage: Option[String],
  var hasDocumentation: Boolean,
  var optional: Boolean,
  var disabled: Boolean,
  var result: TestResultType,
  var tags: Option[String],
  var specReference: Option[String],
  var specDescription: Option[String],
  var specLink: Option[String],
  var group: Option[Long]
) {}
