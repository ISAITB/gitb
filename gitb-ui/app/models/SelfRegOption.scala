package models

class SelfRegOption(_communityId: Long, _communityName: String, _communityDescription: Option[String], _selfRegTokenHelpText: Option[String], _selfRegType: Short, _templates: Option[List[SelfRegTemplate]], _labels: List[CommunityLabels], _customOrganisationProperties: List[OrganisationParameters]) {

  var communityId: Long = _communityId
  var communityName: String = _communityName
  var communityDescription: Option[String] = _communityDescription
  var selfRegTokenHelpText: Option[String] = _selfRegTokenHelpText
  var selfRegType: Short = _selfRegType
  var templates: Option[List[SelfRegTemplate]] = _templates
  var labels: List[CommunityLabels] = _labels
  var customOrganisationProperties: List[OrganisationParameters] = _customOrganisationProperties

}
