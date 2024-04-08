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
