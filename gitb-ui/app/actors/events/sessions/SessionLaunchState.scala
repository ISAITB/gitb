package actors.events.sessions

import com.gitb.core.AnyContent
import com.gitb.tpl.TestCase
import models.{SessionConfigurationData, TestSessionLaunchData, TypedActorConfiguration}

import scala.collection.mutable

class SessionLaunchState {

  private var data = Option.empty[TestSessionLaunchData]
  private val pendingTestCases = mutable.LinkedHashSet[Long]()
  private val loadingDefinitionTestCases = mutable.Set[Long]()
  private val inProgressTestCases = mutable.Set[Long]()
  private val completedTestCases = mutable.Set[Long]()
  private val configuredTestSessions = mutable.Set[String]()
  private val inProgressTestSessions = mutable.Set[String]()
  private val startedTestSessions = mutable.Set[String]()
  private val completedTestSessions = mutable.Set[String]()
  private val idToSessionMap = mutable.Map[Long, String]()
  private val sessionToIdMap = mutable.Map[String, Long]()
  private val testCaseDefinitionCache = mutable.Map[Long, TestCase]()

  def setLaunchData(data: TestSessionLaunchData): SessionLaunchState = {
    this.data = Some(data)
    this.pendingTestCases.addAll(data.testCases)
    this
  }

  def setStartedTestSession(startedSessionId: String): SessionLaunchState = {
    // Record the test session as started and add the mapping between test case ID and test session ID
    this.startedTestSessions += startedSessionId
    this
  }

  def setInProgressTestSession(testCaseId: Long): SessionLaunchState = {
    this.loadingDefinitionTestCases -= testCaseId
    this.inProgressTestCases += testCaseId
    this
  }

  def setLoadingDefinitionForTestCase(testCaseId: Long): SessionLaunchState = {
    this.pendingTestCases -= testCaseId
    this.loadingDefinitionTestCases += testCaseId
    this
  }

  def setConfiguredTestSession(configuredTestCaseId: Long, assignedSessionId: String): SessionLaunchState = {
    // Record the test session as configured and add the mapping between test case ID and test session ID
    this.inProgressTestSessions += assignedSessionId
    this.configuredTestSessions += assignedSessionId
    this.sessionToIdMap += (assignedSessionId -> configuredTestCaseId)
    this.idToSessionMap += (configuredTestCaseId -> assignedSessionId)
    this
  }

  def setFailedTestCase(testCaseId: Long): SessionLaunchState = {
    this.loadingDefinitionTestCases -= testCaseId
    this.inProgressTestCases -= testCaseId
    this.completedTestCases += testCaseId
    idToSessionMap.get(testCaseId).foreach { testSessionId =>
      this.inProgressTestSessions -= testSessionId
    }
    releaseTestCaseState(testCaseId)
    this
  }

  def setFailedTestSession(testSessionId: String): SessionLaunchState = {
    this.inProgressTestSessions -= testSessionId
    sessionToIdMap.get(testSessionId).foreach { testCaseId =>
      this.loadingDefinitionTestCases -= testCaseId
      this.inProgressTestCases -= testCaseId
      this.completedTestCases += testCaseId
      releaseTestCaseState(testCaseId)
    }
    this
  }

  def setCompletedTestSession(testSessionId: String): SessionLaunchState = {
    this.completedTestSessions += testSessionId
    this.inProgressTestSessions -= testSessionId
    sessionToIdMap.get(testSessionId).foreach { testCaseId =>
      this.inProgressTestCases -= testCaseId
      this.completedTestCases += testCaseId
    }
    this
  }

  private def releaseTestCaseState(testCaseId: Long): Unit = {
    testCaseDefinitionCache.remove(testCaseId)
  }

  def removeTestSessions(testSessions: Set[String]): Int = {
    var counter = 0
    testSessions.foreach { testSessionId =>
      if (sessionToIdMap.contains(testSessionId)) {
        counter += 1
        this.inProgressTestSessions -= testSessionId
        sessionToIdMap.get(testSessionId).foreach { testCaseId =>
          this.loadingDefinitionTestCases -= testCaseId
          this.inProgressTestCases -= testCaseId
          this.pendingTestCases -= testCaseId
          this.completedTestCases += testCaseId
          releaseTestCaseState(testCaseId)
        }
      }
    }
    counter
  }

  def setTestCaseDefinition(testCaseId: Long, definition: TestCase): SessionLaunchState = {
    testCaseDefinitionCache += (testCaseId -> definition)
    this
  }

  private def isSequentialExecution(): Boolean = {
    data.exists(_.forceSequentialExecution)
  }

  def nextTestCaseId(): Long = {
    pendingTestCases.head
  }

  def getSessionConfigurationData(onlySimple: Boolean): SessionConfigurationData = {
    if (onlySimple) {
      // Include only the configuration values that are simple texts
      SessionConfigurationData(
        Some(data.get.statementParameters.map { x =>
          TypedActorConfiguration(x.actor, x.endpoint, x.config.filter(_.kind == "SIMPLE"))
        }),
        data.get.domainParameters.map { x =>
          TypedActorConfiguration(x.actor, x.endpoint, x.config.filter(_.kind == "SIMPLE"))
        },
        Some(TypedActorConfiguration(data.get.organisationParameters.actor, data.get.organisationParameters.endpoint, data.get.organisationParameters.config.filter(_.kind == "SIMPLE"))),
        Some(TypedActorConfiguration(data.get.systemParameters.actor, data.get.systemParameters.endpoint, data.get.systemParameters.config.filter(_.kind == "SIMPLE")))
      )
    } else {
      // Include all configuration values
      SessionConfigurationData(
        Some(data.get.statementParameters),
        data.get.domainParameters,
        Some(data.get.organisationParameters),
        Some(data.get.systemParameters)
      )
    }
  }

  def sessionForTestCaseId(testCaseId: Long): Option[String] = {
    idToSessionMap.get(testCaseId)
  }

  def testCaseIdForSession(testSessionId: String): Option[Long] = {
    sessionToIdMap.get(testSessionId)
  }

  def testCaseInputs(testCaseId: Long): Option[List[AnyContent]] = {
    data.flatMap(_.testCaseToInputMap.flatMap(_.get(testCaseId)))
  }

  def testCaseAllowedToExecute(newTestCaseId: Long): Boolean = {
    if (isSequentialExecution()) {
      !loadingDefinitionTestCases.exists(id => id != newTestCaseId) && !inProgressTestCases.exists(id => id != newTestCaseId)
    } else {
      val otherInProgressTestCases = inProgressTestCases.filter(id => id != newTestCaseId)
      val otherIncompleteTestCasesRequireSequentialExecution = otherInProgressTestCases.exists(id => {
        testCaseDefinitionCache.get(id).exists(!_.isSupportsParallelExecution)
      })
      val newTestCaseDefinition = testCaseDefinitionCache.get(newTestCaseId)
      !otherIncompleteTestCasesRequireSequentialExecution && (otherInProgressTestCases.isEmpty || newTestCaseDefinition.isEmpty || newTestCaseDefinition.get.isSupportsParallelExecution)
    }
  }

  def assignPredefinedSessionId(testCaseId: Long): Option[String] = {
    data.flatMap(_.sessionIdsToAssign.flatMap(_.get(testCaseId)))
  }

  def isLoadingDefinition(testCaseId: Long): Boolean = {
    loadingDefinitionTestCases.contains(testCaseId)
  }

  def isInProgress(testCaseId: Long): Boolean = {
    inProgressTestCases.contains(testCaseId)
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
    data.isDefined && startedTestSessions.size == data.get.testCases.size
  }

  def hasSessionsToProcess(): Boolean = {
    pendingTestCases.nonEmpty
  }

  def hasData(): Boolean = {
    data.nonEmpty
  }

  private def pendingCount(): Int = {
    pendingTestCases.size
  }

  private def inProgressCount(): Int = {
    inProgressTestCases.size
  }

  private def startedSessionCount(): Int = {
    startedTestSessions.size
  }

  private def configuredCount(): Int = {
    configuredTestSessions.size
  }

  private def completedCount(): Int = {
    completedTestSessions.size
  }

  private def finishedCount(): Int = {
    completedTestCases.size
  }

  def testCaseDefinition(testCaseId: Long): Option[TestCase] = {
    testCaseDefinitionCache.get(testCaseId)
  }

  def statusText(): String = {
    "Status: pending %s, in progress %s, configured %s, started %s, completed %s, finished %s".formatted(
      pendingCount(),
      inProgressCount(),
      configuredCount(),
      startedSessionCount(),
      completedCount(),
      finishedCount()
    )
  }

}
