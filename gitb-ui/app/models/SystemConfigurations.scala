package models

case class SystemConfigurations(name: String, parameter: Option[String], description: Option[String]) {
}

case class SystemConfigurationsWithEnvironment(config: SystemConfigurations, defaultSetting: Boolean, environmentSetting: Boolean) {
}
