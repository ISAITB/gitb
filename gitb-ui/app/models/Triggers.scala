package models

case class Triggers(id: Long, name: String, description: Option[String], url: String, eventType: Short, serviceType: Short, operation: Option[String], active: Boolean, latestResultOk: Option[Boolean], latestResultOutput: Option[String], community: Long)

class Trigger(var trigger: Triggers, var data: Option[List[TriggerData]], var fireExpressions: Option[List[TriggerFireExpression]])