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

package models.statement

import com.gitb.reports.dto.{ConformanceItem, ConformanceStatementData}
import models.ConformanceStatementItem
import models.Enums.OverviewLevelType.OverviewLevelType

import java.sql.Timestamp
import java.util.Date

case class ConformanceData(
                            reportLevel: OverviewLevelType,
                            domainName: Option[String],
                            domainDescription: Option[String],
                            domainReportMetadata: Option[String],
                            groupName: Option[String],
                            groupDescription: Option[String],
                            groupReportMetadata: Option[String],
                            specificationName: Option[String],
                            specificationDescription: Option[String],
                            specificationReportMetadata: Option[String],
                            organisationId: Option[Long],
                            organisationName: Option[String],
                            systemId: Option[Long],
                            systemName: Option[String],
                            systemVersion: Option[String],
                            systemDescription: Option[String],
                            displayDomainInStatementTree: Boolean,
                            overallResult: String,
                            conformanceItems: java.util.List[ConformanceItem],
                            conformanceItemTree: List[ConformanceStatementItem],
                            actorLastUpdateTime: Map[Long, Timestamp],
                            reportDate: Date
                          ) {

  private var conformanceStatements: Option[java.util.List[ConformanceStatementData]] = None

  def createLocator(): ConformanceDataLocator = {
    new ConformanceDataLocator(this)
  }

  def getConformanceStatements(): java.util.List[ConformanceStatementData]  = {
    if (conformanceStatements.isEmpty) {
      conformanceStatements = Some(ConformanceItem.flattenStatements(conformanceItems))
    }
    conformanceStatements.get
  }

  def getOverallLastUpdateTime(): Option[Date] = {
    var date: Option[Date] = None
    actorLastUpdateTime.values.foreach { actorDate =>
      if (date.isEmpty || (date.get.before(actorDate))) {
        date = Some(actorDate)
      }
    }
    date
  }

}
