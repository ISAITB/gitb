package models

import models.prerequisites.WithPrerequisite

case class SystemParameters(id: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, inExports: Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], community: Long)

class SystemParametersWithValue(_parameter: SystemParameters, _value: Option[SystemParameterValues]) extends WithPrerequisite {
  var parameter: SystemParameters = _parameter
  var value: Option[SystemParameterValues] = _value

  override def prerequisiteKey(): Option[String] = parameter.dependsOn
  override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
  override def currentKey(): String = parameter.testKey
  override def currentValue(): Option[String] = if (value.isDefined) Some(value.get.value) else None
}