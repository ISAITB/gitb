package models

case class TriggerFireExpression(id: Long, expression: String, expressionType: Short, notMatch: Boolean, trigger: Long) {

  def withTrigger(trigger: Long): TriggerFireExpression = {
    TriggerFireExpression(this.id, this.expression, this.expressionType, this.notMatch, trigger)
  }

}