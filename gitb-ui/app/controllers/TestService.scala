package controllers

import actors.SessionManagerActor
import actors.events.sessions.{PrepareTestSessionsEvent, TerminateAllSessionsEvent, TestSessionLaunchData}
import akka.actor.{ActorRef, ActorSystem}
import com.gitb.core.{ActorConfiguration, Configuration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import managers._
import models.prerequisites.PrerequisiteUtil
import models.{Constants, Organizations}
import org.apache.commons.io.FileUtils
import play.api.mvc._
import utils._

import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}

@Singleton
class TestService @Inject() (repositoryUtils: RepositoryUtils, authorizedAction: AuthorizedAction, cc: ControllerComponents, reportManager: ReportManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, organisationManager: OrganizationManager, systemManager: SystemManager, testbedClient: managers.TestbedBackendClient, actorSystem: ActorSystem, testResultManager: TestResultManager) extends AbstractController(cc) {

  private var sessionManagerActor: Option[ActorRef] = None

  def getTestCasePresentation(testId:String, sessionId: Option[String]): GetTestCaseDefinitionResponse = {
    testbedClient.getTestCaseDefinition(testId, sessionId)
  }

  def endSession(session:String): Unit = {
    testbedClient.stop(session)
    reportManager.setEndTimeNow(session)
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinition(test_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, test_id)
    val response = getTestCasePresentation(test_id, None)
    val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions: Action[AnyContent] = authorizedAction { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canViewActorsBySpecificationId(request, specId)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(specId))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestCase(request, test_id)
    ResponseConstructor.constructStringResponse(testbedClient.initiate(test_id.toLong))
  }

  /**
   * Sends the required data on preliminary steps
   */
  def configure(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    val organisationData = loadOrganisationParameters(systemId)
    val response = testbedClient.configure(
      sessionId,
      loadConformanceStatementParameters(systemId, actorId),
      loadDomainParameters(specId),
      organisationData._2,
      loadSystemParameters(systemId)
    )
    val json = JacksonUtil.serializeConfigureResponse(response)
    ResponseConstructor.constructJsonResponse(json)
  }

  private def loadConformanceStatementParameters(systemId: Long, actorId: Long) = {
    conformanceManager.getSystemConfigurationParameters(systemId, actorId)
  }

  private def loadDomainParameters(specId: Long): Option[ActorConfiguration] = {
    val domainId = conformanceManager.getSpecifications(Some(List(specId))).head.domain
    val parameters = conformanceManager.getDomainParameters(domainId, loadValues = true, Some(true))
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

  private def loadOrganisationParameters(systemId: Long): (Organizations, ActorConfiguration) = {
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
          throw new IllegalStateException("Missing required parameter")
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

  private def loadSystemParameters(systemId: Long): ActorConfiguration = {
    val system = systemManager.getSystemById(systemId).get
    val systemConfiguration = new ActorConfiguration()
    systemConfiguration.setActor(Constants.systemConfigurationName)
    systemConfiguration.setEndpoint(Constants.systemConfigurationName)
    addConfig(systemConfiguration, Constants.systemConfiguration_fullName, system.fullname)
    addConfig(systemConfiguration, Constants.systemConfiguration_shortName, system.shortname)
    addConfig(systemConfiguration, Constants.systemConfiguration_version, system.version)
    val systemProperties = PrerequisiteUtil.withValidPrerequisites(systemManager.getSystemParameterValues(systemId))
    if (systemProperties.nonEmpty) {
      systemProperties.foreach{ property =>
        if (property.parameter.use == "R" && property.value.isEmpty) {
          throw new IllegalStateException("Missing required parameter")
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

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(session_id:String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canExecuteTestSession(request, session_id)
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      var response: Result = null
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        // Check for viruses in the uploaded file(s)
        val scanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        files.foreach { file =>
          if (response == null) {
            val scanResult = scanner.scan(file._2.file)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Provided file failed virus scan.")
            }
          }
        }
      }
      if (response == null) {
        val inputs = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.INPUTS)
        val step   = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TEST_STEP)
        val userInputs = JsonUtil.parseJsUserInputs(inputs)
        // Set files to inputs.
        userInputs.foreach { userInput =>
          if (userInput.getValue == null && files.contains(s"file_${userInput.getId}")) {
            val fileInfo = files(s"file_${userInput.getId}")
            userInput.setValue(MimeUtil.getFileAsDataURL(fileInfo.file, fileInfo.contentType.orNull))
          }
        }
        testbedClient.provideInput(session_id, step, Some(userInputs))
        response = ResponseConstructor.constructEmptyResponse
      }
      response
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    testbedClient.initiatePreliminary(session_id)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    testbedClient.start(sessionId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Stops the test case
   */
  def stop(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    endSession(session_id)
    ResponseConstructor.constructEmptyResponse
  }

  def stopAll(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageAnyTestSession(request)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, None, None))
    testResultManager.getAllRunningSessions().foreach { sessionId =>
      endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllCommunitySessions(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(Some(communityId), None, None))
    testResultManager.getRunningSessionsForCommunity(communityId).foreach { sessionId =>
      endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllOrganisationSessions(organisationId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageOrganisationBasic(request, organisationId)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, Some(organisationId), None))
    testResultManager.getRunningSessionsForOrganisation(organisationId).foreach { sessionId =>
      endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    testbedClient.restart(session_id)
    ResponseConstructor.constructEmptyResponse
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

  def startHeadlessTestSessions(): Action[AnyContent] = authorizedAction { request =>
    val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
    val specId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    if (testCaseIds.isDefined && testCaseIds.get.nonEmpty) {
      authorizationManager.canExecuteTestCases(request, testCaseIds.get, specId, systemId, actorId)
      // Load information common to all test sessions
      val statementParameters = loadConformanceStatementParameters(systemId, actorId)
      val domainParameters = loadDomainParameters(specId)
      val organisationData = loadOrganisationParameters(systemId)
      val systemParameters = loadSystemParameters(systemId)
      // Schedule test sessions
      val forceSequentialExecution = false
      val launchInfo = PrepareTestSessionsEvent(TestSessionLaunchData(
        organisationData._1.community, organisationData._1.id, systemId, actorId, testCaseIds.get,
        statementParameters, domainParameters, organisationData._2, systemParameters,
        None, forceSequentialExecution))
      getSessionManagerActor().tell(launchInfo, ActorRef.noSender)
    }
    ResponseConstructor.constructEmptyResponse
  }

}
