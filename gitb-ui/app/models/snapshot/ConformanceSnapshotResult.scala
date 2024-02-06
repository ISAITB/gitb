package models.snapshot

import java.sql.Timestamp

/**
 * Created by simatosc.
 */
case class ConformanceSnapshotResult(
  id: Long,
  snapshotId: Long,
  organisationId: Long,
  systemId: Long,
  domainId: Long,
  specGroupId: Option[Long],
  specId: Long,
  actorId: Long,
  testSuiteId: Long,
  testCaseId: Long,
  testSession: Option[String],
  result: String,
  outputMessage: Option[String],
  updateTime: Option[Timestamp]
)
