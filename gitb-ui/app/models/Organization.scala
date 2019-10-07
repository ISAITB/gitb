package models

case class Organizations(id: Long = 0, shortname: String, fullname: String, organizationType: Short, adminOrganization: Boolean, landingPage: Option[Long], legalNotice: Option[Long], errorTemplate: Option[Long], template: Boolean, templateName: Option[String], community: Long) {

}

class Organization(_id: Long, _sname: String, _fname: String, _otype: Short, _adminOrganization: Boolean,
                   _systems: Option[List[Systems]], _admin: Option[Users],
                   _landingPage: Option[Long], _landingPageObj: Option[LandingPages], _legalNotice: Option[Long], _legalNoticeObj: Option[LegalNotices], _errorTemplate: Option[Long], _errorTemplateObj: Option[ErrorTemplates],
                   _template: Boolean, _templateName: Option[String],
                   _community: Option[Communities]) {
  var id: Long = _id
  var shortname: String = _sname
  var fullname: String = _fname
  var organizationType: Short = _otype
  var systems: Option[List[Systems]] = _systems
  var adminOrganization: Boolean = _adminOrganization
  var admin: Option[Users] = _admin
  var landingPage: Option[Long] = _landingPage
  var landingPageObj: Option[LandingPages] = _landingPageObj
  var legalNotice: Option[Long] = _legalNotice
  var legalNoticeObj: Option[LegalNotices] = _legalNoticeObj
  var errorTemplate: Option[Long] = _errorTemplate
  var errorTemplateObj: Option[ErrorTemplates] = _errorTemplateObj
  var template: Boolean = _template
  var templateName: Option[String] = _templateName
  var community: Option[Communities] = _community

  def this(_case: Organizations, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _errorTemplateObj: ErrorTemplates) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _case.errorTemplate, Option(_errorTemplateObj), _case.template, _case.templateName, None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _errorTemplateObj: ErrorTemplates, _community:Option[Communities]) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _case.errorTemplate, Option(_errorTemplateObj), _case.template, _case.templateName, _community)

  def toCaseObject: Organizations = {
    if (community.isDefined) {
      Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, community.get.id)
    } else {
      Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, 0)
    }
  }

}