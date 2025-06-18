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