package controllers

import com.gitb.tr.TestResultType
import config.Configurations
import actors.WebSocketActor
import jaxws.HeaderHandlerResolver
import managers.{ReportManager, ConformanceManager}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import com.gitb.tbs._
import scala.concurrent.Future
import controllers.util._
import utils.{JsonUtil, JacksonUtil}

object TestService{
  val port:TestbedService = {
    val backendURL: java.net.URL = new java.net.URL(Configurations.TESTBED_SERVICE_URL+"?wsdl");
    val service: TestbedService_Service = new TestbedService_Service(backendURL)
    //add header handler resolver to add custom header element for TestbedClient service address
    val handlerResolver = new HeaderHandlerResolver()
    service.setHandlerResolver(handlerResolver)

    val port = service.getTestbedServicePort()
    port
  }

  def getTestCasePresentation(testId:String): GetTestCaseDefinitionResponse = {
    System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|192.168.*.*")
    val request:BasicRequest = new BasicRequest
    request.setTcId(testId)

    TestService.port.getTestCaseDefinition(request)
  }

  def endSession(session_id:String): Future[Unit] = {
    Future {
      val request: BasicCommand = new BasicCommand
      request.setTcInstanceId(session_id)
      TestService.port.stop(request)

      ReportManager.setEndTimeNow(session_id)
    }
  }
}

class TestService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestService])

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinition(test_id:String) = Action.async {
    Future {
      val response = TestService.getTestCasePresentation(test_id)
      val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
      logger.debug("[TestCase] " + json)
      ResponseConstructor.constructJsonResponse(json)
    }
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinition(actor_id:String) = Action.async {
    ConformanceManager.getActorDefinition(actor_id.toLong) map { actor =>
    val json = JsonUtil.jsActor(actor).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

    /*  Used to get definition from TestbedService
    Future {
      val request: GetActorDefinitionRequest = new GetActorDefinitionRequest
      request.setActorId(actor_id)

      TestService.port.getActorDefinition(request)
    } map { response =>
      val json = JacksonUtil.serializeActorPresentation(response.getActor)
      logger.debug("[Actor] " + json)
      ResponseConstructor.constructJsonResponse(json)
    }
    */
  }
  /**
   * Initiates the test case
   */
  def initiate(test_id:String) = Action.async {
    Future {
      val request: BasicRequest = new BasicRequest
      request.setTcId(test_id)

      TestService.port.initiate(request)
    }  map { response =>
      ResponseConstructor.constructStringResponse(response.getTcInstanceId)
    }
  }
  /**
   * Sends the required data on preliminary steps
   */
  def configure(session_id:String) = Action.async { request =>
    val configs = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIGS)

    Future {
      val request: ConfigureRequest = new ConfigureRequest
      request.setTcInstanceId(session_id)
      request.getConfigs.addAll(JacksonUtil.parseActorConfigurations(configs))

      TestService.port.configure(request)
    } map { response =>
      val json = JacksonUtil.serializeConfigureResponse(response)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(session_id:String) = Action.async { request =>
    val inputs = ParameterExtractor.requiredBodyParameter(request, Parameters.INPUTS)
    val step   = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_STEP)

    Future {
      val request: ProvideInputRequest = new ProvideInputRequest
      request.setTcInstanceId(session_id)
      request.setStepId(step)
      request.getInput.addAll(JacksonUtil.parseUserInputs(inputs))

      TestService.port.provideInput(request)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String) = Action.async { request =>
    Future {
      val request:BasicCommand = new BasicCommand
      request.setTcInstanceId(session_id)

      TestService.port.initiatePreliminary(request)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }

  }
  /**
   * Starts the test case
   */
  def start(session_id:String) = Action.async {
    Future {
      val request: BasicCommand = new BasicCommand
      request.setTcInstanceId(session_id)

      TestService.port.start(request)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }
  /**
   * Stops the test case
   */
  def stop(session_id:String) = Action.async {
    TestService.endSession(session_id) map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }
  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String) = Action.async {
    Future {
      val request: BasicCommand = new BasicCommand
      request.setTcInstanceId(session_id)

      TestService.port.restart(request)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Returns all open interoperability testing sessions
   */
  def getSessions() = Action {
    ResponseConstructor.constructJsonResponse(WebSocketActor.getSessions)
  }
}
