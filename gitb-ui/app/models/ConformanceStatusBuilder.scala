package models

import com.gitb.tr.TestResultType
import models.ConformanceStatusBuilder.{ConformanceData, FilterCriteria}
import models.Enums.TestResultStatus
import utils.TimeUtil

import java.util
import java.util.Date
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.MapHasAsScala

object ConformanceStatusBuilder {

  class FilterCriteria[A <: ConformanceStatement](updateTimeStart: Option[Date], updateTimeEnd: Option[Date], status: Option[List[String]]) {

    def check(overview: A): Boolean = {
      // Check overall status.
      if (status.isDefined) {
        if (!status.get.contains(statusFromResultCounts(overview.completedTestsToConsider, overview.failedTestsToConsider, overview.undefinedTestsToConsider))) {
          return false
        }
      }
      // Check update time.
      if (updateTimeStart.isDefined || updateTimeEnd.isDefined) {
        if (!(overview.updateTime.isDefined &&
          (updateTimeStart.isEmpty || overview.updateTime.get.compareTo(updateTimeStart.get) >= 0) &&
          (updateTimeEnd.isEmpty || overview.updateTime.get.compareTo(updateTimeEnd.get) <= 0))) {
          return false
        }
      }
      true
    }

    private def statusFromResultCounts(completed: Long, failed: Long, undefined: Long): String = {
      if (failed > 0) {
        TestResultStatus.FAILURE.toString
      } else if (undefined > 0) {
        TestResultStatus.UNDEFINED.toString
      } else if (completed > 0) {
        TestResultStatus.SUCCESS.toString
      } else {
        TestResultStatus.UNDEFINED.toString
      }
    }

  }

  class ResultHolder {
    var completed = 0
    var failed = 0
    var other = 0
  }

  class ConformanceData[A <: ConformanceStatementResultData](var overallResult: A) {
    private val groupData = new mutable.HashMap[Long, ResultHolder]()
    private var groupDataApplied = false

    def addConformanceResult(result: String, isOptional: Boolean, isDisabled: Boolean, testCaseGroupId: Option[Long]): Unit = {
      // Record statistics.
      if (!isDisabled) {
        if (isOptional) {
          if (TestResultStatus.withName(result) == TestResultStatus.SUCCESS) {
            overallResult.completedOptionalTests += 1
          } else if (TestResultStatus.withName(result) == TestResultStatus.FAILURE) {
            overallResult.failedOptionalTests += 1
          } else {
            overallResult.undefinedOptionalTests += 1
          }
        } else {
          if (TestResultStatus.withName(result) == TestResultStatus.SUCCESS) {
            overallResult.completedTests += 1
            if (testCaseGroupId.isEmpty) {
              overallResult.completedTestsToConsider += 1
            } else {
              groupResult(testCaseGroupId.get).completed += 1
            }
          } else if (TestResultStatus.withName(result) == TestResultStatus.FAILURE) {
            overallResult.failedTests += 1
            if (testCaseGroupId.isEmpty) {
              overallResult.failedTestsToConsider += 1
            } else {
              groupResult(testCaseGroupId.get).failed += 1
            }
          } else {
            overallResult.undefinedTests += 1
            if (testCaseGroupId.isEmpty) {
              overallResult.undefinedTestsToConsider += 1
            } else {
              groupResult(testCaseGroupId.get).other += 1
            }
          }
        }
      }
    }

    private def groupResult(groupId: Long): ResultHolder  = {
      if (groupData.contains(groupId)) {
        groupData(groupId)
      } else {
        val counter = new ResultHolder()
        groupData += (groupId -> counter)
        counter
      }
    }

    def complete(): ConformanceData[A] = {
      if (!groupDataApplied) {
        if (groupData.nonEmpty) {
          groupData.values.foreach { groupResult =>
            if (groupResult.completed > 0) {
              overallResult.completedTestsToConsider += 1
            } else if (groupResult.failed > 0) {
              overallResult.failedTestsToConsider += 1
            } else {
              overallResult.undefinedTestsToConsider += 1
            }
          }
        }
        overallResult.result = if (overallResult.failedTestsToConsider > 0) {
          TestResultType.FAILURE.toString
        } else if (overallResult.undefinedTestsToConsider > 0) {
          TestResultType.UNDEFINED.toString
        } else if (overallResult.completedTestsToConsider > 0) {
          TestResultType.SUCCESS.toString
        } else {
          TestResultType.UNDEFINED.toString
        }
        groupDataApplied = true
      }
      this
    }
  }

}

class ConformanceStatusBuilder[A <: ConformanceStatement](recordDetails: Boolean) {

  // SystemID|ActorID -> (Aggregated conformance status, List of conformance results)
  private val conformanceMap = new util.LinkedHashMap[String, (ConformanceData[A], ListBuffer[A])]

  def addConformanceResult(result: A, isOptional: Boolean, isDisabled: Boolean, testCaseGroupId: Option[Long]): Unit = {
    val key = s"${result.systemId}|${result.actorId}"
    var data = conformanceMap.get(key)
    if (data == null) {
      data = (new ConformanceData[A](result.copy().asInstanceOf[A]), new ListBuffer[A])
      conformanceMap.put(key, data)
    }
    // Record statistics.
    data._1.addConformanceResult(result.result, isOptional, isDisabled, testCaseGroupId)
    // Set overall last update time.
    if (result.updateTime.isDefined && (data._1.overallResult.updateTime.isEmpty || data._1.overallResult.updateTime.get.before(result.updateTime.get))) {
      data._1.overallResult.updateTime = TimeUtil.copyTimestamp(result.updateTime)
    }
    // Record individual result.
    if (recordDetails) {
      data._2 += result
    }
  }

  def getOverview(filter: Option[FilterCriteria[A]]): List[A] = {
    val statements = new ListBuffer[A]
    for (conformanceEntry <- conformanceMap.asScala) {
      conformanceEntry._2._1.complete()
      if (filter.isEmpty || filter.get.check(conformanceEntry._2._1.overallResult)) {
        statements += conformanceEntry._2._1.overallResult
      }
    }
    statements.toList
  }

  def getDetails(filter: Option[FilterCriteria[A]]): List[A] = {
    val statements = new ListBuffer[A]
    for (conformanceEntry <- conformanceMap.asScala) {
      conformanceEntry._2._1.complete()
      if (filter.isEmpty || filter.get.check(conformanceEntry._2._1.overallResult)) {
        statements ++= conformanceEntry._2._2
      }
    }
    statements.toList
  }

}