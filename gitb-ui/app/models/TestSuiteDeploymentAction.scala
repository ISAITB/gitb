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

package models

import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice

import scala.collection.mutable

class TestSuiteDeploymentAction(
   var specification: Option[Long],
   val action: TestSuiteReplacementChoice,
   val updateTestSuite: Boolean,
   val updateActors: Option[Boolean],
   val sharedTestSuite: Boolean,
   val testCaseUpdates: Option[List[TestCaseDeploymentAction]]
) {

  private var testCaseMap: Option[Map[String, (Boolean, Boolean)]] = None

  private def getTestCaseMap(): Map[String, (Boolean, Boolean)] = {
    if (testCaseMap.isEmpty) {
      if (testCaseUpdates.isDefined) {
        val tempMap = new mutable.HashMap[String, (Boolean, Boolean)]()
        testCaseUpdates.get.foreach { entry =>
          tempMap += (entry.identifier -> (entry.updateDefinition.getOrElse(false), entry.resetTestHistory.getOrElse(false)))
        }
        testCaseMap = Some(tempMap.toMap)
      } else {
        testCaseMap = Some(Map.empty)
      }
    }
    testCaseMap.get
  }

  def updateTestCaseMetadata(identifier: String): Boolean = {
    testCaseUpdates.isDefined && getTestCaseMap().getOrElse(identifier, (false, false))._1
  }

  def resetTestCaseHistory(identifier: String): Boolean = {
    testCaseUpdates.isDefined && getTestCaseMap().getOrElse(identifier, (false, false))._2
  }

}
