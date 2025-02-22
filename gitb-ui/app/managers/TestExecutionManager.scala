package managers

import actors.SessionManagerActor
import actors.events.{ConformanceStatementSucceededEvent, TestSessionFailedEvent, TestSessionSucceededEvent}
import actors.events.sessions.{PrepareTestSessionsEvent, TerminateSessionsEvent}
import com.gitb.core.{ActorConfiguration, AnyContent, Configuration, ValueEmbeddingEnumeration}
import com.gitb.tr.TestResultType
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException}
import models.Enums.InputMappingMatchType
import models._
import models.automation.{InputMappingContent, TestSessionLaunchInfo, TestSessionLaunchRequest}
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils, TimeUtil}

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestExecutionManager @Inject() (testbedClient: managers.TestbedBackendClient,
                                      domainParameterManager: DomainParameterManager,
                                      testResultManager: TestResultManager,
                                      repositoryUtils: RepositoryUtils,
                                      actorManager: ActorManager,
                                      actorSystem: ActorSystem,
                                      organisationManager: OrganizationManager,
                                      systemManager: SystemManager,
                                      apiHelper: AutomationApiHelper,
                                      triggerHelper: TriggerHelper,
                                      conformanceManager: ConformanceManager,
                                      dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._
  private var sessionManagerActor: Option[ActorRef] = None
  private final val reservedInputNames = Set(
    Constants.systemTestVariable, Constants.organisationTestVariable, Constants.domainTestVariable,
    Constants.systemConfigurationName, Constants.organisationConfigurationName, Constants.domainConfigurationName
  )

  def startHeadlessTestSessions(testCaseIds: List[Long], systemId: Long, actorId: Long, testCaseToInputMap: Option[Map[Long, List[AnyContent]]], sessionIdsToAssign: Option[Map[Long, String]], forceSequentialExecution: Boolean): Unit = {
    if (testCaseIds.nonEmpty) {
      // Load information common to all test sessions
      val statementParameters = loadConformanceStatementParameters(systemId, actorId)
      val domainParameters = loadDomainParameters(actorId)
      val organisationData = loadOrganisationParameters(systemId)
      val systemParameters = loadSystemParameters(systemId)
      // Schedule test sessions
      val launchInfo = PrepareTestSessionsEvent(TestSessionLaunchData(
        organisationData._1.community, organisationData._1.id, systemId, actorId, testCaseIds,
        statementParameters, domainParameters, organisationData._2, systemParameters,
        testCaseToInputMap, sessionIdsToAssign, forceSequentialExecution))
      getSessionManagerActor().tell(launchInfo, ActorRef.noSender)
    }
  }

  def endSession(session:String): Unit = {
    testbedClient.stop(session)
    setEndTimeNow(session)
  }

  def loadConformanceStatementParameters(systemId: Long, actorId: Long): List[ActorConfiguration] = {
    // Get parameter information from the DB
    val result = exec(for {
      actorIdentifier <- PersistenceSchema.actors.filter(_.id === actorId).map(x => x.actorId).result.head
      parameterData <- PersistenceSchema.endpoints
        .join(PersistenceSchema.parameters).on(_.id === _.endpoint)
        .joinLeft(PersistenceSchema.configs).on((p, c) => p._2.id === c.parameter && c.system === systemId)
        .filter(_._1._1.actor === actorId)
        .map(x => (
          x._1._1.name, // Endpoint name [1]
          x._1._2.name, // Parameter name [2]
          x._2, // Parameter value [3]
          x._1._2.dependsOn, // DependsOn [4]
          x._1._2.dependsOnValue, // DependsOnValue [5]
          x._1._2.kind, // Kind [6]
          x._1._2.id, // Parameter ID [7]
          x._1._2.use, // Parameter use [8]
          x._1._2.notForTests, // Parameter not for tests [9]
          x._1._2.testKey // Parameter test key [10]
        )).result
      parameters <- DBIO.successful(parameterData.map { x =>
        val parameterValueData: (Option[String], Option[String]) = x._3 match {
          case Some(v) => (Some(v.value), v.contentType)
          case None => (None, None)
        }
        new SystemConfigurationParameterMinimal(x._7, x._1, x._2, x._10, parameterValueData._1, x._4, x._5, x._6, x._8, parameterValueData._2, x._9)
      })
    } yield (actorIdentifier, parameters))
    // Keep only the values that have valid prerequisites defined.
    val parameterData = PrerequisiteUtil.withValidPrerequisites(result._2)
    val actorMap = new mutable.HashMap[String, ActorConfiguration]()
    parameterData.foreach{ p =>
      if (p.parameterUse == "R" && p.parameterValue.isEmpty) {
        throw MissingRequiredParameterException(p.parameterName, "Missing required conformance statement parameter ["+p.parameterName+"]")
      } else if (!p.notForTests && p.parameterValue.isDefined) {
        var actorConfigEntry = actorMap.get(p.endpointName)
        if (actorConfigEntry.isEmpty) {
          val actorConfig = new ActorConfiguration()
          actorConfig.setActor(result._1)
          actorConfig.setEndpoint(p.endpointName)
          actorMap += (p.endpointName -> actorConfig)
          actorConfigEntry = Some(actorConfig)
        }
        val config = new Configuration()
        config.setName(p.parameterKey)
        if (p.parameterKind == "SECRET") {
          config.setValue(MimeUtil.decryptString(p.parameterValue.get))
        } else if (p.parameterKind == "BINARY") {
          config.setValue(MimeUtil.getFileAsDataURL(repositoryUtils.getStatementParameterFile(p.parameterId, systemId), p.valueContentType.orNull))
        } else {
          config.setValue(p.parameterValue.get)
        }
        actorConfigEntry.get.getConfig.add(config)
      }
    }
    actorMap.values.toList
  }

  def loadDomainParameters(actorId: Long): Option[ActorConfiguration] = {
    val domainId = actorManager.getById(actorId).get.domain
    val parameters = domainParameterManager.getDomainParameters(domainId, loadValues = true, Some(true), onlySimple = false)
    if (parameters.nonEmpty) {
      val domainConfiguration = new ActorConfiguration()
      domainConfiguration.setActor(Constants.domainConfigurationName)
      domainConfiguration.setEndpoint(Constants.domainConfigurationName)
      parameters.foreach { parameter =>
        if (parameter.kind == "HIDDEN") {
          addConfig(domainConfiguration, parameter.name, MimeUtil.decryptString(parameter.value.get))
        } else if (parameter.kind == "BINARY") {
          addConfig(domainConfiguration, parameter.name, MimeUtil.getFileAsDataURL(repositoryUtils.getDomainParameterFile(domainId, parameter.id), parameter.contentType.orNull))
        } else {
          addConfig(domainConfiguration, parameter.name, parameter.value.get)
        }
      }
      Some(domainConfiguration)
    } else {
      None
    }
  }

  def loadOrganisationParameters(systemId: Long): (Organizations, ActorConfiguration) = {
    val organisation = organisationManager.getOrganizationBySystemId(systemId)
    val organisationConfiguration = new ActorConfiguration()
    organisationConfiguration.setActor(Constants.organisationConfigurationName)
    organisationConfiguration.setEndpoint(Constants.organisationConfigurationName)
    addConfig(organisationConfiguration, Constants.organisationConfiguration_fullName, organisation.fullname)
    addConfig(organisationConfiguration, Constants.organisationConfiguration_shortName, organisation.shortname)
    val organisationProperties = PrerequisiteUtil.withValidPrerequisites(organisationManager.getOrganisationParameterValues(organisation.id))
    if (organisationProperties.nonEmpty) {
      organisationProperties.foreach{ property =>
        if (property.parameter.use == "R" && property.value.isEmpty) {
          throw MissingRequiredParameterException(property.parameter.name, "Missing required organisation parameter ["+property.parameter.name+"]")
        }
        if (!property.parameter.notForTests && property.value.isDefined) {
          if (property.parameter.kind == "SECRET") {
            addConfig(organisationConfiguration, property.parameter.testKey, MimeUtil.decryptString(property.value.get.value))
          } else if (property.parameter.kind == "BINARY") {
            addConfig(organisationConfiguration, property.parameter.testKey, MimeUtil.getFileAsDataURL(repositoryUtils.getOrganisationPropertyFile(property.parameter.id, organisation.id), property.value.get.contentType.orNull))
          } else {
            addConfig(organisationConfiguration, property.parameter.testKey, property.value.get.value)
          }
        }
      }
    }
    (organisation, organisationConfiguration)
  }

  def loadSystemParameters(systemId: Long): ActorConfiguration = {
    val system = systemManager.getSystemById(systemId).get
    val systemConfiguration = new ActorConfiguration()
    systemConfiguration.setActor(Constants.systemConfigurationName)
    systemConfiguration.setEndpoint(Constants.systemConfigurationName)
    addConfig(systemConfiguration, Constants.systemConfiguration_fullName, system.fullname)
    addConfig(systemConfiguration, Constants.systemConfiguration_shortName, system.shortname)
    if (system.version.nonEmpty) {
      addConfig(systemConfiguration, Constants.systemConfiguration_version, system.version.get)
    }
    addConfig(systemConfiguration, Constants.systemConfiguration_apiKey, system.apiKey)
    val systemProperties = PrerequisiteUtil.withValidPrerequisites(systemManager.getSystemParameterValues(systemId))
    if (systemProperties.nonEmpty) {
      systemProperties.foreach{ property =>
        if (property.parameter.use == "R" && property.value.isEmpty) {
          throw exceptions.MissingRequiredParameterException(property.parameter.name, "Missing required system parameter ["+property.parameter.name+"]")
        }
        if (!property.parameter.notForTests && property.value.isDefined) {
          if (property.parameter.kind == "SECRET") {
            addConfig(systemConfiguration, property.parameter.testKey, MimeUtil.decryptString(property.value.get.value))
          } else if (property.parameter.kind == "BINARY") {
            addConfig(systemConfiguration, property.parameter.testKey, MimeUtil.getFileAsDataURL(repositoryUtils.getSystemPropertyFile(property.parameter.id, systemId), property.value.get.contentType.orNull))
          } else {
            addConfig(systemConfiguration, property.parameter.testKey, property.value.get.value)
          }
        }
      }
    }
    systemConfiguration
  }

  private def addConfig(configuration: ActorConfiguration, key: String, value: String) =  {
    val config = new Configuration()
    config.setName(key)
    config.setValue(value)
    configuration.getConfig.add(config)
  }

  private def getSessionManagerActor(): ActorRef = {
    if (sessionManagerActor.isEmpty) {
      sessionManagerActor = Some(
        actorSystem
          .actorSelection("/user/"+SessionManagerActor.actorName)
          .resolveOne(Duration.of(5, ChronoUnit.SECONDS))
          .toCompletableFuture
          .get()
      )
    }
    sessionManagerActor.get
  }

  private def prepareAnyContentInput(input: AnyContent): Unit = {
    if (input.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64 && input.getValue != null) {
      // Needs conversion to data URL
      input.setValue(MimeUtil.base64AsDataURL(input.getValue))
    }
  }

  def processAutomationLaunchRequest(request: TestSessionLaunchRequest): Seq[TestSessionLaunchInfo] = {
    val q = for {
      statementIds <- apiHelper.getStatementIdsForApiKeys(request.organisation, request.system, request.actor, None, Some(request.testSuite), Some(request.testCase))
      testCaseData <- {
        var query = PersistenceSchema.conformanceResults
          .join(PersistenceSchema.testSuites).on(_.testsuite === _.id)
          .join(PersistenceSchema.testCases).on(_._1.testcase === _.id)
          .filter(_._1._1.actor === statementIds.actorId)
          .filter(_._1._1.sut === statementIds.systemId)
        if (request.testSuite.nonEmpty) {
          query = query.filter(_._1._2.identifier inSet request.testSuite.toSet)
        }
        if (request.testCase.nonEmpty) {
          query = query.filter(_._2.identifier inSet request.testCase.toSet)
        }
        query
          .sortBy(x => (x._1._2.id.asc, x._2.testSuiteOrder.asc))
          .map(x => (x._1._2.id, x._1._2.identifier, x._2.id, x._2.identifier)) // (TS ID, TS identifier, TC ID, TC identifier)
          .result
      }
      testCaseInputData <- {
        val sessionIdsToAssign = mutable.HashMap[Long, String]()
        val sessionLaunchInfo = ListBuffer[TestSessionLaunchInfo]()
        val requestedTestSuites = new mutable.TreeSet[String]()
        val requestedTestCases = new mutable.TreeSet[String]()
        requestedTestSuites.addAll(request.testSuite)
        requestedTestCases.addAll(request.testCase)
        // Map to lookup TC IDs only based on TC identifiers
        val testCaseIdentifierToIdMap = new mutable.HashMap[String, mutable.HashSet[Long]]()
        // Map to lookup the TC identifiers for given TS identifiers
        val testSuiteIdentifierToTestCaseIdentifiersMap = new mutable.HashMap[String, mutable.HashSet[String]]()
        // Map to lookup the TC ids for specific TS and TC identifier combinations
        val completeTestCaseIdentifierToIdMap = new mutable.HashMap[String, Long]()
        // Prepare test case IDs
        val testCaseIds = ListBuffer[Long]()
        testCaseData.foreach { ids =>
          val testSuiteIdentifier = ids._2
          val testCaseId = ids._3
          val testCaseIdentifier = ids._4
          requestedTestSuites.remove(testSuiteIdentifier)
          requestedTestCases.remove(testCaseIdentifier)
          testCaseIds += testCaseId
          if (testCaseIdentifierToIdMap.contains(testCaseIdentifier)) {
            // The test case IDs are recorded as a list buffer as we may have multiple (in difference test suites)
            testCaseIdentifierToIdMap(testCaseIdentifier) += testCaseId
          } else {
            val set = new mutable.HashSet[Long]()
            set.add(testCaseId)
            testCaseIdentifierToIdMap += (testCaseIdentifier -> set)
          }
          if (testSuiteIdentifierToTestCaseIdentifiersMap.contains(testSuiteIdentifier)) {
            testSuiteIdentifierToTestCaseIdentifiersMap(testSuiteIdentifier).add(testCaseIdentifier)
          } else {
            val set = new mutable.HashSet[String]()
            set.add(testCaseIdentifier)
            testSuiteIdentifierToTestCaseIdentifiersMap += (testSuiteIdentifier -> set)
          }
          completeTestCaseIdentifierToIdMap += (testSuiteIdentifier+"|"+testCaseIdentifier -> testCaseId)
          // Prepare test session ID
          val testSessionId = UUID.randomUUID().toString
          sessionIdsToAssign += (testCaseId -> testSessionId)
          sessionLaunchInfo += TestSessionLaunchInfo(testSuiteIdentifier, testCaseIdentifier, testSessionId)
        }
        if (requestedTestCases.nonEmpty) {
          throw AutomationApiException(ErrorCodes.API_TEST_CASE_NOT_FOUND, "One or more requested test cases could not be found ["+requestedTestCases.mkString(",")+"]")
        }
        if (requestedTestSuites.nonEmpty) {
          throw AutomationApiException(ErrorCodes.API_TEST_SUITE_NOT_FOUND, "One or more requested test suites could not be found ["+requestedTestSuites.mkString(",")+"]")
        }
        // Prepare test case inputs
        var testCaseInputs: Option[Map[Long, List[AnyContent]]] = None
        if (request.inputMapping.nonEmpty) {
          val mapToUse = new mutable.HashMap[Long, mutable.HashMap[String, InputMappingContent]]()
          request.inputMapping.foreach { mapping =>
            prepareAnyContentInput(mapping.input)
            if (mapping.testSuite.nonEmpty && mapping.testCase.nonEmpty) {
              // Apply to test cases within test suites
              mapping.testSuite.foreach { tsMapping =>
                mapping.testCase.foreach { tcMapping =>
                  if (testSuiteIdentifierToTestCaseIdentifiersMap.contains(tsMapping)) {
                    if (testSuiteIdentifierToTestCaseIdentifiersMap(tsMapping).contains(tcMapping)) {
                      val tcId = completeTestCaseIdentifierToIdMap.get(tsMapping+"|"+tcMapping)
                      if (tcId.isDefined) {
                        recordTestCaseInput(mapToUse, tcId.get, new InputMappingContent(mapping.input, InputMappingMatchType.TEST_SUITE_AND_TEST_CASE))
                      }
                    } else {
                      throw AutomationApiException(ErrorCodes.API_TEST_CASE_INPUT_MAPPING_INVALID, "Input mapping references an invalid test case identifier ["+tcMapping+"]")
                    }
                  } else {
                    throw AutomationApiException(ErrorCodes.API_TEST_SUITE_INPUT_MAPPING_INVALID, "Input mapping references an invalid test suite identifier ["+tsMapping+"]")
                  }
                }
              }
            } else if (mapping.testSuite.nonEmpty) {
              // Apply to all test cases of test suite
              mapping.testSuite.foreach { tsMapping =>
                if (testSuiteIdentifierToTestCaseIdentifiersMap.contains(tsMapping)) {
                  testSuiteIdentifierToTestCaseIdentifiersMap(tsMapping).foreach { tcIdentifier =>
                    val tcId = completeTestCaseIdentifierToIdMap.get(tsMapping+"|"+tcIdentifier)
                    if (tcId.isDefined) {
                      recordTestCaseInput(mapToUse, tcId.get, new InputMappingContent(mapping.input, InputMappingMatchType.TEST_SUITE))
                    }
                  }
                } else {
                  throw AutomationApiException(ErrorCodes.API_TEST_SUITE_INPUT_MAPPING_INVALID, "Input mapping references an invalid test suite identifier ["+tsMapping+"]")
                }
              }
            } else if (mapping.testCase.nonEmpty) {
              // Apply to all test cases matching identifier
              mapping.testCase.foreach { tcMapping =>
                if (testCaseIdentifierToIdMap.contains(tcMapping)) {
                  testCaseIdentifierToIdMap(tcMapping).foreach { tcId =>
                    recordTestCaseInput(mapToUse, tcId, new InputMappingContent(mapping.input, InputMappingMatchType.TEST_CASE))
                  }
                } else {
                  throw AutomationApiException(ErrorCodes.API_TEST_CASE_INPUT_MAPPING_INVALID, "Input mapping references an invalid test case identifier ["+tcMapping+"]")
                }
              }
            } else {
              // Apply to all
              testCaseIds.foreach { tcId =>
                recordTestCaseInput(mapToUse, tcId, new InputMappingContent(mapping.input, InputMappingMatchType.DEFAULT))
              }
            }
          }
          testCaseInputs = Some(mapToUse.map { entry =>
            val map: mutable.HashMap[String, InputMappingContent] = entry._2
            entry._1 -> map.values.toList.map(_.input)
          }.toMap)
        }
        if (testCaseIds.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_NO_TEST_CASES_TO_EXECUTE, "No test cases specified for execution")
        }
        DBIO.successful((testCaseIds.toList, testCaseInputs, sessionLaunchInfo.toList, sessionIdsToAssign.toMap))
      }
    } yield (testCaseInputData._1, statementIds.systemId, statementIds.actorId, testCaseInputData._2, testCaseInputData._3, testCaseInputData._4)
    val results = exec(q)
    startHeadlessTestSessions(results._1, results._2, results._3, results._4, Some(results._6), request.forceSequentialExecution)
    results._5
  }

  private def recordTestCaseInput(map: mutable.HashMap[Long, mutable.HashMap[String, InputMappingContent]], testCaseId: Long, input: InputMappingContent): Unit = {
    if (input.input.getName == null || input.input.getName.isBlank) {
      throw AutomationApiException(ErrorCodes.INPUT_WITHOUT_NAME, "An input was provided that did not define its name")
    } else if (reservedInputNames.contains(input.input.getName)) {
      throw AutomationApiException(ErrorCodes.INPUT_WITH_RESERVED_NAME, "An input was provided with a reserved name ["+input.input.getName+"]")
    } else if (StringUtils.containsAny(input.input.getName, '$', '{', '}')) {
      throw AutomationApiException(ErrorCodes.INPUT_WITH_INVALID_NAME, "An input was provided with an invalid name ["+input.input.getName+"]. Inputs must not contains characters ['$', '{', '}']")
    }
    if (map.contains(testCaseId)) {
      val currentInputs = map(testCaseId)
      if (currentInputs.contains(input.input.getName)) {
        val currentInput = currentInputs(input.input.getName)
        val currentMatchType = currentInput.matchType
        if (currentMatchType.id <= input.matchType.id) {
          // Current match type is less specific - replace
          currentInput.input = input.input
          currentInput.matchType = input.matchType
        }
      } else {
        currentInputs += (input.input.getName -> input)
      }
    } else {
      val list = mutable.HashMap[String, InputMappingContent]()
      list += (input.input.getName -> input)
      map += (testCaseId -> list)
    }
  }

  def processAutomationStopRequest(organisationKey: String, sessionIds: List[String]): Unit = {
    val sessionData = exec(
      for {
        organisationData <- apiHelper.loadOrganisationDataForAutomationProcessing(organisationKey)
        _ <- apiHelper.checkOrganisationForAutomationApiUse(organisationData)
        verifiedSessionIds <- PersistenceSchema.testResults
            .filter(_.organizationId === organisationData.get.organisationId)
            .filter(_.testSessionId inSet sessionIds)
            .map(x => x.testSessionId)
            .result
      } yield (organisationData.get.organisationId, verifiedSessionIds)
    )
    // Signal stop to session manager (for tests that haven't started yet)
    getSessionManagerActor().tell(TerminateSessionsEvent(sessionData._1, sessionIds.toSet), ActorRef.noSender)
    // Signal stop to test engine (for already tests that are already running).
    sessionData._2.foreach { sessionId =>
      testbedClient.stop(sessionId)
    }
  }

  def finishTestReport(sessionId: String, status: TestResultType, outputMessage: Option[String]): Unit = {
    val now = Some(TimeUtil.getCurrentTimestamp())
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      startTime <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(_.startTime).result.headOption
      _ <- {
        if (startTime.isDefined) {
          // Test session finalisation and cleanup actions.
          for {
            _ <- PersistenceSchema.testResults
              .filter(_.testSessionId === sessionId)
              .map(x => (x.result, x.endTime, x.outputMessage))
              .update(status.value(), now, outputMessage)
            // Delete any pending test interactions
            _ <- testResultManager.deleteTestInteractions(sessionId, None)
            // Update also the conformance results for the system
            _ <- PersistenceSchema.conformanceResults
              .filter(_.testsession === sessionId)
              .map(x => (x.result, x.outputMessage, x.updateTime))
              .update(status.value(), outputMessage, now)
            // Delete temporary test session data (used for user interactions).
            _ <- {
              onSuccessCalls += (() => {
                val sessionFolderInfo = repositoryUtils.getPathForTestSessionObj(sessionId, startTime, isExpected = true)
                val tempDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = true)
                FileUtils.deleteQuietly(tempDataFolder.toFile)
              })
              DBIO.successful(())
            }
          } yield ()
        } else {
          // The test session was not recorded - nothing to do.
          DBIO.successful(())
        }
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    // Triggers linked to test sessions: (communityID, systemID, actorID)
    val sessionIds: Option[(Option[Long], Option[Long], Option[Long])] = exec(
      PersistenceSchema.testResults
        .filter(_.testSessionId === sessionId)
        .map(x => (x.communityId, x.sutId, x.actorId))
        .result
        .headOption
    )
    if (sessionIds.isDefined && sessionIds.get._1.isDefined && sessionIds.get._2.isDefined && sessionIds.get._3.isDefined) {
      val communityId = sessionIds.get._1.get
      val systemId = sessionIds.get._2.get
      // We have all the data we need to fire the triggers.
      if (status == TestResultType.SUCCESS) {
        triggerHelper.publishTriggerEvent(new TestSessionSucceededEvent(communityId, sessionId))
      } else if (status == TestResultType.FAILURE) {
        triggerHelper.publishTriggerEvent(new TestSessionFailedEvent(communityId, sessionId))
      }
      // See if the conformance statement is now successfully completed and fire an additional trigger if so.
      val completedActors = conformanceManager.getCompletedConformanceStatementsForTestSession(systemId, sessionId)
      completedActors.foreach { actorId =>
        triggerHelper.publishTriggerEvent(new ConformanceStatementSucceededEvent(communityId, systemId, actorId))
      }
    }
    // Flush remaining log messages
    testResultManager.flushSessionLogs(sessionId, None)
  }

  private def setEndTimeNow(sessionId: String): Unit = {
    val now = Some(TimeUtil.getCurrentTimestamp())
    exec (
      (for {
        testSession <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
        _ <- {
          if (testSession.isDefined) {
            (for {t <- PersistenceSchema.testResults if t.testSessionId === sessionId} yield t.endTime).update(now) andThen
              (for {c <- PersistenceSchema.conformanceResults if c.testsession === sessionId} yield (c.result, c.updateTime)).update(testSession.get.result, now)
          } else {
            DBIO.successful(())
          }
        }
      } yield ()
        ).transactionally
    )
  }

}
