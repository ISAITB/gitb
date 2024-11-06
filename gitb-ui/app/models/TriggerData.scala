package models

case class TriggerData(dataType: Short, dataId: Long, trigger: Long) {

  def withTrigger(trigger: Long): TriggerData = {
    TriggerData(this.dataType, this.dataId, trigger)
  }

}