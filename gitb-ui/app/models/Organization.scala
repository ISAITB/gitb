package models

case class Organizations(id: Long = 0, shortname: String, fullname: String, organizationType: Short, landingPage: Option[Long], legalNotice: Option[Long]) {

}

class Organization(_id: Long, _sname: String, _fname: String, _otype: Short,
                   _systems: Option[List[Systems]], _admin: Option[Users], _landingPage: Option[Long], _landingPageObj: Option[LandingPages], _legalNotice: Option[Long], _legalNoticeObj: Option[LegalNotices]) {
  var id: Long = _id
  var shortname: String = _sname
  var fullname: String = _fname
  var organizationType: Short = _otype
  var systems: Option[List[Systems]] = _systems
  var admin: Option[Users] = _admin
  var landingPage: Option[Long] = _landingPage
  var landingPageObj: Option[LandingPages] = _landingPageObj
  var legalNotice: Option[Long] = _legalNotice
  var legalNoticeObj: Option[LegalNotices] = _legalNoticeObj

  def this(_case: Organizations) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, None, None, _case.landingPage, None, _case.legalNotice, None)

  def this(_case: Organizations, _landingPageObj: LandingPages) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, None)

  def this(_case: Organizations, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, None, None, _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj))

  def this(_case: Organizations, _systems: List[Systems], _admin: Users) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, Some(_systems), Option(_admin), _case.landingPage, None, _case.legalNotice, None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, None)

  def this(_case: Organizations, _systems: List[Systems], _admin: Users, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, Some(_systems), Option(_admin), _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj))

  def toCaseObject: Organizations = {
    Organizations(id, shortname, fullname, organizationType, landingPage, legalNotice)
  }

}
