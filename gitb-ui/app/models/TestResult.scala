package models

import java.sql.Timestamp

/**
 * Show the result of conformance test for a specific test case for a vendor
 */
case class TestResult(
                       sessionId: String,
                       systemId:Option[Long],
                       system:Option[String],
                       organizationId:Option[Long],
                       organization:Option[String],
                       communityId:Option[Long],
                       community:Option[String],
                       testCaseId:Option[Long],
                       testCase:Option[String],
                       testSuiteId:Option[Long],
                       testSuite:Option[String],
                       actorId:Option[Long],
                       actor:Option[String],
                       specificationId:Option[Long],
                       specification:Option[String],
                       domainId:Option[Long],
                       domain:Option[String],
                       result:String,
                       startTime:Timestamp,
                       endTime:Option[Timestamp],
                       tpl:String
                     )
{
  def withPresentation(presentation:String) = {
    TestResult(this.sessionId, this.systemId, this.system, this.organizationId, this.organization,
      this.communityId, this.community, this.testCaseId, this.testCase,  this.testSuiteId, this.testSuite,
      this.actorId, this.actor, this.specificationId, this.specification, this.domainId, this.domain,
      this.result, this.startTime, this.endTime, presentation)
  }
}

case class TestResultReport(testResult:TestResult, testCase: Option[TestCases], actor: Option[Actors])

case class TestResultSessionReport(testResult:TestResult, testCase: Option[TestCases], organization: Option[Organizations], system: Option[Systems], spec: Option[Specifications], domain: Option[Domain], testSuite: Option[TestSuites])