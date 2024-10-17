package actors.events.sessions

import com.gitb.core.AnyContent
import com.gitb.tpl.TestCase
import models.TestSessionLaunchData

object SessionLaunchState {

  def newState(): SessionLaunchState = {
    SessionLaunchState(None, Set.empty, Set.empty, Set.empty, Map.empty, Map.empty)
  }

}

case class SessionLaunchState (
  data: Option[TestSessionLaunchData],
  configuredTestSessions: Set[String],
  startedTestSessions: Set[String],
  completedTestSessions: Set[String],
  sessionIdMap: Map[String, Long],
  testCaseDefinitionCache: Map[Long, TestCase]
) {

  def newForLaunchData(data: TestSessionLaunchData): SessionLaunchState = {
    SessionLaunchState(Some(data), configuredTestSessions, startedTestSessions, completedTestSessions, sessionIdMap, testCaseDefinitionCache)
  }

  def newForStartedTestSession(startedSessionId: String): SessionLaunchState = {
    // Record the test session as started and add the mapping between test case ID and test session ID
    SessionLaunchState(data, configuredTestSessions, startedTestSessions + startedSessionId, completedTestSessions, sessionIdMap, testCaseDefinitionCache)
  }

  def newForConfiguredTestSession(configuredTestCaseId: Long, assignedSessionId: String): SessionLaunchState = {
    // Record the test session as configured and add the mapping between test case ID and test session ID
    if (data.isEmpty) {
      SessionLaunchState(None, configuredTestSessions + assignedSessionId, startedTestSessions, completedTestSessions, sessionIdMap + (assignedSessionId -> configuredTestCaseId), testCaseDefinitionCache)
    } else {
      SessionLaunchState(dataForRemovedTestCaseId(configuredTestCaseId), configuredTestSessions + assignedSessionId, startedTestSessions, completedTestSessions, sessionIdMap + (assignedSessionId -> configuredTestCaseId), testCaseDefinitionCache)
    }
  }

  private def dataForRemovedTestCaseId(testCaseId: Long): Option[TestSessionLaunchData] = {
    val otherTestCaseIds = data.get.testCases.filter(_ != testCaseId)
    // Input map
    var inputMapToUse: Option[Map[Long, List[AnyContent]]] = None
    if (data.get.testCaseToInputMap.nonEmpty) {
      inputMapToUse = Some(data.get.testCaseToInputMap.get.removed(testCaseId))
    }
    // New data
    Some(TestSessionLaunchData(
      data.get.communityId, data.get.organisationId, data.get.systemId, data.get.actorId,
      otherTestCaseIds, data.get.statementParameters, data.get.domainParameters, data.get.organisationParameters,
      data.get.systemParameters, inputMapToUse, data.get.sessionIdsToAssign, data.get.forceSequentialExecution
    ))
  }

  def newForFailedTestCase(testCaseId: Long): SessionLaunchState = {
    SessionLaunchState(dataForRemovedTestCaseId(testCaseId), configuredTestSessions, startedTestSessions, completedTestSessions, sessionIdMap, testCaseDefinitionCache)
  }

  def newForCompletedTestSession(sessionId: String): SessionLaunchState = {
    SessionLaunchState(data, configuredTestSessions, startedTestSessions, completedTestSessions + sessionId, sessionIdMap, testCaseDefinitionCache)
  }

  def newWithoutTestSessions(testSessions: Set[String]): SessionLaunchState = {
    if (data.exists(_.sessionIdsToAssign.isDefined)) {
      val testCaseIdsToRemove = data.get.sessionIdsToAssign.get.filter(x => testSessions.contains(x._2)).keys.toSet
      if (testCaseIdsToRemove.nonEmpty) {
        SessionLaunchState(
          data.map(_.newWithoutTestCaseIds(testCaseIdsToRemove)),
          configuredTestSessions,
          startedTestSessions,
          completedTestSessions,
          sessionIdMap,
          testCaseDefinitionCache.removedAll(testCaseIdsToRemove)
        )
      } else {
        this
      }
    } else {
      this
    }
  }

  def newForLoadedTestCaseDefinition(testCaseId: Long, definition: TestCase): SessionLaunchState = {
    SessionLaunchState(data, configuredTestSessions, startedTestSessions, completedTestSessions, sessionIdMap, testCaseDefinitionCache + (testCaseId -> definition))
  }

  private def isSequentialExecution(): Boolean = {
    data.exists(_.forceSequentialExecution)
  }

  def nextTestCaseId(): Long = {
    data.get.testCases.head
  }

  def testCaseInputs(testCaseId: Long): Option[List[AnyContent]] = {
    data.flatMap(_.testCaseToInputMap.flatMap(_.get(testCaseId)))
  }

  def testCaseAllowedToExecute(newTestCaseId: Long): Boolean = {
    val incompleteSessions = configuredTestSessions.union(startedTestSessions).removedAll(completedTestSessions)
    if (isSequentialExecution()) {
      incompleteSessions.isEmpty
    } else {
      val otherIncompleteTestCaseIds = sessionIdMap.filter(x => {
        incompleteSessions.contains(x._1)
      }).values
      val otherIncompleteTestCasesRequireSequentialExecution = otherIncompleteTestCaseIds.exists(id => {
        testCaseDefinitionCache.get(id).exists(!_.isSupportsParallelExecution)
      })
      !otherIncompleteTestCasesRequireSequentialExecution && (incompleteSessions.isEmpty || testCaseDefinitionCache.get(newTestCaseId).exists(_.isSupportsParallelExecution))
    }
  }

  def assignPredefinedSessionId(testCaseId: Long): Option[String] = {
    data.flatMap(_.sessionIdsToAssign.flatMap(_.get(testCaseId)))
  }

  def isConfiguredSession(sessionId: String): Boolean = {
    configuredTestSessions.contains(sessionId)
  }

  def isStartedSession(sessionId: String): Boolean = {
    startedTestSessions.contains(sessionId)
  }

  def matchesOrganisation(organisationId: Long): Boolean = {
    data.exists(_.organisationId == organisationId)
  }

  def organisation(): Long = {
    data.map(_.organisationId).getOrElse(0L)
  }

  def system(): Long = {
    data.get.systemId
  }

  def community(): Long = {
    data.get.communityId
  }

  def actor(): Long = {
    data.get.actorId
  }

  def allSessionsStarted(): Boolean = {
    // The completed sessions include those that encountered errors
    testSessionCount() == 0 && configuredTestSessions.removedAll(startedTestSessions).removedAll(completedTestSessions).isEmpty
  }

  def hasSessionsToProcess(): Boolean = {
    data.get.testCases.nonEmpty
  }

  def hasData(): Boolean = {
    data.nonEmpty
  }

  def testSessionCount(): Int = {
    data.map(_.testCases.size).getOrElse(0)
  }

  private def startedSessionCount(): Int = {
    startedTestSessions.size
  }

  private def configuredSessionCount(): Int = {
    configuredTestSessions.size
  }

  private def completedSessionCount(): Int = {
    completedTestSessions.size
  }

  def testCaseDefinition(testCaseId: Long): Option[TestCase] = {
    testCaseDefinitionCache.get(testCaseId)
  }

  def statusText(): String = {
    "Status: pending %s, configured %s, started %s, completed %s".formatted(testSessionCount(), configuredSessionCount(), startedSessionCount(), completedSessionCount())
  }

}
