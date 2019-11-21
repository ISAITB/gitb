package controllers

import com.gitb.core.{ActorConfiguration, Configuration, ValueEmbeddingEnumeration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import javax.inject.{Inject, Singleton}
import jaxws.HeaderHandlerResolver
import managers._
import models.Constants
import org.apache.commons.codec.binary.Base64
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{ClamAVClient, JacksonUtil, JsonUtil, MimeUtil}

@Singleton
class TestService @Inject() (reportManager: ReportManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, organisationManager: OrganizationManager, systemManager: SystemManager) extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestService])

  private var portInternal: TestbedService = null

  private def port() = {
    if (portInternal == null) {
      logger.info("Creating TestbedService client")
      val backendURL: java.net.URL = new java.net.URL(Configurations.TESTBED_SERVICE_URL+"?wsdl")
      val service: TestbedService_Service = new TestbedService_Service(backendURL)
      //add header handler resolver to add custom header element for TestbedClient service address
      val handlerResolver = new HeaderHandlerResolver()
      service.setHandlerResolver(handlerResolver)

      val port = service.getTestbedServicePort()
      portInternal = port
    }
    portInternal
  }

  def getTestCasePresentation(testId:String): GetTestCaseDefinitionResponse = {
    System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|192.168.*.*")
    val request:BasicRequest = new BasicRequest
    request.setTcId(testId)

    port().getTestCaseDefinition(request)
  }

  def endSession(session_id:String) = {
    val request: BasicCommand = new BasicCommand
    request.setTcInstanceId(session_id)
    port().stop(request)

    reportManager.setEndTimeNow(session_id)
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinition(test_id:String) = AuthorizedAction { request =>
    authorizationManager.canViewTestCase(request, test_id)
    val response = getTestCasePresentation(test_id)
    val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
    logger.debug("[TestCase] " + json)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions() = AuthorizedAction { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canViewActorsBySpecificationId(request, specId)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(specId))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestCase(request, test_id)
    val requestData: BasicRequest = new BasicRequest
    requestData.setTcId(test_id)

    val response = port().initiate(requestData)
    ResponseConstructor.constructStringResponse(response.getTcInstanceId)
  }
  /**
   * Sends the required data on preliminary steps
   */
  def configure(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong

    val cRequest: ConfigureRequest = new ConfigureRequest
    cRequest.setTcInstanceId(session_id)

    // Load system configuration parameters.
    val systemParameters = conformanceManager.getSystemConfigurationParameters(systemId, actorId)
    import scala.collection.JavaConversions._
    cRequest.getConfigs.addAll(systemParameters)
    // Load domain configuration parameters.
    val domainId = conformanceManager.getSpecifications(Some(List(specId))).head.domain
    val parameters = conformanceManager.getDomainParameters(domainId)
    if (parameters.nonEmpty) {
      val domainConfiguration = new ActorConfiguration()
      domainConfiguration.setActor(Constants.domainConfigurationName)
      domainConfiguration.setEndpoint(Constants.domainConfigurationName)
      parameters.foreach { parameter =>
        addConfig(domainConfiguration, parameter.name, parameter.value.get)
      }
      cRequest.getConfigs.add(domainConfiguration)
    }
    // Load organisation parameters.
    val organisation = organisationManager.getOrganizationBySystemId(systemId)
    val organisationConfiguration = new ActorConfiguration()
    organisationConfiguration.setActor(Constants.organisationConfigurationName)
    organisationConfiguration.setEndpoint(Constants.organisationConfigurationName)
    addConfig(organisationConfiguration, Constants.organisationConfiguration_fullName, organisation.fullname)
    addConfig(organisationConfiguration, Constants.organisationConfiguration_shortName, organisation.shortname)
    val organisationProperties = organisationManager.getOrganisationParameterValues(organisation.id)
    if (organisationProperties.nonEmpty) {
      organisationProperties.foreach{ property =>
        if (property.parameter.use == "R" && property.value.isEmpty) {
          throw new IllegalStateException("Missing required parameter")
        }
        if (!property.parameter.notForTests && property.value.isDefined) {
          addConfig(organisationConfiguration, property.parameter.testKey, property.value.get.value)
        }
      }
    }
    cRequest.getConfigs.add(organisationConfiguration)
    // Load system parameters.
    val system = systemManager.getSystemById(systemId).get
    val systemConfiguration = new ActorConfiguration()
    systemConfiguration.setActor(Constants.systemConfigurationName)
    systemConfiguration.setEndpoint(Constants.systemConfigurationName)
    addConfig(systemConfiguration, Constants.systemConfiguration_fullName, system.fullname)
    addConfig(systemConfiguration, Constants.systemConfiguration_shortName, system.shortname)
    addConfig(systemConfiguration, Constants.systemConfiguration_version, system.version)
    val systemProperties = systemManager.getSystemParameterValues(systemId)
    if (systemProperties.nonEmpty) {
      systemProperties.foreach{ property =>
        if (property.parameter.use == "R" && property.value.isEmpty) {
          throw new IllegalStateException("Missing required parameter")
        }
        if (!property.parameter.notForTests && property.value.isDefined) {
          addConfig(systemConfiguration, property.parameter.testKey, property.value.get.value)
        }
      }
    }
    cRequest.getConfigs.add(systemConfiguration)

    val response = port().configure(cRequest)
    val json = JacksonUtil.serializeConfigureResponse(response)
    ResponseConstructor.constructJsonResponse(json)
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
          import scala.collection.JavaConversions._
          found = scanForVirus(input.getItem.toList, scanner)
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
  def provideInput(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val inputs = ParameterExtractor.requiredBodyParameter(request, Parameters.INPUTS)
    val step   = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_STEP)
    val userInputs = JacksonUtil.parseUserInputs(inputs)

    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      // Check for viruses in the uploaded file(s)
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      import scala.collection.JavaConversions._
      if (scanForVirus(userInputs.toList, virusScanner)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Provided file failed virus scan.")
      }
    }
    if (response == null) {
      val pRequest: ProvideInputRequest = new ProvideInputRequest
      pRequest.setTcInstanceId(session_id)
      pRequest.setStepId(step)
      pRequest.getInput.addAll(userInputs)

      port().provideInput(pRequest)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val bRequest:BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().initiatePreliminary(bRequest)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().start(bRequest)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Stops the test case
   */
  def stop(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    endSession(session_id)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String) = AuthorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)

    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().restart(bRequest)
    ResponseConstructor.constructEmptyResponse
  }

}
