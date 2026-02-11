/*
 * Copyright (C) 2026 European Union
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

case class Communities(
                        id: Long = 0,
                        shortname: String,
                        fullname: String,
                        supportEmail: Option[String],
                        selfRegType: Short,
                        selfRegToken: Option[String],
                        selfRegTokenHelpText: Option[String],
                        selfRegNotification: Boolean,
                        interactionNotification: Boolean,
                        description: Option[String],
                        selfRegRestriction: Short,
                        selfRegForceTemplateSelection: Boolean,
                        selfRegForceRequiredProperties: Boolean,
                        selfRegAllowOrganisationTokens: Boolean,
                        selfRegAllowOrganisationTokenManagement: Boolean,
                        selfRegForceOrganisationTokenInput: Boolean,
                        selfRegJoinExisting: Boolean,
                        selfRegJoinAsAdmin: Boolean,
                        allowCertificateDownload: Boolean,
                        allowStatementManagement: Boolean,
                        allowSystemManagement: Boolean,
                        allowPostTestOrganisationUpdates: Boolean,
                        allowPostTestSystemUpdates: Boolean,
                        allowPostTestStatementUpdates : Boolean,
                        allowAutomationApi : Boolean,
                        allowCommunityView : Boolean,
                        allowUserManagement: Boolean,
                        apiKey: String,
                        latestStatusLabel: Option[String],
                        domain: Option[Long]) {

  def withApiKey(apiKey: String): Communities = {
    copy(apiKey = apiKey)
  }

}

class Community(
                 _id:Long,
                 _shortname:String,
                 _fullname:String,
                 _supportEmail:Option[String],
                 _selfRegType: Short,
                 _selfRegToken: Option[String],
                 _selfRegTokenHelpText: Option[String],
                 _selfRegNotification: Boolean,
                 _interactionNotification: Boolean,
                 _description: Option[String],
                 _selfRegRestriction: Short,
                 _selfRegForceTemplateSelection: Boolean,
                 _selfRegForceRequiredProperties: Boolean,
                 _selfRegAllowOrganisationTokens: Boolean,
                 _selfRegAllowOrganisationTokenManagement: Boolean,
                 _selfRegForceOrganisationTokenInput: Boolean,
                 _selfRegJoinExisting: Boolean,
                 _selfRegJoinAsAdmin: Boolean,
                 _allowCertificateDownload: Boolean,
                 _allowStatementManagement: Boolean,
                 _allowSystemManagement: Boolean,
                 _allowPostTestOrganisationUpdates: Boolean,
                 _allowPostTestSystemUpdates: Boolean,
                 _allowPostTestStatementUpdates : Boolean,
                 _allowAutomationApi: Boolean,
                 _allowCommunityView: Boolean,
                 _allowUserManagement: Boolean,
                 _apiKey: String,
                 _domain:Option[Domain],
                 _defaultSelfRegOrganisation: Option[Organizations],
                 _defaultUserPreferences: Option[UserPreferenceDefaults]) {
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var supportEmail:Option[String] = _supportEmail
  var selfRegType:Short = _selfRegType
  var selfRegToken:Option[String] = _selfRegToken
  var selfRegTokenHelpText: Option[String] = _selfRegTokenHelpText
  var selfRegNotification:Boolean = _selfRegNotification
  var interactionNotification:Boolean = _interactionNotification
  var description:Option[String] = _description
  var selfRegRestriction:Short = _selfRegRestriction
  var selfRegForceTemplateSelection:Boolean = _selfRegForceTemplateSelection
  var selfRegForceRequiredProperties:Boolean = _selfRegForceRequiredProperties
  var selfRegAllowOrganisationTokens:Boolean = _selfRegAllowOrganisationTokens
  var selfRegAllowOrganisationTokenManagement:Boolean = _selfRegAllowOrganisationTokenManagement
  var selfRegForceOrganisationTokenInput:Boolean = _selfRegForceOrganisationTokenInput
  var selfRegJoinExisting:Boolean = _selfRegJoinExisting
  var selfRegJoinAsAdmin:Boolean = _selfRegJoinAsAdmin
  var allowCertificateDownload: Boolean = _allowCertificateDownload
  var allowStatementManagement: Boolean = _allowStatementManagement
  var allowSystemManagement: Boolean = _allowSystemManagement
  var allowPostTestOrganisationUpdates: Boolean = _allowPostTestOrganisationUpdates
  var allowPostTestSystemUpdates: Boolean = _allowPostTestSystemUpdates
  var allowPostTestStatementUpdates: Boolean = _allowPostTestStatementUpdates
  var allowAutomationApi: Boolean = _allowAutomationApi
  var allowCommunityView: Boolean = _allowCommunityView
  var allowUserManagement: Boolean = _allowUserManagement
  var apiKey: String  = _apiKey
  var domain:Option[Domain] = _domain
  var defaultSelfRegOrganisation: Option[Organizations] = _defaultSelfRegOrganisation
  var defaultUserPreferences: Option[UserPreferenceDefaults] = _defaultUserPreferences

  def this(_case:Communities, _domain:Option[Domain], _defaultSelfRegOrganisation: Option[Organizations], _defaultUserPreferences: Option[UserPreferenceDefaults]) =
    this(
      _case.id,
      _case.shortname,
      _case.fullname,
      _case.supportEmail,
      _case.selfRegType,
      _case.selfRegToken,
      _case.selfRegTokenHelpText,
      _case.selfRegNotification,
      _case.interactionNotification,
      _case.description,
      _case.selfRegRestriction,
      _case.selfRegForceTemplateSelection,
      _case.selfRegForceRequiredProperties,
      _case.selfRegAllowOrganisationTokens,
      _case.selfRegAllowOrganisationTokenManagement,
      _case.selfRegForceOrganisationTokenInput,
      _case.selfRegJoinExisting,
      _case.selfRegJoinAsAdmin,
      _case.allowCertificateDownload,
      _case.allowStatementManagement,
      _case.allowSystemManagement,
      _case.allowPostTestOrganisationUpdates,
      _case.allowPostTestSystemUpdates,
      _case.allowPostTestStatementUpdates,
      _case.allowAutomationApi,
      _case.allowCommunityView,
      _case.allowUserManagement,
      _case.apiKey,
      _domain,
      _defaultSelfRegOrganisation,
      _defaultUserPreferences
    )

  def this(_case:Communities, _domain:Option[Domain], _defaultSelfRegOrganisation: Option[Organizations]) =
    this(_case, _domain, _defaultSelfRegOrganisation, None)

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
      interactionNotification,
      description,
      selfRegRestriction,
      selfRegForceTemplateSelection,
      selfRegForceRequiredProperties,
      selfRegAllowOrganisationTokens,
      selfRegAllowOrganisationTokenManagement,
      selfRegForceOrganisationTokenInput,
      selfRegJoinExisting,
      selfRegJoinAsAdmin,
      allowCertificateDownload,
      allowStatementManagement,
      allowSystemManagement,
      allowPostTestOrganisationUpdates,
      allowPostTestSystemUpdates,
      allowPostTestStatementUpdates,
      allowAutomationApi,
      allowCommunityView,
      allowUserManagement,
      apiKey,
      None,
      d
    )
  }

}