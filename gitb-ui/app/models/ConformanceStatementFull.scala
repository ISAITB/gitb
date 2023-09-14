package models

import utils.TimeUtil

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
    specificationGroupName: Option[String],
    val specificationGroupNameFull: Option[String],
    val specificationGroupOptionName: String,
    val specificationGroupOptionNameFull: String,
    val testSuiteId: Option[Long],
    val testSuiteName: Option[String],
    val testSuiteDescription: Option[String],
    val testCaseId: Option[Long],
    val testCaseName: Option[String],
    val testCaseDescription: Option[String],
    val testCaseOptional: Option[Boolean],
    val testCaseDisabled: Option[Boolean],
    val testCaseTags: Option[String],
    val testCaseOrder: Option[Short],
    result: String,
    val outputMessage: Option[String],
    val sessionId: Option[String],
    updateTime: Option[Timestamp],
    completedTests: Long,
    failedTests: Long,
    undefinedTests: Long,
    completedOptionalTests: Long,
    failedOptionalTests: Long,
    undefinedOptionalTests: Long
) extends ConformanceStatement(
  domainId, domainName, domainNameFull,
  actorId, actorName, actorFull,
  specificationId, specificationName, specificationNameFull, systemId,
  result, updateTime, completedTests, failedTests, undefinedTests, completedOptionalTests, failedOptionalTests, undefinedOptionalTests, None, specificationGroupName) {


  override def copy(): ConformanceStatementFull = {
    new ConformanceStatementFull(
      this.communityId,
      this.communityName,
      this.organizationId,
      this.organizationName,
      this.systemId,
      this.systemName,
      this.domainId,
      this.domainName,
      this.domainNameFull,
      this.actorId,
      this.actorName,
      this.actorFull,
      this.specificationId,
      this.specificationName,
      this.specificationNameFull,
      this.specificationGroupName,
      this.specificationGroupNameFull,
      this.specificationGroupOptionName,
      this.specificationGroupOptionNameFull,
      this.testSuiteId,
      this.testSuiteName,
      this.testSuiteDescription,
      this.testCaseId,
      this.testCaseName,
      this.testCaseDescription,
      this.testCaseOptional,
      this.testCaseDisabled,
      this.testCaseTags,
      this.testCaseOrder,
      this.result,
      this.outputMessage,
      this.sessionId,
      TimeUtil.copyTimestamp(this.updateTime),
      this.completedTests,
      this.failedTests,
      this.undefinedTests,
      this.completedOptionalTests,
      this.failedOptionalTests,
      this.undefinedOptionalTests
    )
  }

}