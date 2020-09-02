package models

import models.prerequisites.WithPrerequisite

case class OrganisationParameters(id: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, inExports: Boolean, inSelfRegistration: Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], community: Long)

class OrganisationParametersWithValue(_parameter: OrganisationParameters, _value: Option[OrganisationParameterValues]) extends WithPrerequisite {
  var parameter: OrganisationParameters = _parameter
  var value: Option[OrganisationParameterValues] = _value

  override def prerequisiteKey(): Option[String] = parameter.dependsOn
  override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
  override def currentKey(): String = parameter.testKey
  override def currentValue(): Option[String] = if (value.isDefined) Some(value.get.value) else None
}
