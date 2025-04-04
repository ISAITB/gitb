package managers.`export`

import models._

case class CommunityExportData(community: Option[Communities],
                               domainExportData: Option[DomainExportData],
                               administrators: Option[Seq[Users]],
                               statementCertificateSettings: Option[Option[ConformanceCertificate]],
                               overviewCertificateSettings: Option[Option[ConformanceOverviewCertificateWithMessages]],
                               keystore: Option[Option[CommunityKeystore]],
                               reportSettings: Option[List[CommunityReportSettings]],
                               organisationProperties: Option[Seq[OrganisationParameters]],
                               systemProperties: Option[Seq[SystemParameters]],
                               labels: Option[List[CommunityLabels]],
                               landingPages: Option[List[LandingPages]],
                               legalNotices: Option[List[LegalNotices]],
                               errorTemplates: Option[List[ErrorTemplates]],
                               triggers: Option[List[Trigger]],
                               resources: Option[List[CommunityResources]],
                               organisations: Option[Seq[Organizations]],
                               organisationUserMap: Option[Map[Long, List[models.Users]]],
                               organisationParameterValueMap: Option[Map[Long, List[models.OrganisationParameterValues]]],
                               organisationSystemMap: Option[Map[Long, List[models.Systems]]],
                               systemParameterValueMap: Option[Map[Long, List[models.SystemParameterValues]]],
                               systemStatementsMap: Option[Map[Long, List[(models.Specifications, models.Actors)]]], // System ID to [specification, actor]
                               systemConfigurationsMap: Option[Map[String, List[models.Configs]]] , // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
                               systemSettings: Option[SystemSettingsExportData]
                              )