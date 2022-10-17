package models

/**
 * Created by serbay.
 */
case class Parameters(id: Long, name: String, testKey: String, desc: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests:Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], endpoint: Long) {
	def withEndpoint(endpointId:Long, newDisplayOrder: Option[Short]): Parameters = {
		Parameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, newDisplayOrder.getOrElse(displayOrder), dependsOn, dependsOnValue, defaultValue, endpointId)
	}
}
