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

class SelfRegOption(_communityId: Long, _communityName: String, _communityDescription: Option[String], _selfRegTokenHelpText: Option[String], _selfRegType: Short, _templates: Option[List[SelfRegTemplate]], _labels: List[CommunityLabels], _customOrganisationProperties: List[OrganisationParameters], _forceTemplateSelection: Boolean, _forceRequiredProperties: Boolean) {

  var communityId: Long = _communityId
  var communityName: String = _communityName
  var communityDescription: Option[String] = _communityDescription
  var selfRegTokenHelpText: Option[String] = _selfRegTokenHelpText
  var selfRegType: Short = _selfRegType
  var templates: Option[List[SelfRegTemplate]] = _templates
  var labels: List[CommunityLabels] = _labels
  var customOrganisationProperties: List[OrganisationParameters] = _customOrganisationProperties
  var forceTemplateSelection: Boolean = _forceTemplateSelection
  var forceRequiredProperties: Boolean = _forceRequiredProperties

}
