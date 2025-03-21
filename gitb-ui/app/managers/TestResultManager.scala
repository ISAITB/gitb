package managers

import com.gitb.core.StepStatus
import com.gitb.tpl.TestCase
import com.gitb.utils.XMLUtils
import config.Configurations
import models.Enums.TestResultStatus
import models._
import org.apache.commons.io.FileUtils
import org.apache.pekko.actor.{ActorSystem, Cancellable}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{EmailUtil, JacksonUtil, RepositoryUtils, TimeUtil}

import java.io.{File, StringReader}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class TestResultManager @Inject() (actorSystem: ActorSystem,
                                   repositoryUtils: RepositoryUtils,
                                   communityHelper: CommunityHelper,
                                   dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  private def logger = LoggerFactory.getLogger("TestResultManager")
  private var interactionNotificationFuture: Option[Cancellable] = None

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

  private def consumeSessionLogs(sessionId: String) = {
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

  def getTestResultForSessionWrapper(sessionId: String): Option[(TestResult, String)] = {
    exec(getTestResultForSession(sessionId))
  }

  def getCommunityIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    exec(
      PersistenceSchema.testResults
        .filter(_.testSessionId === sessionId)
        .map(r => (r.testSessionId, r.communityId))
        .result
        .headOption
    )
  }

  def getOrganisationIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(r => (r.testSessionId, r.organizationId)).result.headOption)
    result
  }

  def getTestResultForSession(sessionId: String): DBIO[Option[(TestResult, String)]] = {
    for {
      testResult <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
      testDefinition <- if (testResult.isDefined) {
        PersistenceSchema.testResultDefinitions.filter(_.testSessionId === sessionId).result.headOption
      } else {
        DBIO.successful(None)
      }
      result <- if (testResult.isDefined) {
        DBIO.successful(Some((testResult.get, testDefinition.get.tpl)))
      } else {
        DBIO.successful(None)
      }
    } yield result
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

  def getAllRunningSessions(): List[String] = {
    val results = exec(
      PersistenceSchema.testResults
        .filter(_.endTime.isEmpty)
        .map(x => x.testSessionId)
        .result.map(_.toList)
    )
    results
  }

  def getRunningSessionsForCommunity(community: Long): List[String] = {
    val results = exec(
      PersistenceSchema.testResults
        .filter(_.communityId === community)
        .filter(_.endTime.isEmpty)
        .map(x => x.testSessionId)
        .result.map(_.toList)
    )
    results
  }

  def getRunningSessionsForOrganisation(organisation: Long): List[String] = {
    val results = exec(
      PersistenceSchema.testResults
        .filter(_.organizationId === organisation)
        .filter(_.endTime.isEmpty)
        .map(x => x.testSessionId)
        .result.map(_.toList)
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
    // No update is made to the test case's name as the test session history is decoupled.
    DBIO.successful(())
  }

  def updateForUpdatedTestSuite(id: Long, name: String): DBIO[_] = {
    // No update is made to the test suite's name as the test session history is decoupled.
    DBIO.successful(())
  }

  def updateForUpdatedDomain(id: Long, name: String): DBIO[_] = {
    // No update is made as the test session history is decoupled.
    DBIO.successful(())
  }

  def updateForUpdatedSpecification(id: Long, name: String): DBIO[_] = {
    // No update is made as the test session history is decoupled.
    DBIO.successful(())
  }

  def updateForUpdatedActor(id: Long, name: String): DBIO[_] = {
    // No update is made as the test session history is decoupled.
    DBIO.successful(())
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

  def deleteObsoleteTestResultsForOrganisationWrapper(organisationId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteObsoleteTestResultsForOrganisation(organisationId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteObsoleteTestResultsForCommunityWrapper(communityId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteObsoleteTestResultsForCommunity(communityId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteSessionDataFromFileSystem(testResult: TestResult): Unit = {
    val pathInfo = repositoryUtils.getPathForTestSessionObj(testResult.sessionId, Some(testResult.startTime), isExpected = true)
    try {
        FileUtils.deleteDirectory(pathInfo.path.toFile)
    } catch {
      case e:Exception =>
        logger.warn("Unable to delete folder ["+pathInfo.path.toFile.getAbsolutePath+"] for session [" + testResult.sessionId + "]", e)
    }
  }

  private def deleteTestSession(testSession: TestResult, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val queryConformanceResults = PersistenceSchema.conformanceResults.filter(_.testsession === testSession.sessionId)
    for {
      // Delete file system data (on success)
      _ <- {
        onSuccessCalls += (() => {
          deleteSessionDataFromFileSystem(testSession)
        })
        DBIO.successful(())
      }
      // Load conformance result
      conformanceResults <- queryConformanceResults.result
      // Calculate if needed new conformance result
      _ <- {
        if (conformanceResults.nonEmpty) {
          // Update the result entries to match the other sessions' results
          for {
            // Get latest relevant test session (except the one being deleted)
            latestTestSession <- {
              if (testSession.systemId.isDefined && testSession.testCaseId.isDefined) {
                // It only makes sense to update conformance statement results if the test session had its system and test case links intact.
                PersistenceSchema.testResults
                  .filter(_.sutId === testSession.systemId.get)
                  .filter(_.testCaseId === testSession.testCaseId)
                  .filter(_.testSessionId =!= testSession.sessionId)
                  .filter(_.endTime.isDefined)
                  .sortBy(_.endTime.desc)
                  .result
                  .headOption
              } else {
                DBIO.successful(None)
              }
            }
            // Update the results.
            _ <- {
              val updateQuery = for { c <- queryConformanceResults } yield (c.testsession, c.result, c.outputMessage, c.updateTime)
              if (latestTestSession.isDefined) {
                // Replace status
                var updateTimeToSet = latestTestSession.get.endTime
                if (updateTimeToSet.isEmpty) {
                  updateTimeToSet = Some(latestTestSession.get.startTime)
                }
                updateQuery.update(Some(latestTestSession.get.sessionId), latestTestSession.get.result, latestTestSession.get.outputMessage, updateTimeToSet)
              } else {
                // Set as empty status
                updateQuery.update(None, TestResultStatus.UNDEFINED.toString, None, None)
              }
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
      // Delete from conformance snapshot results (where we don't care about updating the overall conformance status.
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.testSessionId === testSession.sessionId).map(_.testSessionId).update(None)
      // Delete test result definition
      _ <- PersistenceSchema.testResultDefinitions.filter(_.testSessionId === testSession.sessionId).delete
      // Delete test step reports
      _ <- PersistenceSchema.testStepReports.filter(_.testSessionId === testSession.sessionId).delete
      // Delete test interactions
      _ <- deleteTestInteractions(testSession.sessionId, None)
      // Delete test result
      _ <- PersistenceSchema.testResults.filter(_.testSessionId === testSession.sessionId).delete
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

  private def deleteTestSessionsInternal(sessions: Iterable[TestResult], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val actions = ListBuffer[DBIO[_]]()
    sessions.foreach { session =>
      actions += deleteTestSession(session, onSuccessCalls)
    }
    toDBIO(actions)
  }

  def deleteObsoleteTestResultsForOrganisation(organisationId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for  {
      sessions <- PersistenceSchema.testResults
        .filter(x => x.organizationId === organisationId &&
          (x.testSuiteId.isEmpty || x.testCaseId.isEmpty || x.communityId.isEmpty || x.sutId.isEmpty || x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
        ).result
      _ <- deleteObsoleteTestSessions(sessions, onSuccessCalls)
    } yield ()
  }

  def deleteObsoleteTestResultsForCommunity(communityId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      sessions <- PersistenceSchema.testResults
        .filter(x => x.communityId === communityId &&
          (x.testSuiteId.isEmpty || x.testCaseId.isEmpty || x.sutId.isEmpty || x.organizationId.isEmpty || x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
        )
        .result
      _ <- deleteObsoleteTestSessions(sessions, onSuccessCalls)
    } yield ()
  }

  private def deleteObsoleteTestSessions(sessions: Iterable[TestResult], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val actions = ListBuffer[DBIO[_]]()
    sessions.foreach { session =>
      actions += (
        for {
          // Make sure we don't delete obsolete sessions that may be actively linked to a shared test suite.
          linkedToSharedTestSuite <- {
            if (session.testSuiteId.isEmpty || session.testCaseId.isEmpty) {
              DBIO.successful(false)
            } else {
              PersistenceSchema.testSuites.filter(_.id === session.testSuiteId.get).map(_.shared).result.head
            }
          }
          _ <- if (linkedToSharedTestSuite) {
            // Skip the delete.
            DBIO.successful(())
          } else {
            // Proceed.
            deleteTestSession(session, onSuccessCalls)
          }
        } yield ()
      )
    }
    toDBIO(actions)
  }

  def deleteAllObsoleteTestResults(): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      sessions <- PersistenceSchema.testResults
        .filter(x => x.testSuiteId.isEmpty || x.testCaseId.isEmpty || x.sutId.isEmpty || x.organizationId.isEmpty || x.communityId.isEmpty || x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
        .result
      _ <- deleteObsoleteTestSessions(sessions, onSuccessCalls)
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

  def getTestSessionLog(sessionId: String, startTime: Option[Timestamp], isExpected: Boolean): Option[List[String]] = {
    getTestSessionLog(sessionId, repositoryUtils.getPathForTestSessionObj(sessionId, startTime, isExpected))
  }

  def getTestSessionLog(sessionId: String, isExpected: Boolean): Option[List[String]] = {
    getTestSessionLog(sessionId, repositoryUtils.getPathForTestSessionWrapper(sessionId, isExpected))
  }

  private def getTestSessionLog(sessionId: String, sessionFolderInfo: SessionFolderInfo): Option[List[String]] = {
    if (!sessionFolderInfo.archived) {
      flushSessionLogs(sessionId, Some(sessionFolderInfo.path.toFile))
    }
    try {
      val file = new File(sessionFolderInfo.path.toFile, "log.txt")
      if (file.exists()) {
        Some(Files.readAllLines(Paths.get(file.getAbsolutePath)).asScala.toList)
      } else {
        None
      }
    } finally {
      if (sessionFolderInfo.archived) {
        FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
      }
    }
  }

  def flushSessionLogs(sessionId: String, sessionFolder: Option[File]): Unit = {
    val messages = consumeSessionLogs(sessionId)
    if (messages.nonEmpty) {
      val logFilePath = new File(sessionFolder.getOrElse(repositoryUtils.getPathForTestSession(sessionId, isExpected = false).path.toFile), "log.txt")
      if (!logFilePath.exists()) {
        logFilePath.getParentFile.mkdirs()
        logFilePath.createNewFile()
      }
      import scala.jdk.CollectionConverters._
      Files.write(logFilePath.toPath, messages.asJava, StandardOpenOption.APPEND)
    }
  }

  def saveTestInteraction(interaction: TestInteraction): Unit = {
    exec((PersistenceSchema.testInteractions += interaction).transactionally)
  }

  def getPendingTestSessionsForAdminInteraction(communityId: Option[Long]): Seq[String] = {
    if (communityId.isEmpty) {
      exec(
        PersistenceSchema.testInteractions
          .filter(_.admin === true)
          .map(_.testSessionId)
          .distinct
          .result
      )
    } else {
      exec(
        PersistenceSchema.testInteractions
          .join(PersistenceSchema.testResults).on(_.testSessionId === _.testSessionId)
          .filter(_._1.admin === true)
          .filter(_._2.communityId === communityId)
          .map(_._1.testSessionId)
          .distinct
          .result
      )
    }
  }

  def getTestInteractions(sessionId: String, adminInteractions: Option[Boolean]): List[TestInteraction] = {
    exec(
      PersistenceSchema.testInteractions
      .filter(_.testSessionId === sessionId)
      .filterOpt(adminInteractions)((q, flag) => q.admin === flag)
      .sortBy(_.createTime.desc)
      .result
    ).toList
  }

  def deleteTestInteractionsWrapper(sessionId: String, stepId: Option[String]): Unit = {
    exec(deleteTestInteractions(sessionId, stepId).transactionally)
  }

  def deleteTestInteractions(sessionId: String, stepId: Option[String]): DBIO[_] = {
    PersistenceSchema.testInteractions
      .filter(_.testSessionId === sessionId)
      .filterOpt(stepId)((q, id) => q.testStepId === id)
      .delete
  }

  def schedulePendingTestInteractionNotifications(): Unit = {
    if (Configurations.EMAIL_ENABLED) {
      val windowMinutes = Configurations.EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER
      if (interactionNotificationFuture.isDefined && !interactionNotificationFuture.get.isCancelled) {
        logger.info("Pending test interaction notification check reset.")
        interactionNotificationFuture.get.cancel()
      } else {
        logger.info("Pending test interaction notification check set up.")
      }
      interactionNotificationFuture = Some(actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, windowMinutes.minutes) {
        () => {
          if (Configurations.EMAIL_ENABLED) {
            logger.debug("Checking to send pending test interaction notifications.")
            // Set the start time to 30 minutes before the current time.
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, windowMinutes * -1)
            notifyForPendingTestInteractions(cal)
          }
        }
      })
    } else {
      if (interactionNotificationFuture.isDefined && !interactionNotificationFuture.get.isCancelled) {
        logger.info("Pending test interaction notification check stopped.")
        interactionNotificationFuture.get.cancel()
      }
    }
  }

  private def notifyForPendingTestInteractions(notificationWindowStart: Calendar): Unit = {
    val windowStart = new Timestamp(notificationWindowStart.getTimeInMillis)
    if (Configurations.EMAIL_ENABLED) {
      val query = for {
        communitiesSupportingNotifications <- PersistenceSchema.communities
          .filter(_.supportEmail.isDefined)
          .filter(_.interactionNotification)
          .map(x => (x.id, x.fullname, x.supportEmail.get))
          .result
        communityIdsToNotify <- {
          if (communitiesSupportingNotifications.nonEmpty) {
            PersistenceSchema.testInteractions
              .join(PersistenceSchema.testResults).on(_.testSessionId === _.testSessionId)
              .filter(_._2.communityId.isDefined)
              .filter(_._2.communityId inSet communitiesSupportingNotifications.map(_._1))
              .filter(_._1.admin)
              .filter(_._1.createTime >= windowStart)
              .map(_._2.communityId)
              .distinct
              .result
          } else {
            DBIO.successful(Seq.empty)
          }
        }
      } yield (communityIdsToNotify, communitiesSupportingNotifications)
      val result = exec(query)
      if (result._1.nonEmpty) {
        val idSet = result._1.flatten.toSet
        logger.debug("Sending {} pending test interaction notifications.", idSet.size)
        result._2.foreach { communityData =>
          if (idSet.contains(communityData._1)) {
            sendInteractionNotification(communityData._1, communityData._2, communityData._3)
          }
        }
      }
    }
  }

  private def sendInteractionNotification(communityId: Long, communityName: String, supportEmail: String): Unit = {
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
    scala.concurrent.Future {
      val subject = "Test Bed pending test interactions"
      var content = "<h2>You have pending test interactions</h2>"
      content +=
        "Test sessions are waiting for new administrator interactions ("+communityName+")." +
          "<br/><br/>Click <a href=\""+Configurations.TESTBED_HOME_LINK+"\">here</a> to connect and view the pending test sessions."
      try {
        EmailUtil.sendEmail(Array[String](supportEmail), null, subject, content, null)
      } catch {
        case e:Exception =>
          logger.error("Error while sending pending interaction notification for community ["+communityId+"]", e)
      }
    }
  }

  def getTestResultOfSession(sessionId: String): (TestResult, String) = {
    val result = getTestResultForSessionWrapper(sessionId)
    val testcase = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(result.get._2)))
    val json = JacksonUtil.serializeTestCasePresentation(testcase)
    (result.get._1, json)
  }

  def getOrganisationActiveTestResults(organisationId: Long,
                                       systemIds: Option[List[Long]],
                                       domainIds: Option[List[Long]],
                                       specIds: Option[List[Long]],
                                       specGroupIds: Option[List[Long]],
                                       actorIds: Option[List[Long]],
                                       testSuiteIds: Option[List[Long]],
                                       testCaseIds: Option[List[Long]],
                                       startTimeBegin: Option[String],
                                       startTimeEnd: Option[String],
                                       sessionId: Option[String],
                                       sortColumn: Option[String],
                                       sortOrder: Option[String]): List[TestResult] = {
    exec(
      getTestResultsQuery(None, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, Some(List(organisationId)), systemIds, None, startTimeBegin, startTimeEnd, None, None, sessionId, Some(false), sortColumn, sortOrder)
        .result.map(_.toList)
    )
  }

  def getTestResults(page: Long,
                     limit: Long,
                     organisationId: Long,
                     systemIds: Option[List[Long]],
                     domainIds: Option[List[Long]],
                     specIds: Option[List[Long]],
                     specGroupIds: Option[List[Long]],
                     actorIds: Option[List[Long]],
                     testSuiteIds: Option[List[Long]],
                     testCaseIds: Option[List[Long]],
                     results: Option[List[String]],
                     startTimeBegin: Option[String],
                     startTimeEnd: Option[String],
                     endTimeBegin: Option[String],
                     endTimeEnd: Option[String],
                     sessionId: Option[String],
                     sortColumn: Option[String],
                     sortOrder: Option[String]): (Iterable[TestResult], Int) = {

    val query = getTestResultsQuery(None, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, Some(List(organisationId)), systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, Some(true), sortColumn, sortOrder)
    val output = exec(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield (results, resultCount)
    )
    output
  }

  def getActiveTestResults(communityIds: Option[List[Long]],
                           domainIds: Option[List[Long]],
                           specIds: Option[List[Long]],
                           specGroupIds: Option[List[Long]],
                           actorIds: Option[List[Long]],
                           testSuiteIds: Option[List[Long]],
                           testCaseIds: Option[List[Long]],
                           organisationIds: Option[List[Long]],
                           systemIds: Option[List[Long]],
                           startTimeBegin: Option[String],
                           startTimeEnd: Option[String],
                           sessionId: Option[String],
                           orgParameters: Option[Map[Long, Set[String]]],
                           sysParameters: Option[Map[Long, Set[String]]],
                           sortColumn: Option[String],
                           sortOrder: Option[String]): List[TestResult] = {
    exec(
      getTestResultsQuery(communityIds, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, communityHelper.organisationIdsToUse(organisationIds, orgParameters), communityHelper.systemIdsToUse(systemIds, sysParameters), None, startTimeBegin, startTimeEnd, None, None, sessionId, Some(false), sortColumn, sortOrder)
        .result.map(_.toList)
    )
  }

  def getFinishedTestResults(page: Long,
                             limit: Long,
                             communityIds: Option[List[Long]],
                             domainIds: Option[List[Long]],
                             specIds: Option[List[Long]],
                             specGroupIds: Option[List[Long]],
                             actorIds: Option[List[Long]],
                             testSuiteIds: Option[List[Long]],
                             testCaseIds: Option[List[Long]],
                             organisationIds: Option[List[Long]],
                             systemIds: Option[List[Long]],
                             results: Option[List[String]],
                             startTimeBegin: Option[String],
                             startTimeEnd: Option[String],
                             endTimeBegin: Option[String],
                             endTimeEnd: Option[String],
                             sessionId: Option[String],
                             orgParameters: Option[Map[Long, Set[String]]],
                             sysParameters: Option[Map[Long, Set[String]]],
                             sortColumn: Option[String],
                             sortOrder: Option[String]): (Iterable[TestResult], Int) = {

    val query = getTestResultsQuery(communityIds, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, communityHelper.organisationIdsToUse(organisationIds, orgParameters), communityHelper.systemIdsToUse(systemIds, sysParameters), results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, Some(true), sortColumn, sortOrder)
    val output = exec(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield (results, resultCount)
    )
    output
  }

  def getTestResult(sessionId: String): Option[TestResult] = {
    val query = getTestResultsQuery(None, None, None, None, None, None, None, None, None, None, None, None, None, Some(sessionId), None, None, None)
    exec(query.result.headOption)
  }

  private def getSpecIdsCriterionToUse(specIds: Option[List[Long]], specGroupIds: Option[List[Long]]): Option[List[Long]] = {
    // We use the groups to get the applicable spec IDs. This is because specs can move around in groups, and we shouldn't link
    // test results directly to the groups.
    val specIdsToUse = if (specGroupIds.isDefined && specGroupIds.get.nonEmpty) {
      exec(PersistenceSchema.specifications.filter(_.group inSet specGroupIds.get).map(_.id).result.map(x => Some(x.toList)))
    } else {
      specIds
    }
    specIdsToUse
  }

  private def getTestResultsQuery(communityIds: Option[List[Long]],
                                  domainIds: Option[List[Long]],
                                  specIds: Option[List[Long]],
                                  actorIds: Option[List[Long]],
                                  testSuiteIds: Option[List[Long]],
                                  testCaseIds: Option[List[Long]],
                                  organizationIds: Option[Iterable[Long]],
                                  systemIds: Option[Iterable[Long]],
                                  results: Option[List[String]],
                                  startTimeBegin: Option[String],
                                  startTimeEnd: Option[String],
                                  endTimeBegin: Option[String],
                                  endTimeEnd: Option[String],
                                  sessionId: Option[String],
                                  completedStatus: Option[Boolean],
                                  sortColumn: Option[String],
                                  sortOrder: Option[String]) = {
    var query = PersistenceSchema.testResults
      .filterOpt(communityIds)((table, ids) => table.communityId inSet ids)
      .filterOpt(domainIds)((table, ids) => table.domainId inSet ids)
      .filterOpt(specIds)((table, ids) => table.specificationId inSet ids)
      .filterOpt(actorIds)((table, ids) => table.actorId inSet ids)
      .filterOpt(testCaseIds)((table, ids) => table.testCaseId inSet ids)
      .filterOpt(organizationIds)((table, ids) => table.organizationId inSet ids)
      .filterOpt(systemIds)((table, ids) => table.sutId inSet ids)
      .filterOpt(results)((table, results) => table.result inSet results)
      .filterOpt(testSuiteIds)((table, ids) => table.testSuiteId inSet ids)
      .filterOpt(startTimeBegin)((table, timeStr) => table.startTime >= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(startTimeEnd)((table, timeStr) => table.startTime <= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(endTimeBegin)((table, timeStr) => table.endTime >= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(endTimeEnd)((table, timeStr) => table.endTime <= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(sessionId)((table, id) => table.testSessionId === id)
      .filterOpt(completedStatus)((table, completed) => if (completed) table.endTime.isDefined else table.endTime.isEmpty)
    // Apply sorting
    if (sortColumn.isDefined && sortOrder.isDefined) {
      if (sortOrder.get == "asc") {
        query = sortColumn.get match {
          case "specification" => query.sortBy(_.specification)
          case "session" => query.sortBy(_.testSessionId)
          case "startTime" => query.sortBy(_.startTime)
          case "endTime" => query.sortBy(_.endTime)
          case "organization" => query.sortBy(_.organization)
          case "system" => query.sortBy(_.sut)
          case "result" => query.sortBy(_.result)
          case "testCase" => query.sortBy(_.testCase)
          case "actor" => query.sortBy(_.actor)
          case _ => query
        }
      }
      if (sortOrder.get == "desc") {
        query = sortColumn.get match {
          case "specification" => query.sortBy(_.specification.desc)
          case "session" => query.sortBy(_.testSessionId.desc)
          case "startTime" => query.sortBy(_.startTime.desc)
          case "endTime" => query.sortBy(_.endTime.desc)
          case "organization" => query.sortBy(_.organization.desc)
          case "system" => query.sortBy(_.sut.desc)
          case "result" => query.sortBy(_.result.desc)
          case "testCase" => query.sortBy(_.testCase.desc)
          case "actor" => query.sortBy(_.actor.desc)
          case _ => query
        }
      }
    }
    query
  }

}
