package models.statement

import com.gitb.reports.dto.ConformanceItem

case class ConformanceData(
                            domainName: Option[String],
                            groupName: Option[String],
                            specificationName: Option[String],
                            organisationName: Option[String],
                            systemName: Option[String],
                            displayDomainInStatementTree: Boolean,
                            overallResult: String,
                            conformanceItems: java.util.List[ConformanceItem]
                          )
