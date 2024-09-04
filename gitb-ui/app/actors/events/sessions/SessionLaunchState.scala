package actors.events.sessions

import com.gitb.core.AnyContent
import com.gitb.tpl.TestCase
import models.TestSessionLaunchData

case class SessionLaunchState (
  data: Option[TestSessionLaunchData],
  startedTestSessions: Map[String, Long],
  completedTestSessions: Set[String],
  testCaseDefinitionCache: Map[Long, TestCase]
) {

  def newForStartedTestSession(startedTestCaseId: Long, assignedSessionId: String): SessionLaunchState = {
    // Remove the test case ID and record the session ID
    if (data.isEmpty) {
      SessionLaunchState(None, startedTestSessions + (assignedSessionId -> startedTestCaseId), completedTestSessions, testCaseDefinitionCache)
    } else {
      // Test case IDs
      val idsToUse = data.get.testCases.filter(_ != startedTestCaseId)
      // Input map
      var inputMapToUse: Option[Map[Long, List[AnyContent]]] = None
      if (data.get.testCaseToInputMap.nonEmpty) {
        inputMapToUse = Some(data.get.testCaseToInputMap.get.removed(startedTestCaseId))
      }
      // Test definition cache
      val definitionCacheToUse = testCaseDefinitionCache.removed(startedTestCaseId)
      val newData = TestSessionLaunchData(
        data.get.communityId, data.get.organisationId, data.get.systemId, data.get.actorId,
        idsToUse, data.get.statementParameters, data.get.domainParameters, data.get.organisationParameters,
        data.get.systemParameters, inputMapToUse, data.get.sessionIdsToAssign, data.get.forceSequentialExecution
      )
      SessionLaunchState(Some(newData), startedTestSessions + (assignedSessionId -> startedTestCaseId), completedTestSessions, definitionCacheToUse)
    }
  }

  def newWithoutTestSessions(testSessions: Set[String]): SessionLaunchState = {
    if (data.exists(_.sessionIdsToAssign.isDefined)) {
      val testCaseIdsToRemove = data.get.sessionIdsToAssign.get.filter(x => testSessions.contains(x._2)).keys.toSet
      if (testCaseIdsToRemove.nonEmpty) {
        SessionLaunchState(
          data.map(_.newWithoutTestCaseIds(testCaseIdsToRemove)),
          startedTestSessions,
          completedTestSessions,
          testCaseDefinitionCache.removedAll(testCaseIdsToRemove)
        )
      } else {
        this
      }
    } else {
      this
    }
  }

  def newWithLoadedTestCaseDefinition(testCaseId: Long, definition: TestCase): SessionLaunchState = {
    SessionLaunchState(data, startedTestSessions, completedTestSessions, testCaseDefinitionCache + (testCaseId -> definition))
  }

  def newWithCompletedTestSession(sessionId: String): SessionLaunchState = {
    SessionLaunchState(data, startedTestSessions, completedTestSessions + sessionId, testCaseDefinitionCache)
  }

  def hasActiveTestSessions(): Boolean = {
    completedTestSessions.size != startedTestSessions.size || !completedTestSessions.subsetOf(startedTestSessions.keySet)
  }

  def testSessionCount(): Int = {
    data.map(_.testCases.size).getOrElse(0)
  }

}
