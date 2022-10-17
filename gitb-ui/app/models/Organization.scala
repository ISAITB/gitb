package models

case class Organizations(id: Long = 0, shortname: String, fullname: String, organizationType: Short, adminOrganization: Boolean, landingPage: Option[Long], legalNotice: Option[Long], errorTemplate: Option[Long], template: Boolean, templateName: Option[String], apiKey: Option[String], community: Long) {

  def withApiKey(apiKey: String): Organizations = {
    Organizations(this.id, this.shortname, this.fullname, this.organizationType, this.adminOrganization,
      this.landingPage, this.legalNotice, this.errorTemplate, this.template, this.templateName,
      Some(apiKey), this.community)
  }

}

class Organization(_id: Long, _sname: String, _fname: String, _otype: Short, _adminOrganization: Boolean,
                   _systems: Option[List[Systems]], _admin: Option[Users],
                   _landingPage: Option[Long], _landingPageObj: Option[LandingPages], _legalNotice: Option[Long], _legalNoticeObj: Option[LegalNotices], _errorTemplate: Option[Long], _errorTemplateObj: Option[ErrorTemplates],
                   _template: Boolean, _templateName: Option[String], _apiKey: Option[String],
                   _community: Option[Communities], _communityId: Long) {
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
  var apiKey: Option[String] = _apiKey
  var community: Option[Communities] = _community
  var communityId: Long = _communityId

  def this(_case: Organizations, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _errorTemplateObj: ErrorTemplates) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _case.errorTemplate, Option(_errorTemplateObj), _case.template, _case.templateName, _case.apiKey, None, _case.community)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _errorTemplateObj: ErrorTemplates, _community:Option[Communities]) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _case.errorTemplate, Option(_errorTemplateObj), _case.template, _case.templateName, _case.apiKey, _community, _case.community)

  def toCaseObject: Organizations = {
    Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, apiKey, communityId)
  }

}