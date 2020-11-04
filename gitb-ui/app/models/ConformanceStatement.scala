package models

import java.util

/**
 * Created by serbay on 11/3/14.
 */
case class ConformanceStatement(domainId: Long, domainName: String, domainNameFull: String, actorId: Long, actorName: String, actorFull: String, specificationId: Long, specificationName: String, specificationNameFull: String, var completedTests: Long, var failedTests: Long, var undefinedTests: Long)
case class ConformanceStatementFull(communityId: Long, communityName: String, organizationId: Long, organizationName: String, systemId: Long, systemName: String, domainId: Long, domainName: String, domainNameFull: String, actorId: Long, actorName: String, actorFull: String, specificationId: Long, specificationName: String, specificationNameFull: String, testSuiteName: Option[String], testCaseName: Option[String], testCaseDescription: Option[String], result: Option[String], outputMessage: Option[String], sessionId: Option[String], var completedTests: Long, var failedTests: Long, var undefinedTests: Long)
case class ConformanceStatementSet(domainId: Long, domainName: String, domainNameFull: String, actorId: Long, actorName: String, actorFull: String, specificationId: Long, specificationName: String, specificationNameFull: String, testCaseIds: util.HashSet[Long])
