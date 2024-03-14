package models.statement

import com.gitb.reports.dto.{ConformanceItem, ConformanceStatementData}
import models.ConformanceStatementItem
import models.Enums.OverviewLevelType.OverviewLevelType

case class ConformanceData(
                            reportLevel: OverviewLevelType,
                            domainName: Option[String],
                            groupName: Option[String],
                            specificationName: Option[String],
                            organisationId: Option[Long],
                            organisationName: Option[String],
                            systemId: Option[Long],
                            systemName: Option[String],
                            displayDomainInStatementTree: Boolean,
                            overallResult: String,
                            conformanceItems: java.util.List[ConformanceItem],
                            conformanceItemTree: List[ConformanceStatementItem]
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

}
