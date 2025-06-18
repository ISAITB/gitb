/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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