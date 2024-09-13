package models

import models.prerequisites.WithPrerequisite

/**
 * Created by serbay.
 */
case class Parameters(id: Long, name: String, testKey: String, desc: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests:Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], endpoint: Long) {

	def withEndpoint(endpointId:Long, newDisplayOrder: Option[Short]): Parameters = {
		Parameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, newDisplayOrder.getOrElse(displayOrder), dependsOn, dependsOnValue, defaultValue, endpointId)
	}

}

class ParametersWithValue(_parameter: Parameters, _value: Option[Configs]) extends WithPrerequisite {
	var parameter: Parameters = _parameter
	var value: Option[Configs] = _value

	override def prerequisiteKey(): Option[String] = parameter.dependsOn
	override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
	override def currentKey(): String = parameter.testKey
	override def currentValue(): Option[String] = if (value.isDefined) Some(value.get.value) else None
}

