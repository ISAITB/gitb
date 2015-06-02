package models

/**
 * Show the result of conformance test for a specific test case for a vendor
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
                       startTime:String,
                       endTime:Option[String],
                       sutVersion:Option[String],
                       tpl:String
                      )
{
  def withPresentation(presentation:String) = {
    TestResult(this.sessionId, this.systemId, this.actorId, this.testCaseId, this.result, this.startTime, this.endTime, this.sutVersion, presentation)
  }
}

case class TestResultReport(testResult:TestResult, testCase: Option[TestCases], actor: Option[Actors])