package models

import models.statement.ResultHolder

class ConformanceStatementResultData(
                                      var result: String,
                                      var completedTests: Long,
                                      var failedTests: Long,
                                      var undefinedTests: Long,
                                      var completedOptionalTests: Long,
                                      var failedOptionalTests: Long,
                                      var undefinedOptionalTests: Long,
                                      var completedTestsToConsider: Long,
                                      var failedTestsToConsider: Long,
                                      var undefinedTestsToConsider: Long,
                                    ) extends ResultHolder {

  def this() = {
    this("UNDEFINED", 0, 0, 0, 0, 0, 0, 0, 0, 0)
  }

  override def resultStatus(): String = {
    result
  }

  def copy(): ConformanceStatementResultData = {
    new ConformanceStatementResultData(
      result,
      completedTests, failedTests, undefinedTests,
      completedOptionalTests, failedOptionalTests, undefinedOptionalTests,
      completedTestsToConsider, failedTestsToConsider, undefinedTestsToConsider
    )
  }

}
