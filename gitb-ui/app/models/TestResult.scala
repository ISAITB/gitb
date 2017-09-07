package models

import java.sql.Timestamp

/**
 * Show the result of conformance test for a specific test case for a vendor
  *
  * @param sessionId
 * @param systemId
 * @param actorId
 * @param testCaseId
 * @param result
 * @param startTime
 * @param endTime
 * @param sutVersion
 * @param tpl
 */
case class TestResult(
                       sessionId: String,
                       systemId:Long,
                       actorId:Long,
                       testCaseId:Long,
                       result:String,
                       startTime:Timestamp,
                       endTime:Option[Timestamp],
                       sutVersion:Option[String],
                       tpl:String
                      )
{
  def withPresentation(presentation:String) = {
    TestResult(this.sessionId, this.systemId, this.actorId, this.testCaseId, this.result, this.startTime, this.endTime, this.sutVersion, presentation)
  }
}

case class TestResultReport(testResult:TestResult, testCase: Option[TestCases], actor: Option[Actors])

case class TestResultSessionReport(testResult:TestResult, testCase: Option[TestCases], organization: Option[Organizations], system: Option[Systems], spec: Option[Specifications], domain: Option[Domain], testSuite: Option[TestSuites])