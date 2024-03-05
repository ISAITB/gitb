package models

case class ConformanceCertificate(
   id:Long,
   title:Option[String],
   includeTitle:Boolean,
   includeMessage:Boolean,
   includeTestStatus:Boolean,
   includeTestCases:Boolean,
   includeDetails:Boolean,
   includeSignature:Boolean,
   includePageNumbers:Boolean,
   message: Option[String],
   community:Long
) {

  def withMessage(message: String): ConformanceCertificate = {
    ConformanceCertificate(id, title, includeTitle, includeMessage, includeTestStatus, includeTestCases, includeDetails, includeSignature, includePageNumbers, message = Some(message), community)
  }

  def toConformanceCertificateInfo(keystore: Option[CommunityKeystore]): ConformanceCertificateInfo = {
    ConformanceCertificateInfo(
      title, includeTitle, includeMessage, includeTestStatus, includeTestCases, includeItemDetails = false, includeDetails, includeSignature, includePageNumbers,
      message, keystore, community
    )
  }

}
