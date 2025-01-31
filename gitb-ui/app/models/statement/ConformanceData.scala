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
