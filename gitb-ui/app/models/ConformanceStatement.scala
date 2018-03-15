package models

import java.util

/**
 * Created by serbay on 11/3/14.
 */
case class TestResults(completed: Int, total: Int)
case class ConformanceStatement(domainId: Long, domainName: String, domainNameFull: String, actorId: Long, actorName: String, actorFull: String, specificationId: Long, specificationName: String, specificationNameFull: String, completedTests: Long, totalTests: Long)
case class ConformanceStatementSet(domainId: Long, domainName: String, domainNameFull: String, actorId: Long, actorName: String, actorFull: String, specificationId: Long, specificationName: String, specificationNameFull: String, testCaseIds: util.HashSet[Long])
