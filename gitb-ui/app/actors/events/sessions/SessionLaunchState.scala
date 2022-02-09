package actors.events.sessions

import com.gitb.core.AnyContent
import com.gitb.tpl.TestCase
import models.TestSessionLaunchData

case class SessionLaunchState (
  data: Option[TestSessionLaunchData],
  startedTestSessions: Set[String],
  completedTestSessions: Set[String],
  testCaseDefinitionCache: Map[Long, TestCase]
) {

  def newForStartedTestSession(startedTestCaseId: Long, assignedSessionId: String): SessionLaunchState = {
    // Remove the test case ID and record the session ID
    if (data.isEmpty) {
      SessionLaunchState(None, startedTestSessions + assignedSessionId, completedTestSessions, testCaseDefinitionCache)
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
      SessionLaunchState(Some(newData), startedTestSessions + assignedSessionId, completedTestSessions, definitionCacheToUse)
    }
  }

  def newWithLoadedTestCaseDefinition(testCaseId: Long, definition: TestCase): SessionLaunchState = {
    SessionLaunchState(data, startedTestSessions, completedTestSessions, testCaseDefinitionCache + (testCaseId -> definition))
  }

  def newWithCompletedTestSession(sessionId: String): SessionLaunchState = {
    SessionLaunchState(data, startedTestSessions, completedTestSessions + sessionId, testCaseDefinitionCache)
  }

  def hasActiveTestSessions(): Boolean = {
    completedTestSessions.size != startedTestSessions.size || !completedTestSessions.subsetOf(startedTestSessions)
  }

}
