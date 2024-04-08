package models

case class ConformanceCertificateInfo(
    title:Option[String],
    includeTitle:Boolean,
    includeMessage:Boolean,
    includeItemStatus:Boolean,
    includeItems:Boolean,
    includeItemDetails:Boolean,
    includeDetails:Boolean,
    includeSignature:Boolean,
    includePageNumbers:Boolean,
    message: Option[String],
    keystore: Option[CommunityKeystore],
    community:Long
  ) {

  def withKeystore(keystore: CommunityKeystore): ConformanceCertificateInfo = {
    ConformanceCertificateInfo(title, includeTitle, includeMessage, includeItemStatus, includeItems, includeItemDetails, includeDetails, includeSignature, includePageNumbers, message, Some(keystore), community)
  }

}