package models.statement

import com.gitb.tr.TestResultType

trait ResultCountHolder extends ResultHolder {

  def completedCount(): Long
  def failedCount(): Long
  def otherCount(): Long

  override def resultStatus(): String = {
    if (failedCount() > 0) {
      TestResultType.FAILURE.toString
    } else if (otherCount() > 0) {
      TestResultType.UNDEFINED.toString
    } else if (completedCount() > 0) {
      TestResultType.SUCCESS.toString
    } else {
      TestResultType.UNDEFINED.toString
    }

  }

}
