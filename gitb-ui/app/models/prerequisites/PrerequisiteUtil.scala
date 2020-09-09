package models.prerequisites

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object PrerequisiteUtil {

  def withValidPrerequisites[A <: WithPrerequisite](allValues: Iterable[A]): List[A] = {
    val checkMap = mutable.Map[String, Boolean]()
    val valueMap = mutable.Map[String, A]()
    allValues.foreach { value =>
      valueMap += (value.currentKey() -> value)
    }
    val valuesToReturn = ListBuffer[A]()
    allValues.foreach { value =>
      if (checkPrerequisites(checkMap, valueMap, value)) {
        valuesToReturn += value
      }
    }
    valuesToReturn.toList
  }

  private def checkPrerequisites[A <: WithPrerequisite](checkMap: mutable.Map[String, Boolean], valueMap: mutable.Map[String, A], value: A): Boolean = {
    var checkResult = checkMap.get(value.currentKey())
    if (checkResult.isEmpty) {
      if (value.prerequisiteKey().isDefined) {
        val prerequisite = valueMap.get(value.prerequisiteKey().get)
        if (prerequisite.isDefined) {
          val prerequisiteCheckOk = checkPrerequisites(checkMap, valueMap, prerequisite.get)
          if (prerequisiteCheckOk) {
            // Ok if the prerequisite's value matches the current one's expected value.
            checkResult = Some(value.prerequisiteValue().isDefined && prerequisite.get.currentValue().isDefined && value.prerequisiteValue().get.equals(prerequisite.get.currentValue().get))
          } else {
            // Prerequisite is lacking itself prerequisites.
            checkResult = Some(false)
          }
        } else {
          // Not normal. The prerequisite couldn't be found.
          checkResult = Some(false)
        }
      } else {
        checkResult = Some(true)
      }
      checkMap += (value.currentKey() -> checkResult.get)
    }
    checkResult.get
  }

}
