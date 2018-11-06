package models

case class ConformanceCertificates (id:Long, title:Option[String], message: Option[String], includeMessage:Boolean, includeTestStatus:Boolean, includeTestCases:Boolean, includeDetails:Boolean, includeSignature:Boolean, keystoreFile: Option[String], keystoreType: Option[String], keystorePassword: Option[String], keyPassword: Option[String], community:Long) {
}

class ConformanceCertificate (_id:Long, _title:Option[String], _message: Option[String], _includeMessage:Boolean, _includeTestStatus:Boolean, _includeTestCases:Boolean, _includeDetails:Boolean, _includeSignature:Boolean, _keystoreFile: Option[String], _keystoreType: Option[String], _keystorePassword: Option[String], _keyPassword: Option[String], _community:Long)
{
  var id:Long = _id
  var title:Option[String] = _title
  var message:Option[String] = _message
  var includeMessage:Boolean = _includeMessage
  var includeTestStatus:Boolean = _includeTestStatus
  var includeTestCases:Boolean = _includeTestCases
  var includeDetails:Boolean = _includeDetails
  var includeSignature:Boolean = _includeSignature
  var keystoreFile:Option[String] = _keystoreFile
  var keystoreType:Option[String] = _keystoreType
  var keystorePassword:Option[String] = _keystorePassword
  var keyPassword:Option[String] = _keyPassword
  var community:Long = _community

  def this(_case:ConformanceCertificates) =
    this(_case.id, _case.title, _case.message, _case.includeMessage, _case.includeTestStatus, _case.includeTestCases, _case.includeDetails, _case.includeSignature, _case.keystoreFile, _case.keystoreType, _case.keystorePassword, _case.keyPassword, _case.community)

  def toCaseObject:ConformanceCertificates = {
    ConformanceCertificates(id, title, message, includeMessage, includeTestStatus, includeTestCases, includeDetails, includeSignature, keystoreFile, keystoreType, keystorePassword, keyPassword, community)
  }

}

