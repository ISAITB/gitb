package models

import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice

import scala.collection.mutable

class TestSuiteDeploymentAction(
  var specification: Long,
  var action: TestSuiteReplacementChoice,
  var updateTestSuite: Boolean,
  var updateActors: Boolean,
  var testCaseUpdates: Option[List[TestCaseDeploymentAction]]
) {

  private var testCaseMap: Option[Map[String, (Boolean, Boolean)]] = None

  private def getTestCaseMap(): Map[String, (Boolean, Boolean)] = {
    if (testCaseMap.isEmpty) {
      if (testCaseUpdates.isDefined) {
        val tempMap = new mutable.HashMap[String, (Boolean, Boolean)]()
        testCaseUpdates.get.foreach { entry =>
          tempMap += (entry.identifier -> (entry.updateDefinition, entry.resetTestHistory))
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
