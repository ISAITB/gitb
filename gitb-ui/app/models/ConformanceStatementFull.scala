package models

import java.sql.Timestamp

class ConformanceStatementFull(
    val communityId: Long,
    val communityName: String,
    val organizationId: Long,
    val organizationName: String,
    systemId: Long,
    val systemName: String,
    domainId: Long,
    domainName: String,
    domainNameFull: String,
    actorId: Long,
    actorName: String,
    actorFull: String,
    specificationId: Long,
    specificationName: String,
    specificationNameFull: String,
    val testSuiteName: Option[String],
    val testCaseName: Option[String],
    val testCaseDescription: Option[String],
    result: String,
    val outputMessage: Option[String],
    val sessionId: Option[String],
    updateTime: Option[Timestamp],
    completedTests: Long,
    failedTests: Long,
    undefinedTests: Long
) extends ConformanceStatement(
  domainId, domainName, domainNameFull,
  actorId, actorName, actorFull,
  specificationId, specificationName, specificationNameFull, systemId,
  result, updateTime, completedTests, failedTests, undefinedTests) {}