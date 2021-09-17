package models

import models.ConformanceStatusBuilder.FilterCriteria
import models.Enums.TestResultStatus

import java.util
import java.util.Date
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.MapHasAsScala

object ConformanceStatusBuilder {

  class FilterCriteria[A <: ConformanceStatement](updateTimeStart: Option[Date], updateTimeEnd: Option[Date], status: Option[List[String]]) {

    def check(overview: A): Boolean = {
      // Check overall status.
      if (status.isDefined) {
        if (!status.get.contains(statusFromResultCounts(overview.failedTests, overview.undefinedTests))) {
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

    private def statusFromResultCounts(failed: Long, undefined: Long): String = {
      if (failed > 0) {
        TestResultStatus.FAILURE.toString
      } else if (undefined > 0) {
        TestResultStatus.UNDEFINED.toString
      } else {
        TestResultStatus.SUCCESS.toString
      }
    }

  }

}

class ConformanceStatusBuilder[A <: ConformanceStatement](recordDetails: Boolean) {

  // SystemID|ActorID -> (Aggregated conformance status, List of conformance results)
  private val conformanceMap = new util.LinkedHashMap[String, (A, ListBuffer[A])]

  def addConformanceResult(result: A): Unit = {
    val key = s"${result.systemId}|${result.actorId}"
    var data = conformanceMap.get(key)
    if (data == null) {
      data = (result, new ListBuffer[A])
      conformanceMap.put(key, data)
    }
    // Record statistics.
    if (TestResultStatus.withName(result.result) == TestResultStatus.SUCCESS) {
      data._1.completedTests += 1
    } else if (TestResultStatus.withName(result.result) == TestResultStatus.FAILURE) {
      data._1.failedTests += 1
    } else {
      data._1.undefinedTests += 1
    }
    // Set overall last update time.
    if (result.updateTime.isDefined && (data._1.updateTime.isEmpty || data._1.updateTime.get.before(result.updateTime.get))) {
      data._1.updateTime = result.updateTime
    }
    // Record individual result.
    if (recordDetails) {
      data._2 += result
    }
  }

  def getOverview(filter: Option[FilterCriteria[A]]): List[A] = {
    val statements = new ListBuffer[A]
    for (conformanceEntry <- conformanceMap.asScala) {
      if (filter.isEmpty || filter.get.check(conformanceEntry._2._1)) {
        statements += conformanceEntry._2._1
      }
    }
    statements.toList
  }

  def getDetails(filter: Option[FilterCriteria[A]]): List[A] = {
    val statements = new ListBuffer[A]
    for (conformanceEntry <- conformanceMap.asScala) {
      if (filter.isEmpty || filter.get.check(conformanceEntry._2._1)) {
        statements ++= conformanceEntry._2._2
      }
    }
    statements.toList
  }

}