/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import actors.SessionManagerActor
import actors.events.sessions.{PrepareTestSessionsEvent, TerminateSessionsEvent}
import actors.events.{ConformanceStatementSucceededEvent, TestSessionFailedEvent, TestSessionSucceededEvent}
import com.gitb.core.{AnyContent, Configuration, ValueEmbeddingEnumeration}
import com.gitb.tr.TestResultType
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException}
import managers.triggers.TriggerHelper
import models.Enums.InputMappingMatchType
import models._
import models.automation.{InputMappingContent, TestSessionLaunchInfo, TestSessionLaunchRequest}
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.actor.{ActorRef, ActorSystem, Scheduler}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils, TimeUtil}

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

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
                                      dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._
  private val LOGGER = LoggerFactory.getLogger(classOf[TestExecutionManager])
  private var sessionManagerActor: Option[ActorRef] = None
  private final val reservedInputNames = Set(
    Constants.systemTestVariable, Constants.organisationTestVariable, Constants.domainTestVariable,
    Constants.systemConfigurationName, Constants.organisationConfigurationName, Constants.domainConfigurationName
  )

  def startHeadlessTestSessions(testCaseIds: List[Long], systemId: Long, actorId: Long, testCaseToInputMap: Option[Map[Long, List[AnyContent]]], sessionIdsToAssign: Option[Map[Long, String]], forceSequentialExecution: Boolean): Future[Unit] = {
    if (testCaseIds.nonEmpty) {
      // Load information common to all test sessions
      loadConformanceStatementParameters(systemId, actorId, onlySimple = false).zip(
        loadDomainParametersByActorId(actorId, onlySimple = false).zip(
          loadOrganisationParameters(systemId, onlySimple = false).zip(
            loadSystemParameters(systemId, onlySimple = false)
          )
        )
      ).map { results =>
        val statementParameters = results._1
        val domainParameters = results._2._1
        val organisationData = results._2._2._1
        val systemParameters = results._2._2._2
        // Schedule test sessions
        val launchInfo = PrepareTestSessionsEvent(TestSessionLaunchData(
          organisationData._1.community, organisationData._1.id, systemId, actorId, testCaseIds,
          statementParameters, domainParameters, organisationData._2, systemParameters,
          testCaseToInputMap, sessionIdsToAssign, forceSequentialExecution))
        getSessionManagerActor().tell(launchInfo, ActorRef.noSender)
      }
    } else {
      Future.successful(())
    }
  }

  def endSession(session:String): Future[Unit] = {
    testbedClient.stop(session).flatMap { _ =>
      setEndTimeNow(session)
    }
  }

  def loadConformanceStatementParameters(systemId: Long, actorId: Long, onlySimple: Boolean): Future[List[TypedActorConfiguration]] = {
    // Get parameter information from the DB
    DB.run(
      for {
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
      } yield (actorIdentifier, parameters)
    ).map { result =>
      // Keep only the values that have valid prerequisites defined.
      val parameterData = PrerequisiteUtil.withValidPrerequisites(result._2)
      val actorMap = new mutable.HashMap[String, (String, String, ListBuffer[TypedConfiguration])]()
      parameterData.foreach{ p =>
        if (p.parameterUse == "R" && p.parameterValue.isEmpty) {
          throw MissingRequiredParameterException(p.parameterName, "Missing required conformance statement parameter ["+p.parameterName+"]")
        } else if (!p.notForTests && p.parameterValue.isDefined && (!onlySimple || p.parameterKind == "SIMPLE")) {
          var actorConfigEntry = actorMap.get(p.endpointName)
          if (actorConfigEntry.isEmpty) {
            val actorConfigData = (result._1, p.endpointName, new ListBuffer[TypedConfiguration])
            actorMap += (p.endpointName -> actorConfigData)
            actorConfigEntry = Some(actorConfigData)
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
          actorConfigEntry.get._3 += TypedConfiguration(config, p.parameterKind)
        }
      }
      actorMap.values.map(actorData => TypedActorConfiguration(actorData._1, actorData._2, actorData._3.toList)).toList
    }
  }

  def loadDomainParametersByDomainId(domainId: Long, onlySimple: Boolean): Future[Option[TypedActorConfiguration]] = {
    domainParameterManager.getDomainParameters(domainId, loadValues = true, Some(true), onlySimple = onlySimple).map { parameters =>
      if (parameters.nonEmpty) {
        val configs = parameters.map(parameter => {
          if (parameter.kind == "HIDDEN") {
            toTypedConfig(parameter.name, MimeUtil.decryptString(parameter.value.get), parameter.kind)
          } else if (parameter.kind == "BINARY") {
            toTypedConfig(parameter.name, MimeUtil.getFileAsDataURL(repositoryUtils.getDomainParameterFile(domainId, parameter.id), parameter.contentType.orNull), parameter.kind)
          } else {
            toTypedConfig(parameter.name, parameter.value.get, parameter.kind)
          }
        })
        Some(TypedActorConfiguration(Constants.domainConfigurationName, Constants.domainConfigurationName, configs))
      } else {
        None
      }
    }
  }

  def loadDomainParametersByActorId(actorId: Long, onlySimple: Boolean): Future[Option[TypedActorConfiguration]] = {
    actorManager.getById(actorId).flatMap { actor =>
      val domainId = actor.get.domain
      loadDomainParametersByDomainId(domainId, onlySimple)
    }
  }

  def loadOrganisationParameters(systemId: Long, onlySimple: Boolean): Future[(Organizations, TypedActorConfiguration)] = {
    organisationManager.getOrganizationBySystemId(systemId).flatMap { organisation =>
      val configs = new ListBuffer[TypedConfiguration]
      configs += toTypedConfig(Constants.organisationConfiguration_fullName, organisation.fullname, "SIMPLE")
      configs += toTypedConfig(Constants.organisationConfiguration_shortName, organisation.shortname, "SIMPLE")
      organisationManager.getOrganisationParameterValues(organisation.id, onlySimple = Some(onlySimple)).map { parameters =>
        val organisationProperties = PrerequisiteUtil.withValidPrerequisites(parameters)
        if (organisationProperties.nonEmpty) {
          organisationProperties.foreach{ property =>
            if (property.parameter.use == "R" && property.value.isEmpty) {
              throw MissingRequiredParameterException(property.parameter.name, "Missing required organisation parameter ["+property.parameter.name+"]")
            }
            if (!property.parameter.notForTests && property.value.isDefined) {
              if (property.parameter.kind == "SECRET") {
                configs += toTypedConfig(property.parameter.testKey, MimeUtil.decryptString(property.value.get.value), property.parameter.kind)
              } else if (property.parameter.kind == "BINARY") {
                configs += toTypedConfig(property.parameter.testKey, MimeUtil.getFileAsDataURL(repositoryUtils.getOrganisationPropertyFile(property.parameter.id, organisation.id), property.value.get.contentType.orNull), property.parameter.kind)
              } else {
                configs += toTypedConfig(property.parameter.testKey, property.value.get.value, property.parameter.kind)
              }
            }
          }
        }
        (organisation, TypedActorConfiguration(Constants.organisationConfigurationName, Constants.organisationConfigurationName, configs.toList))
      }
    }
  }

  def loadSystemParameters(systemId: Long, onlySimple: Boolean): Future[TypedActorConfiguration] = {
    systemManager.getSystemById(systemId).flatMap { result =>
      val system = result.get
      val configs = new ListBuffer[TypedConfiguration]
      configs += toTypedConfig(Constants.systemConfiguration_fullName, system.fullname, "SIMPLE")
      configs += toTypedConfig(Constants.systemConfiguration_shortName, system.shortname, "SIMPLE")
      if (system.version.nonEmpty) {
        configs += toTypedConfig(Constants.systemConfiguration_version, system.version.get, "SIMPLE")
      }
      configs += toTypedConfig(Constants.systemConfiguration_apiKey, system.apiKey, "SIMPLE")
      systemManager.getSystemParameterValues(systemId, onlySimple = Some(onlySimple)).map { parameters =>
        val systemProperties = PrerequisiteUtil.withValidPrerequisites(parameters)
        if (systemProperties.nonEmpty) {
          systemProperties.foreach{ property =>
            if (property.parameter.use == "R" && property.value.isEmpty) {
              throw exceptions.MissingRequiredParameterException(property.parameter.name, "Missing required system parameter ["+property.parameter.name+"]")
            }
            if (!property.parameter.notForTests && property.value.isDefined) {
              if (property.parameter.kind == "SECRET") {
                configs += toTypedConfig(property.parameter.testKey, MimeUtil.decryptString(property.value.get.value), property.parameter.kind)
              } else if (property.parameter.kind == "BINARY") {
                configs += toTypedConfig(property.parameter.testKey, MimeUtil.getFileAsDataURL(repositoryUtils.getSystemPropertyFile(property.parameter.id, systemId), property.value.get.contentType.orNull), property.parameter.kind)
              } else {
                configs += toTypedConfig(property.parameter.testKey, property.value.get.value, property.parameter.kind)
              }
            }
          }
        }
        TypedActorConfiguration(Constants.systemConfigurationName, Constants.systemConfigurationName, configs.toList)
      }
    }
  }

  private def toTypedConfig(key: String, value: String, kind: String): TypedConfiguration =  {
    val config = new Configuration()
    config.setName(key)
    config.setValue(value)
    TypedConfiguration(config, kind)
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

  def processAutomationLaunchRequest(request: TestSessionLaunchRequest): Future[Seq[TestSessionLaunchInfo]] = {
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
        // Map to look up the TC identifiers for given TS identifiers
        val testSuiteIdentifierToTestCaseIdentifiersMap = new mutable.HashMap[String, mutable.HashSet[String]]()
        // Map to look up the TC ids for specific TS and TC identifier combinations
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
          sessionLaunchInfo += TestSessionLaunchInfo(testSuiteIdentifier, testCaseIdentifier, testSessionId, None)
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
    DB.run(q).flatMap { results =>
      startHeadlessTestSessions(results._1, results._2, results._3, results._4, Some(results._6), request.forceSequentialExecution).flatMap { _ =>
        if (request.waitForCompletion) {
          // Wait until the test sessions have completed.
          val sessionBuffer = new ListBuffer[TestSessionLaunchInfo]()
          sessionBuffer.addAll(results._5)
          val maximumWaitTime = request.maximumWaitTime.getOrElse(30.seconds.toMillis)
          val scheduler = actorSystem.scheduler
          val startTime = System.currentTimeMillis()
          after(getPollDelay(0, maximumWaitTime), scheduler)(checkTestSessionsUntilFinished(sessionBuffer, startTime, maximumWaitTime, scheduler)).map { sessionInfo =>
            sessionInfo.toList
          }
        } else {
          Future.successful(results._5)
        }
      }
    }
  }

  private def checkTestSessionsUntilFinished(sessions: ListBuffer[TestSessionLaunchInfo], startMillis: Long, maximumWait: Long, scheduler: Scheduler): Future[ListBuffer[TestSessionLaunchInfo]] = {
    LOGGER.debug("Checking test sessions for completion...")
    checkAllTestSessionsFinished(sessions).flatMap { sessionInfo =>
      if (!sessionInfo.exists(_.completed.getOrElse(false) == false)) {
        // All test sessions completed.
        Future.successful(sessionInfo)
      } else {
        val elapsedMillis = System.currentTimeMillis() - startMillis
        if (elapsedMillis > maximumWait) {
          // Not all sessions completed in the allotted time.
          Future.successful(sessionInfo)
        } else {
          // Schedule another check after a brief delay. If the delay exceeds the maximum wait time then try one last time at the maximum wait time.
          val pollDelay = getPollDelay(elapsedMillis, maximumWait)
          after(pollDelay, scheduler)(checkTestSessionsUntilFinished(sessionInfo, startMillis, maximumWait, scheduler))
        }
      }
    }
  }

  private def getPollDelay(elapsedMillis: Long, maximumWait: Long): FiniteDuration = {
    var pollDelay = 5000L // Delay of 5 seconds by default
    val millisToGoBeforeTimeout = maximumWait - elapsedMillis
    if (millisToGoBeforeTimeout < pollDelay) {
      // If we are set to stop polling before the next 5 seconds elapse, poll at the timeout time.
      pollDelay = millisToGoBeforeTimeout
    }
    LOGGER.debug("Polling in {} milliseconds ({} elapsed and {} until timeout)", pollDelay, elapsedMillis, millisToGoBeforeTimeout)
    FiniteDuration(pollDelay, TimeUnit.MILLISECONDS)
  }

  // Helper function for scheduling a Future after a delay
  private def after[T](delay: FiniteDuration, scheduler: Scheduler)(future: => Future[T]): Future[T] = {
    val promise = scala.concurrent.Promise[T]()
    scheduler.scheduleOnce(delay)(promise.completeWith(future))
    promise.future
  }

  private def checkAllTestSessionsFinished(sessions: ListBuffer[TestSessionLaunchInfo]): Future[ListBuffer[TestSessionLaunchInfo]] = {
    // Check only the session IDs that are not yet completed.
    val pendingSessionIds = sessions.filter(_.completed.getOrElse(false) != true).map(_.testSessionIdentifier)
    // Query the DB for those sessions that are newly completed.
    DB.run(
      PersistenceSchema.testResults
        .filter(_.testSessionId inSet pendingSessionIds)
        .filter(_.endTime.isDefined)
        .map(x => x.testSessionId)
        .result
    ).map { completedSessionIds =>
      // Update the results to flag the newly completed sessions as such.
      LOGGER.debug("Completed sessions: {}", completedSessionIds)
      val completedSessionSet = completedSessionIds.toSet
      sessions.zipWithIndex.foreach { entry =>
        if (completedSessionSet.contains(entry._1.testSessionIdentifier)) {
          sessions(entry._2) = entry._1.withCompletedFlag(true)
        } else if (entry._1.completed.isEmpty) {
          sessions(entry._2) = entry._1.withCompletedFlag(false)
        }
      }
      sessions
    }
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

  def processAutomationStopRequest(organisationKey: String, sessionIds: List[String]): Future[Unit] = {
    for {
      sessionData <- {
        DB.run(
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
      }
      _ <- {
        // Signal stop to session manager (for tests that haven't started yet)
        getSessionManagerActor().tell(TerminateSessionsEvent(sessionData._1, sessionIds.toSet), ActorRef.noSender)
        // Signal stop to test engine (for already tests that are already running).
        Future.sequence {
          sessionData._2.map { sessionId =>
            testbedClient.stop(sessionId)
          }
        }
      }
    } yield ()
  }

  def finishTestReport(sessionId: String, status: TestResultType, outputMessage: Option[String]): Future[Unit] = {
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
    } yield startTime.isDefined
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).flatMap { sessionWasRecorded =>
      if (sessionWasRecorded) {
        // Triggers linked to test sessions: (communityID, systemID, actorID)
        DB.run(
          PersistenceSchema.testResults
            .filter(_.testSessionId === sessionId)
            .map(x => (x.communityId, x.sutId, x.actorId))
            .result
            .headOption
        ).flatMap { sessionIds =>
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
            conformanceManager.getCompletedConformanceStatementsForTestSession(systemId, sessionId).map { completedActors =>
              completedActors.foreach { actorId =>
                triggerHelper.publishTriggerEvent(new ConformanceStatementSucceededEvent(communityId, systemId, actorId))
              }
            }
          }
          // Flush remaining log messages
          testResultManager.flushSessionLogs(sessionId, None)
        }
      } else {
        Future.successful(())
      }
    }
  }

  private def setEndTimeNow(sessionId: String): Future[Unit] = {
    val now = Some(TimeUtil.getCurrentTimestamp())
    DB.run (
      (for {
        testSession <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
        _ <- {
          if (testSession.isDefined) {
            for {
              _ <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(_.endTime).update(now)
              _ <- PersistenceSchema.conformanceResults.filter(_.testsession === sessionId).map(c => (c.result, c.updateTime)).update(testSession.get.result, now)
            } yield ()
          } else {
            DBIO.successful(())
          }
        }
      } yield ()
      ).transactionally
    )
  }

}
