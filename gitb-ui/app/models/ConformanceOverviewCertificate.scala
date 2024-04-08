package models

case class ConformanceOverviewCertificate(
    id: Long,
    title: Option[String],
    includeTitle: Boolean,
    includeMessage: Boolean,
    includeStatementStatus: Boolean,
    includeStatements: Boolean,
    includeStatementDetails: Boolean,
    includeDetails: Boolean,
    includeSignature: Boolean,
    includePageNumbers: Boolean,
    enableAllLevel: Boolean,
    enableDomainLevel: Boolean,
    enableGroupLevel: Boolean,
    enableSpecificationLevel: Boolean,
    community:Long
) {

  def toConformanceCertificateInfo(message: Option[ConformanceOverviewCertificateMessage], keystore: Option[CommunityKeystore]): ConformanceCertificateInfo = {
    ConformanceCertificateInfo(
      title, includeTitle, includeMessage, includeStatementStatus, includeStatements, includeStatementDetails, includeDetails, includeSignature, includePageNumbers,
      message.map(_.message), keystore, community
    )
  }

}

