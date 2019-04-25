package controllers

import actors.WebSocketActor
import com.gitb.core.{ActorConfiguration, Configuration}
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import javax.inject.{Inject, Singleton}
import jaxws.HeaderHandlerResolver
import managers.{ConformanceManager, ReportManager}
import models.Constants
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil}

@Singleton
class TestService @Inject() (reportManager: ReportManager, conformanceManager: ConformanceManager) extends Controller {

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
  def getTestCaseDefinition(test_id:String) = Action.apply {
    val response = getTestCasePresentation(test_id)
    val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
    logger.debug("[TestCase] " + json)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions() = Action.apply { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(specId))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String) = Action.apply {
    val request: BasicRequest = new BasicRequest
    request.setTcId(test_id)

    val response = port().initiate(request)
    ResponseConstructor.constructStringResponse(response.getTcInstanceId)
  }
  /**
   * Sends the required data on preliminary steps
   */
  def configure(session_id:String) = Action.apply { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    val configs = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIGS)

    val cRequest: ConfigureRequest = new ConfigureRequest
    cRequest.setTcInstanceId(session_id)
    cRequest.getConfigs.addAll(JacksonUtil.parseActorConfigurations(configs))

    val domainId = conformanceManager.getSpecifications(Some(List(specId)))(0).domain
    val parameters = conformanceManager.getDomainParameters(domainId)
    if (!parameters.isEmpty) {
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

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(session_id:String) = Action.apply { request =>
    val inputs = ParameterExtractor.requiredBodyParameter(request, Parameters.INPUTS)
    val step   = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_STEP)

    val pRequest: ProvideInputRequest = new ProvideInputRequest
    pRequest.setTcInstanceId(session_id)
    pRequest.setStepId(step)
    pRequest.getInput.addAll(JacksonUtil.parseUserInputs(inputs))

    val response = port().provideInput(pRequest)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String) = Action.apply { request =>
    val bRequest:BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().initiatePreliminary(bRequest)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(session_id:String) = Action.apply {
    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().start(bRequest)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Stops the test case
   */
  def stop(session_id:String) = Action.apply {
    endSession(session_id)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String) = Action.apply {
    val bRequest: BasicCommand = new BasicCommand
    bRequest.setTcInstanceId(session_id)

    port().restart(bRequest)
    ResponseConstructor.constructEmptyResponse
  }

}
