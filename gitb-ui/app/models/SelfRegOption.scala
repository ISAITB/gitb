package models

class SelfRegOption(_communityId: Long, _communityName: String, _selfRegType: Short, _templates: Option[List[SelfRegTemplate]]) {

  var communityId: Long = _communityId
  var communityName: String = _communityName
  var selfRegType: Short = _selfRegType
  var templates: Option[List[SelfRegTemplate]] = _templates

}
