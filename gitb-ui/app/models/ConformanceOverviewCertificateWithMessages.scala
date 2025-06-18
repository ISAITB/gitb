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

import models.Enums.OverviewLevelType
import models.Enums.OverviewLevelType.OverviewLevelType

case class ConformanceOverviewCertificateWithMessages(settings: ConformanceOverviewCertificate, messages: Iterable[ConformanceOverviewCertificateMessage]) {

  def messageToUse(reportLevel: OverviewLevelType, reportIdentifier: Option[Long]): Option[ConformanceOverviewCertificateMessage] = {
    var customMessage: Option[ConformanceOverviewCertificateMessage] = None
    if (settings.includeMessage) {
      if (reportLevel == OverviewLevelType.OrganisationLevel) {
        customMessage = messages.find(msg => msg.messageType == OverviewLevelType.OrganisationLevel.id)
      } else if (reportLevel == OverviewLevelType.DomainLevel) {
        if (reportIdentifier.isDefined) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.DomainLevel.id && msg.domain.isDefined && msg.domain.get == reportIdentifier.get)
        }
        if (customMessage.isEmpty) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.DomainLevel.id && msg.domain.isEmpty)
        }
      } else if (reportLevel == OverviewLevelType.SpecificationGroupLevel) {
        if (reportIdentifier.isDefined) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.SpecificationGroupLevel.id && msg.group.isDefined && msg.group.get == reportIdentifier.get)
        }
        if (customMessage.isEmpty) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.SpecificationGroupLevel.id && msg.group.isEmpty)
        }
      } else { // Specification
        if (reportIdentifier.isDefined) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.SpecificationLevel.id && msg.specification.isDefined && msg.specification.get == reportIdentifier.get)
        }
        if (customMessage.isEmpty) {
          customMessage = messages.find(msg => msg.messageType == OverviewLevelType.SpecificationLevel.id && msg.specification.isEmpty)
        }
      }
    }
    customMessage
  }

}
