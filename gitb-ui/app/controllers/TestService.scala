package controllers

import com.gitb.core.{ActorConfiguration, Configuration, ValueEmbeddingEnumeration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import javax.inject.{Inject, Singleton}
import jaxws.HeaderHandlerResolver
import managers.{AuthorizationManager, ConformanceManager, ReportManager}
import models.Constants
import org.apache.commons.codec.binary.Base64
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{ClamAVClient, JacksonUtil, JsonUtil, MimeUtil}

@Singleton
class TestService @Inject() (reportManager: ReportManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends Controller {

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
    val configs = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIGS)

    val cRequest: ConfigureRequest = new ConfigureRequest
    cRequest.setTcInstanceId(session_id)
    cRequest.getConfigs.addAll(JacksonUtil.parseActorConfigurations(configs))

    val domainId = conformanceManager.getSpecifications(Some(List(specId)))(0).domain
    val parameters = conformanceManager.getDomainParameters(domainId)
    if (parameters.nonEmpty) {
      val domainConfiguration = new ActorConfiguration()
      domainConfiguration.setActor(Constants.domainConfigurationName)
      domainConfiguration.setEndpoint(Constants.domainConfigurationName)
      parameters.foreach { parameter =>
        val config = new Configuration()
        config.setName(parameter.name)
        config.setValue(parameter.value.get)
        domainConfiguration.getConfig.add(config)
      }
      cRequest.getConfigs.add(domainConfiguration)
    }

    val response = port().configure(cRequest)
    val json = JacksonUtil.serializeConfigureResponse(response)
    ResponseConstructor.constructJsonResponse(json)
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
