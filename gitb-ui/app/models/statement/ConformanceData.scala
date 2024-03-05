package models.statement

import com.gitb.reports.dto.ConformanceItem
import models.{ConformanceStatement, ConformanceStatementItem}
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
                            conformanceItemTree: List[ConformanceStatementItem],
                            statements: List[ConformanceStatement]
                          ) {

  def createLocator(): ConformanceDataLocator = {
    new ConformanceDataLocator(this)
  }

}
