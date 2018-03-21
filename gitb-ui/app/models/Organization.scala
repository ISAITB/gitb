package models

case class Organizations(id: Long = 0, shortname: String, fullname: String, organizationType: Short, adminOrganization: Boolean, landingPage: Option[Long], legalNotice: Option[Long], community: Long) {

}

class Organization(_id: Long, _sname: String, _fname: String, _otype: Short, _adminOrganization: Boolean,
                   _systems: Option[List[Systems]], _admin: Option[Users], _landingPage: Option[Long], _landingPageObj: Option[LandingPages], _legalNotice: Option[Long], _legalNoticeObj: Option[LegalNotices], _community: Option[Communities]) {
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
  var community: Option[Communities] = _community

  def this(_case: Organizations) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, None, None, _case.landingPage, None, _case.legalNotice, None, None)

  def this(_case: Organizations, _landingPageObj: LandingPages) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, None, None)

  def this(_case: Organizations, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, None, _case.legalNotice, None, None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, None, None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _community:Option[Communities]) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _community)

  def toCaseObject: Organizations = {
    if (community.isDefined) {
      Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, community.get.id)
    } else {
      Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, 0)
    }
  }

}