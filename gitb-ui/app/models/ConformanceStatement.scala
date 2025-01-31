package models

import utils.TimeUtil

import java.sql.Timestamp

class ConformanceStatement(
    val domainId: Long,
    val domainName: String,
    val domainNameFull: String,
    val domainDescription: Option[String],
    val domainReportMetadata: Option[String],
    val actorId: Long,
    val actorName: String,
    val actorFull: String,
    val actorDescription: Option[String],
    val actorReportMetadata: Option[String],
    val specificationId: Long,
    val specificationName: String,
    val specificationNameFull: String,
    val specificationDescription: Option[String],
    val specificationReportMetadata: Option[String],
    val systemId: Long,
    result: String,
    var updateTime: Option[Timestamp],

    completedTests: Long,
    failedTests: Long,
    undefinedTests: Long,
    completedOptionalTests: Long,
    failedOptionalTests: Long,
    undefinedOptionalTests: Long,
    completedTestsToConsider: Long,
    failedTestsToConsider: Long,
    undefinedTestsToConsider: Long,

    var specificationGroupId: Option[Long] = None,
    var specificationGroupName: Option[String] = None,
    var specificationGroupDescription: Option[String] = None,
    var specificationGroupReportMetadata: Option[String] = None,
    var specificationDisplayOrder: Short = 0,
    var specificationGroupDisplayOrder: Option[Short] = None
) extends ConformanceStatementResultData(
  result, completedTests: Long, failedTests: Long, undefinedTests: Long,
  completedOptionalTests: Long, failedOptionalTests: Long, undefinedOptionalTests: Long,
  completedTestsToConsider: Long, failedTestsToConsider: Long, undefinedTestsToConsider: Long
) {

  override def copy(): ConformanceStatement = {
    new ConformanceStatement(
      domainId, domainName, domainNameFull, domainDescription, domainReportMetadata, actorId, actorName, actorFull, actorDescription, actorReportMetadata,
      specificationId, specificationName, specificationNameFull, specificationDescription, specificationReportMetadata, systemId,
      result, TimeUtil.copyTimestamp(this.updateTime), completedTests, failedTests, undefinedTests,
      completedOptionalTests, failedOptionalTests, undefinedOptionalTests,
      completedTestsToConsider, failedTestsToConsider, undefinedTestsToConsider,
      specificationGroupId, specificationGroupName, specificationGroupDescription, specificationGroupReportMetadata, specificationDisplayOrder, specificationGroupDisplayOrder
    )
  }

}