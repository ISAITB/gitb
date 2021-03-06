package models

case class Communities(
                        id: Long = 0,
                        shortname: String,
                        fullname: String,
                        supportEmail: Option[String],
                        selfRegType: Short,
                        selfRegToken: Option[String],
                        selfRegTokenHelpText: Option[String],
                        selfregNotification: Boolean,
                        description: Option[String],
                        selfRegRestriction: Short,
                        selfRegForceTemplateSelection: Boolean,
                        selfRegForceRequiredProperties: Boolean,
                        allowCertificateDownload: Boolean,
                        allowStatementManagement: Boolean,
                        allowSystemManagement: Boolean,
                        allowPostTestOrganisationUpdates: Boolean,
                        allowPostTestSystemUpdates: Boolean,
                        allowPostTestStatementUpdates : Boolean,
                        domain: Option[Long]) {}

class Community(
                 _id:Long,
                 _shortname:String,
                 _fullname:String,
                 _supportEmail:Option[String],
                 _selfRegType: Short,
                 _selfRegToken: Option[String],
                 _selfRegTokenHelpText: Option[String],
                 _selfregNotification: Boolean,
                 _description: Option[String],
                 _selfRegRestriction: Short,
                 _selfRegForceTemplateSelection: Boolean,
                 _selfRegForceRequiredProperties: Boolean,
                 _allowCertificateDownload: Boolean,
                 _allowStatementManagement: Boolean,
                 _allowSystemManagement: Boolean,
                 _allowPostTestOrganisationUpdates: Boolean,
                 _allowPostTestSystemUpdates: Boolean,
                 _allowPostTestStatementUpdates : Boolean,
                 _domain:Option[Domain]) {
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var supportEmail:Option[String] = _supportEmail
  var selfRegType:Short = _selfRegType
  var selfRegToken:Option[String] = _selfRegToken
  var selfRegTokenHelpText: Option[String] = _selfRegTokenHelpText
  var selfRegNotification:Boolean = _selfregNotification
  var description:Option[String] = _description
  var selfRegRestriction:Short = _selfRegRestriction
  var selfRegForceTemplateSelection:Boolean = _selfRegForceTemplateSelection
  var selfRegForceRequiredProperties:Boolean = _selfRegForceRequiredProperties
  var allowCertificateDownload: Boolean = _allowCertificateDownload
  var allowStatementManagement: Boolean = _allowStatementManagement
  var allowSystemManagement: Boolean = _allowSystemManagement
  var allowPostTestOrganisationUpdates: Boolean = _allowPostTestOrganisationUpdates
  var allowPostTestSystemUpdates: Boolean = _allowPostTestSystemUpdates
  var allowPostTestStatementUpdates : Boolean = _allowPostTestStatementUpdates
  var domain:Option[Domain] = _domain

  def this(_case:Communities, _domain:Option[Domain]) =
    this(
      _case.id,
      _case.shortname,
      _case.fullname,
      _case.supportEmail,
      _case.selfRegType,
      _case.selfRegToken,
      _case.selfRegTokenHelpText,
      _case.selfregNotification,
      _case.description,
      _case.selfRegRestriction,
      _case.selfRegForceTemplateSelection,
      _case.selfRegForceRequiredProperties,
      _case.allowCertificateDownload,
      _case.allowStatementManagement,
      _case.allowSystemManagement,
      _case.allowPostTestOrganisationUpdates,
      _case.allowPostTestSystemUpdates,
      _case.allowPostTestStatementUpdates,
      _domain
    )

  def toCaseObject:Communities = {
    val d = domain match {
      case Some(d) => Some(d.id)
      case None => None
    }
    Communities(
      id,
      shortname,
      fullname,
      supportEmail,
      selfRegType,
      selfRegToken,
      selfRegTokenHelpText,
      selfRegNotification,
      description,
      selfRegRestriction,
      selfRegForceTemplateSelection,
      selfRegForceRequiredProperties,
      allowCertificateDownload,
      allowStatementManagement,
      allowSystemManagement,
      allowPostTestOrganisationUpdates,
      allowPostTestSystemUpdates,
      allowPostTestStatementUpdates,
      d
    )
  }

}