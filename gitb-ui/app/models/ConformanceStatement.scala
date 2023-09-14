package models

import utils.TimeUtil

import java.sql.Timestamp

class ConformanceStatement(
    val domainId: Long,
    val domainName: String,
    val domainNameFull: String,
    val actorId: Long,
    val actorName: String,
    val actorFull: String,
    val specificationId: Long,
    val specificationName: String,
    val specificationNameFull: String,
    val systemId: Long,
    var result: String,
    var updateTime: Option[Timestamp],

    var completedTests: Long,
    var failedTests: Long,
    var undefinedTests: Long,
    var completedOptionalTests: Long,
    var failedOptionalTests: Long,
    var undefinedOptionalTests: Long,

    var specificationGroupId: Option[Long] = None,
    var specificationGroupName: Option[String] = None,
    var specificationDisplayOrder: Short = 0,
    var specificationGroupDisplayOrder: Option[Short] = None
) {

  def copy(): ConformanceStatement = {
    new ConformanceStatement(
      domainId, domainName, domainNameFull, actorId, actorName, actorFull,
      specificationId, specificationName, specificationNameFull, systemId,
      result, TimeUtil.copyTimestamp(this.updateTime), completedTests, failedTests, undefinedTests,
      completedOptionalTests, failedOptionalTests, undefinedOptionalTests,
      specificationGroupId, specificationGroupName, specificationDisplayOrder, specificationGroupDisplayOrder
    )
  }

}