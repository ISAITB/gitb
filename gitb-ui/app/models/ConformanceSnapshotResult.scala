package models

import java.sql.Timestamp

/**
 * Created by simatosc.
 */
case class ConformanceSnapshotResult(
  id: Long,
  snapshotId: Long,
  organisationId: Long,
  organisation: String,
  systemId: Long,
  system: String,
  domainId: Long,
  domain: String,
  specGroupId: Option[Long],
  specGroup: Option[String],
  specGroupDisplayOrder: Option[Short],
  specId: Long,
  spec: String,
  specDisplayOrder: Short,
  actorId: Long,
  actor: String,
  testSuiteId: Long,
  testSuite: String,
  testSuiteDescription: Option[String],
  testCaseId: Long,
  testCase: String,
  testCaseDescription: Option[String],
  testCaseOrder: Short,
  testCaseOptional: Boolean,
  testCaseDisabled: Boolean,
  testCaseTags: Option[String],
  testSession: Option[String],
  result: String,
  outputMessage: Option[String],
  updateTime: Option[Timestamp]
)
