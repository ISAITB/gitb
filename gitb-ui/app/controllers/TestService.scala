package controllers

import actors.WebSocketActor
import akka.actor.ActorSystem
import com.gitb.core.{ActorConfiguration, Configuration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import managers._
import models.Constants
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{ClamAVClient, JacksonUtil, JsonUtil, MimeUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class TestService @Inject() (repositoryUtils: RepositoryUtils, authorizedAction: AuthorizedAction, cc: ControllerComponents, reportManager: ReportManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, organisationManager: OrganizationManager, systemManager: SystemManager, testbedClient: managers.TestbedBackendClient, actorSystem: ActorSystem, webSocketActor: WebSocketActor, testResultManager: TestResultManager) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestService])

  def getTestCasePresentation(testId:String, sessionId: Option[String]): GetTestCaseDefinitionResponse = {
    val request = new GetTestCaseDefinitionRequest
    request.setTcId(testId)
    if (sessionId.isDefined) {
      request.setTcInstanceId(sessionId.get)
    }
    testbedClient.service().getTestCaseDefinition(request)
  }

  def endSession(session:String): Unit = {
    val request: BasicCommand = new BasicCommand
    request.setTcInstanceId(session)
    testbedClient.service().stop(request)
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
    ResponseConstructor.constructStringResponse(initiateInternal(test_id.toLong))
  }

  private def initiateInternal(test_id: Long) = {
    val requestData: BasicRequest = new BasicRequest
    requestData.setTcId(test_id.toString)
    val response = testbedClient.service().initiate(requestData)
    response.getTcInstanceId
  }

  /**
   * Sends the required data on preliminary steps
   */
  def configure(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    val response = configureInternal(
      sessionId,
      loadConformanceStatementParameters(systemId, actorId),
      loadDomainParameters(specId),
      loadOrganisationParameters(systemId),
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

  private def loadOrganisationParameters(systemId: Long): ActorConfiguration = {
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
    organisationConfiguration
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

  private def configureInternal(sessionId: String, statementParameters: List[ActorConfiguration], domainParameters: Option[ActorConfiguration], organisationParameters: ActorConfiguration, systemParameters: ActorConfiguration) = {
    val cRequest: ConfigureRequest = new ConfigureRequest
    cRequest.setTcInstanceId(sessionId)
    import scala.jdk.CollectionConverters._
    cRequest.getConfigs.addAll(statementParameters.asJava)
    if (domainParameters.nonEmpty) {
      cRequest.getConfigs.add(domainParameters.get)
    }
    cRequest.getConfigs.add(organisationParameters)
    cRequest.getConfigs.add(systemParameters)
    val response = testbedClient.service().configure(cRequest)
    response
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
        val pRequest: ProvideInputRequest = new ProvideInputRequest
        pRequest.setTcInstanceId(session_id)
        pRequest.setStepId(step)
        import scala.jdk.CollectionConverters._
        pRequest.getInput.addAll(userInputs.asJava)
        testbedClient.service().provideInput(pRequest)
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

    val bRequest:BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    testbedClient.service().initiatePreliminary(bRequest)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    startInternal(sessionId)
    ResponseConstructor.constructEmptyResponse
  }

  private def startInternal(sessionId: String) = {
    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(sessionId)
    testbedClient.service().start(bRequest)
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
    testResultManager.getAllRunningSessions().foreach { sessionId =>
      endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllCommunitySessions(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    testResultManager.getRunningSessionsForCommunity(communityId).foreach { sessionId =>
      endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllOrganisationSessions(organisationId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageOrganisationBasic(request, organisationId)
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

    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    testbedClient.service().restart(bRequest)
    ResponseConstructor.constructEmptyResponse
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
      val organisationParameters = loadOrganisationParameters(systemId)
      val systemParameters = loadSystemParameters(systemId)
      // Schedule test sessions in sequence
      startHeadlessTestSession(testCaseIds.get, systemId, actorId, statementParameters, domainParameters, organisationParameters, systemParameters)
    }
    ResponseConstructor.constructEmptyResponse
  }

  private def startHeadlessTestSession(testCaseIds: List[Long], systemId: Long, actorId: Long, statementParameters: List[ActorConfiguration], domainParameters: Option[ActorConfiguration], organisationParameters: ActorConfiguration, systemParameters: ActorConfiguration): Unit = {
    // Schedule each test session with a delay of 1 seconds.
    akka.pattern.after(duration = 1.seconds, using = actorSystem.scheduler) {
      val testCaseId = testCaseIds.head
      val testSessionId = initiateInternal(testCaseId)
      webSocketActor.testSessionStarted(testSessionId)
      try {
        configureInternal(testSessionId, statementParameters, domainParameters, organisationParameters, systemParameters)
        // Preliminary step is skipped as this is a headless session. If input was expected during this step the test session will fail.
        val testCasePresentation = getTestCasePresentation(testCaseId.toString, Some(testSessionId))
        reportManager.createTestReport(testSessionId, systemId, testCaseId.toString, actorId, testCasePresentation.getTestcase)
        startInternal(testSessionId)
        Future.successful((testCaseIds.drop(1), systemId, actorId, statementParameters, domainParameters, organisationParameters, systemParameters))
      } catch {
        case e:Exception =>
          webSocketActor.testSessionEnded(testSessionId)
          Future.failed(e)
      }
    } onComplete {
      case Success(result) =>
        if (result._1.nonEmpty) {
          startHeadlessTestSession(result._1, result._2, result._3, result._4, result._5, result._6, result._7)
        }
      case Failure(e) =>
        logger.error("A headless session raised an uncaught error", e)
    }
  }

}
