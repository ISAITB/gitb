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
                       outputMessage: Option[String]
)