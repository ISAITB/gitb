package controllers

import actors.WebSocketActor
import akka.actor.ActorSystem
import com.gitb.core.{ActorConfiguration, Configuration, ValueEmbeddingEnumeration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import javax.inject.{Inject, Singleton}
import managers._
import models.Constants
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.codec.binary.Base64
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{ClamAVClient, JacksonUtil, JsonUtil, MimeUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class TestService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, reportManager: ReportManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, organisationManager: OrganizationManager, systemManager: SystemManager, testbedClient: managers.TestbedBackendClient, actorSystem: ActorSystem, webSocketActor: WebSocketActor) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestService])

  def getTestCasePresentation(testId:String, sessionId: Option[String]): GetTestCaseDefinitionResponse = {
    val request = new GetTestCaseDefinitionRequest
    request.setTcId(testId)
    if (sessionId.isDefined) {
      request.setTcInstanceId(sessionId.get)
    }
    testbedClient.service().getTestCaseDefinition(request)
  }

  def endSession(session_id:String) = {
    val request: BasicCommand = new BasicCommand
    request.setTcInstanceId(session_id)
    testbedClient.service().stop(request)

    reportManager.setEndTimeNow(session_id)
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinition(test_id:String) = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, test_id)
    val response = getTestCasePresentation(test_id, None)
    val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions() = authorizedAction { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canViewActorsBySpecificationId(request, specId)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(specId))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String) = authorizedAction { request =>
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
  def configure(sessionId:String) = authorizedAction { request =>
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
    import scala.collection.JavaConverters._
    cRequest.getConfigs.addAll(asJavaCollection(statementParameters))
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

  private def scanForVirus(inputs: List[com.gitb.core.AnyContent], scanner: ClamAVClient): Boolean = {
    var found = false
    if (inputs != null && inputs.nonEmpty) {
      inputs.foreach { input =>
        if (input.getValue != null && input.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64) {
          val scanResult = scanner.scan(Base64.decodeBase64(MimeUtil.getBase64FromDataURL(input.getValue)))
          if (!ClamAVClient.isCleanReply(scanResult)) {
            found = true
          }
        } else if (input.getItem != null && !input.getItem.isEmpty) {
          import scala.collection.JavaConverters._
          found = scanForVirus(collectionAsScalaIterable(input.getItem).toList, scanner)
        }
        if (found) {
          return found
        }
      }
    }
    found
  }

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(session_id:String) = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val inputs = ParameterExtractor.requiredBodyParameter(request, Parameters.INPUTS)
    val step   = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_STEP)
    val userInputs = JsonUtil.parseJsUserInputs(inputs)

    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      // Check for viruses in the uploaded file(s)
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      if (scanForVirus(userInputs, virusScanner)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Provided file failed virus scan.")
      }
    }
    if (response == null) {
      val pRequest: ProvideInputRequest = new ProvideInputRequest
      pRequest.setTcInstanceId(session_id)
      pRequest.setStepId(step)
      import scala.collection.JavaConverters._
      pRequest.getInput.addAll(asJavaCollection(userInputs))

      testbedClient.service().provideInput(pRequest)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String) = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val bRequest:BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    testbedClient.service().initiatePreliminary(bRequest)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(sessionId:String) = authorizedAction { request =>
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
  def stop(session_id:String) = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    endSession(session_id)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String) = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    testbedClient.service().restart(bRequest)
    ResponseConstructor.constructEmptyResponse
  }

  def startHeadlessTestSessions() = authorizedAction { request =>
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
