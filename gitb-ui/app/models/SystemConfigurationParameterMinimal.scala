package models

import models.prerequisites.WithPrerequisite

class SystemConfigurationParameterMinimal(var parameterId: Long, var endpointName: String, var parameterName: String, var parameterValue: Option[String], var dependsOn: Option[String], var dependsOnValue: Option[String], var parameterKind: String, var parameterUse: String, var valueContentType: Option[String], var notForTests: Boolean) extends WithPrerequisite {

  override def prerequisiteKey(): Option[String] = dependsOn
  override def prerequisiteValue(): Option[String] = dependsOnValue
  override def currentKey(): String = parameterName
  override def currentValue(): Option[String] = if (parameterValue.isDefined) Some(parameterValue.get) else None
}
