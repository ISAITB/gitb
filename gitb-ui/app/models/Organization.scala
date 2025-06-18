/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package models

case class Organizations(id: Long = 0, shortname: String, fullname: String, organizationType: Short, adminOrganization: Boolean, landingPage: Option[Long], legalNotice: Option[Long], errorTemplate: Option[Long], template: Boolean, templateName: Option[String], apiKey: Option[String], community: Long) {

  def withApiKey(apiKey: String): Organizations = {
    Organizations(this.id, this.shortname, this.fullname, this.organizationType, this.adminOrganization,
      this.landingPage, this.legalNotice, this.errorTemplate, this.template, this.templateName,
      Some(apiKey), this.community)
  }

}

class Organization(_id: Long, _sname: String, _fname: String, _otype: Short, _adminOrganization: Boolean,
                   _landingPage: Option[Long], _landingPageObj: Option[LandingPages], _legalNotice: Option[Long], _legalNoticeObj: Option[LegalNotices], _errorTemplate: Option[Long], _errorTemplateObj: Option[ErrorTemplates],
                   _template: Boolean, _templateName: Option[String], _apiKey: Option[String], _communityId: Long, _communityLegalNoticeAppliesAndExists: Boolean) {
  var id: Long = _id
  var shortname: String = _sname
  var fullname: String = _fname
  var organizationType: Short = _otype
  var adminOrganization: Boolean = _adminOrganization
  var landingPage: Option[Long] = _landingPage
  var landingPageObj: Option[LandingPages] = _landingPageObj
  var legalNotice: Option[Long] = _legalNotice
  var legalNoticeObj: Option[LegalNotices] = _legalNoticeObj
  var errorTemplate: Option[Long] = _errorTemplate
  var errorTemplateObj: Option[ErrorTemplates] = _errorTemplateObj
  var template: Boolean = _template
  var templateName: Option[String] = _templateName
  var apiKey: Option[String] = _apiKey
  var communityId: Long = _communityId
  var communityLegalNoticeAppliesAndExists: Boolean = _communityLegalNoticeAppliesAndExists

  def this(_case: Organizations, _landingPageObj: LandingPages, _legalNoticeObj: LegalNotices, _errorTemplateObj: ErrorTemplates, _communityLegalNoticeAppliesAndExists: Boolean) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, _case.adminOrganization, _case.landingPage, Option(_landingPageObj), _case.legalNotice, Option(_legalNoticeObj), _case.errorTemplate, Option(_errorTemplateObj), _case.template, _case.templateName, _case.apiKey, _case.community, _communityLegalNoticeAppliesAndExists)

  def toCaseObject: Organizations = {
    Organizations(id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, apiKey, communityId)
  }

}