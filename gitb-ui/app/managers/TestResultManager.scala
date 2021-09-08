package managers

import com.gitb.core.StepStatus
import models.Enums.TestResultStatus
import models._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestResultManager @Inject() (repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  private def logger = LoggerFactory.getLogger("TestResultManager")

  import dbConfig.profile.api._

  /**
   * Concurrent map used to hold the sessions' step status updates. Status updates are held in a linked hash map
   * for direct access but also retrieval using insertion order.
   */
  private val sessionMap = new TrieMap[String, (mutable.LinkedHashMap[String, TestStepResultInfo], mutable.Queue[String])]()

  private def getOrCreateSession(sessionId: String) = {
    var sessionData = sessionMap.get(sessionId)
    if (sessionData.isEmpty) {
      sessionData = Some(new mutable.LinkedHashMap[String, TestStepResultInfo], new mutable.Queue[String])
      sessionMap.put(sessionId, sessionData.get)
    }
    sessionData.get
  }

  def sessionUpdate(sessionId: String, logMessage: String): Unit = {
    val sessionLogMessages = getOrCreateSession(sessionId)._2
    sessionLogMessages += logMessage.trim
  }

  def consumeSessionLogs(sessionId: String) = {
    val sessionData = getOrCreateSession(sessionId)
    sessionData._2.dequeueAll(_ => true).toList
  }

  def sessionUpdate(sessionId: String, stepId: String, status: TestStepResultInfo): List[(String, TestStepResultInfo)] = {
    val sessionSteps = getOrCreateSession(sessionId)._1
    var stepInfo = sessionSteps.get(stepId)
    if (stepInfo.isEmpty) {
      stepInfo = Some(status)
      sessionSteps.put(stepId, stepInfo.get)
    } else {
      val existingStatus = stepInfo.get.result
      if (status.result == StepStatus.COMPLETED.ordinal().toShort
        || status.result == StepStatus.ERROR.ordinal().toShort
        || status.result == StepStatus.WARNING.ordinal().toShort
        || status.result == StepStatus.SKIPPED.ordinal().toShort && existingStatus != StepStatus.COMPLETED.ordinal().toShort && existingStatus != StepStatus.ERROR.ordinal().toShort && existingStatus != StepStatus.WARNING.ordinal().toShort
        || status.result == StepStatus.WAITING.ordinal().toShort && existingStatus != StepStatus.SKIPPED.ordinal().toShort && existingStatus != StepStatus.COMPLETED.ordinal().toShort && existingStatus != StepStatus.ERROR.ordinal().toShort && existingStatus != StepStatus.WARNING.ordinal().toShort
        || status.result == StepStatus.PROCESSING.ordinal().toShort && existingStatus != StepStatus.WAITING.ordinal().toShort && existingStatus != StepStatus.SKIPPED.ordinal().toShort && existingStatus != StepStatus.COMPLETED.ordinal().toShort && existingStatus != StepStatus.ERROR.ordinal().toShort && existingStatus != StepStatus.WARNING.ordinal().toShort
      ) {
        // The new status logically follows the current one (check made to avoid race conditions that would "rewind" progress)
        stepInfo.get.result = status.result
        stepInfo.get.path = status.path
      }
    }
    // Return the history of step updates
    sessionSteps.map { entry =>
      (entry._1, entry._2)
    }.toList
  }

  def sessionRemove(sessionId: String): List[(String, TestStepResultInfo)] = {
    val sessionSteps = sessionMap.remove(sessionId)
    if (sessionSteps.isEmpty) {
      List()
    } else {
      sessionSteps.get._1.map { entry =>
        (entry._1, entry._2)
      }.toList
    }
  }

  def getTestResultForSessionWrapper(sessionId: String): Option[TestResult] = {
    exec(getTestResultForSession(sessionId))
  }

  def getCommunityIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(r => (r.testSessionId, r.communityId)).result.headOption)
    result
  }

  def getOrganisationIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(r => (r.testSessionId, r.organizationId)).result.headOption)
    result
  }

  def getTestResultForSession(sessionId: String): DBIO[Option[TestResult]] = {
    PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
  }

  /**
   * Gets all running test results
   */
  def getRunningTestResults: List[TestResult] = {
    val results = exec(
      PersistenceSchema.testResults.filter(_.endTime.isEmpty).result.map(_.toList)
    )
    results
  }

  def updateForUpdatedSystem(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield t.sut
    q1.update(Some(name))
  }

  def updateForUpdatedOrganisation(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield t.organization
    q1.update(Some(name))
  }

  def updateForUpdatedCommunity(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield t.community
    q1.update(Some(name))
  }

  def updateForUpdatedTestCase(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield t.testCase
    q1.update(Some(name))
  }

  def updateForUpdatedTestSuite(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield t.testSuite
    q1.update(Some(name))
  }

  def updateForUpdatedDomain(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield t.domain
    q1.update(Some(name))
  }

  def updateForUpdatedSpecification(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield t.specification
    q1.update(Some(name))
  }

  def updateForUpdatedActor(id: Long, name: String): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield t.actor
    q1.update(Some(name))
  }

  def updateForDeletedSystem(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield t.sutId
    q1.update(None)
  }

  def updateForDeletedOrganisation(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield t.organizationId
    q1.update(None)
  }

  def updateForDeletedOrganisationByCommunityId(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield t.organizationId
    q1.update(None)
  }

  def updateForDeletedCommunity(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield t.communityId
    q1.update(None)
  }

  def updateForDeletedTestCase(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield t.testCaseId
    q1.update(None)
  }

  def updateForDeletedTestSuite(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield t.testSuiteId
    q1.update(None)
  }

  def updateForDeletedDomain(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield t.domainId
    q1.update(None)
  }

  def updateForDeletedSpecification(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield t.specificationId
    q1.update(None)
  }

  def updateForDeletedActor(id: Long): DBIO[_] = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield t.actorId
    q1.update(None)
  }

  def deleteObsoleteTestResultsForSystemWrapper(systemId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteObsoleteTestResultsForSystem(systemId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteObsoleteTestResultsForCommunityWrapper(communityId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteObsoleteTestResultsForCommunity(communityId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteSessionDataFromFileSystem(testResult: TestResult): Unit = {
    val pathInfo = repositoryUtils.getPathForTestSessionObj(testResult.sessionId, Some(testResult), isExpected = true)
    try {
        FileUtils.deleteDirectory(pathInfo.path.toFile)
    } catch {
      case e:Exception =>
        logger.warn("Unable to delete folder ["+pathInfo.path.toFile.getAbsolutePath+"] for session [" + testResult.sessionId + "]", e)
    }
  }

  private def deleteTestSession(testSession: TestResult, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val queryTestResult = PersistenceSchema.testResults.filter(_.testSessionId === testSession.sessionId)
    val queryConformanceResult = PersistenceSchema.conformanceResults.filter(_.testsession === testSession.sessionId)
    for {
      // Delete file system data (on success)
      _ <- {
        onSuccessCalls += (() => {
          deleteSessionDataFromFileSystem(testSession)
        })
        DBIO.successful(())
      }
      // Load conformance result
      conformanceResult <- queryConformanceResult.result.headOption
      // Calculate if needed new conformance result
      _ <- {
        if (conformanceResult.isDefined) {
          // Update the result entry to match the other sessions' results
          for {
            // Get latest relevant test session (except the one being deleted)
            latestTestSession <- PersistenceSchema.testResults
              .filter(_.actorId === conformanceResult.get.actor)
              .filter(_.sutId === conformanceResult.get.sut)
              .filter(_.testCaseId === conformanceResult.get.testcase)
              .filter(_.testSessionId =!= testSession.sessionId)
              .filter(_.endTime.isDefined)
              .sortBy(_.endTime.desc)
              .result
              .headOption
            // Update the result.
            _ <- {
              val updateQuery = for { c <- queryConformanceResult } yield (c.testsession, c.result, c.outputMessage)
              if (latestTestSession.isDefined) {
                // Replace status
                updateQuery.update(Some(latestTestSession.get.sessionId), latestTestSession.get.result, latestTestSession.get.outputMessage)
              } else {
                // Set as empty status
                updateQuery.update(None, TestResultStatus.UNDEFINED.toString, None)
              }
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
      // Delete test result
      _ <- queryTestResult.delete
      // Delete test step reports
      _ <- PersistenceSchema.testStepReports.filter(_.testSessionId ===testSession.sessionId).delete
    } yield ()
  }

  def deleteTestSessions(sessionIds: Iterable[String]): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      sessions <- PersistenceSchema.testResults.filter(_.testSessionId inSet sessionIds).result
      _ <- deleteTestSessionsInternal(sessions, onSuccessCalls)
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteTestSessionsInternal(sessions: Iterable[TestResult], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val actions = ListBuffer[DBIO[_]]()
    sessions.foreach { session =>
      actions += deleteTestSession(session, onSuccessCalls)
    }
    toDBIO(actions)
  }

  def deleteObsoleteTestResultsForSystem(systemId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for  {
      sessions <- PersistenceSchema.testResults
                      .filter(x => x.sutId === systemId &&
                          (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
                            x.communityId.isEmpty || x.organizationId.isEmpty ||
                            x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
                      .result
      _ <- deleteTestSessionsInternal(sessions, onSuccessCalls)
    } yield ()
  }

  def deleteObsoleteTestResultsForCommunity(communityId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      sessions <- PersistenceSchema.testResults
        .filter(x => x.communityId === communityId &&
          (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
            x.sutId.isEmpty || x.organizationId.isEmpty ||
            x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
        .result
      _ <- deleteTestSessionsInternal(sessions, onSuccessCalls)
    } yield ()
  }

  def deleteAllObsoleteTestResults(): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      sessions <- PersistenceSchema.testResults
        .filter(x =>
          x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
            x.sutId.isEmpty || x.organizationId.isEmpty || x.communityId.isEmpty ||
            x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
        .result
      _ <- deleteTestSessionsInternal(sessions, onSuccessCalls)
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def testSessionsExistForSystemAndActors(systemId: Long, actorIds: List[Long]): Boolean = {
    exec(
      PersistenceSchema.testResults
        .filter(_.sutId === systemId)
        .filter(_.actorId inSet actorIds)
        .map(x => x.testSessionId)
        .result
        .headOption
    ).isDefined
  }

  def testSessionsExistForSystem(systemId: Long): Boolean = {
    exec(
      PersistenceSchema.testResults
        .filter(_.sutId === systemId)
        .map(x => x.testSessionId)
        .result
        .headOption
    ).isDefined
  }

  def testSessionsExistForOrganisation(organisationId: Long): Boolean = {
    exec(
      PersistenceSchema.testResults
        .filter(_.organizationId === organisationId)
        .map(x => x.testSessionId)
        .result
        .headOption
    ).isDefined
  }

  def testSessionsExistForUserOrganisation(userId: Long): Boolean = {
    exec(
      PersistenceSchema.users
        .join(PersistenceSchema.testResults).on(_.organization === _.organizationId)
        .filter(_._1.id === userId)
        .map(x => x._2.testSessionId)
        .result
        .headOption
    ).isDefined
  }

}
