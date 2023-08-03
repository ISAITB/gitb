package models

class TestCaseDeploymentAction(
  var identifier: String,
  var updateDefinition: Option[Boolean],
  var resetTestHistory: Option[Boolean]
) {
}
