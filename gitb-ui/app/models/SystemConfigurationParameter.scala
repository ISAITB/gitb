package models

import models.prerequisites.WithPrerequisite

class SystemConfigurationParameter(_parameter: Parameters, _configured: Boolean, _config: Option[Configs], _mimeType: Option[String], _extension: Option[String]) extends WithPrerequisite {
  var parameter: Parameters = _parameter
  var config: Option[Configs] = _config
  var configured: Boolean = _configured
  var mimeType: Option[String] = _mimeType
  var extension: Option[String] = _extension

  override def prerequisiteKey(): Option[String] = parameter.dependsOn
  override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
  override def currentKey(): String = parameter.name
  override def currentValue(): Option[String] = if (config.isDefined) Some(config.get.value) else None
}
