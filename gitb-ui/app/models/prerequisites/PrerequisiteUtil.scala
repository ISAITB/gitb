/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
