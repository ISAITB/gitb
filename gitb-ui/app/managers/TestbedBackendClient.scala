package managers

import com.gitb.core.AnyContent
import com.gitb.tbs._
import config.Configurations
import jaxws.HeaderHandlerResolver
import models.SessionConfigurationData
import org.slf4j.{Logger, LoggerFactory}

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestbedBackendClient @Inject() (implicit ec: ExecutionContext) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestbedBackendClient])

  private var portInternal: TestbedService = _

  private def service():TestbedService = {
    if (portInternal == null) {
      logger.info("Creating TestbedService client")
      val backendURL: java.net.URL = URI.create(Configurations.TESTBED_SERVICE_URL+"?wsdl").toURL
      val service: TestbedService_Service = new TestbedService_Service(backendURL)
      //add header handler resolver to add custom header element for TestbedClient service address
      val handlerResolver = new HeaderHandlerResolver()
      service.setHandlerResolver(handlerResolver)
      val port = service.getTestbedServicePort()
      portInternal = port
    }
    portInternal
  }

  def initiate(testCaseId: Long, sessionId: Option[String]): Future[String] = {
    Future {
      val requestData: InitiateRequest = new InitiateRequest
      requestData.setTcId(testCaseId.toString)
      if (sessionId.isDefined) {
        requestData.setTcInstanceId(sessionId.get)
      }
      val response = service().initiate(requestData)
      response.getTcInstanceId
    }
  }

  def configure(sessionId: String, configuration: SessionConfigurationData, inputs: Option[List[AnyContent]]): Future[Unit] = {
    Future {
      val cRequest: ConfigureRequest = new ConfigureRequest
      cRequest.setTcInstanceId(sessionId)
      configuration.apply(cRequest.getConfigs)
      if (inputs.isDefined) {
        import scala.jdk.CollectionConverters._
        cRequest.getInputs.addAll(inputs.get.asJava)
      }
      service().configure(cRequest)
    }
  }

  def restart(sessionId: String): Future[Unit] = {
    Future {
      val bRequest: BasicCommand = new BasicCommand
      bRequest.setTcInstanceId(sessionId)
      service().restart(bRequest)
    }
  }

  def stop(sessionId: String): Future[Unit] = {
    Future {
      val request: BasicCommand = new BasicCommand
      request.setTcInstanceId(sessionId)
      service().stop(request)
    }
  }

  def start(sessionId: String): Future[Unit] = {
    logger.info("Starting test session [{}]", sessionId)
    Future {
      val bRequest: BasicCommand = new BasicCommand
      bRequest.setTcInstanceId(sessionId)
      service().start(bRequest)
    }
  }

  def provideInput(sessionId: String, stepId: String, userInputs: Option[List[UserInput]], isAdmin: Boolean): Future[Unit] = {
    Future {
      val pRequest: ProvideInputRequest = new ProvideInputRequest
      pRequest.setTcInstanceId(sessionId)
      pRequest.setStepId(stepId)
      pRequest.setAdmin(isAdmin)
      if (userInputs.nonEmpty) {
        // User inputs are empty when this is a headless session
        import scala.jdk.CollectionConverters._
        pRequest.getInput.addAll(userInputs.get.asJava)
      }
      service().provideInput(pRequest)
    }
  }

  def getTestCaseDefinition(testId:String, sessionId: Option[String], configuration: SessionConfigurationData): Future[GetTestCaseDefinitionResponse] = {
    Future {
      val request = new GetTestCaseDefinitionRequest
      request.setTcId(testId)
      if (sessionId.isDefined) {
        request.setTcInstanceId(sessionId.get)
      }
      configuration.apply(request.getConfigs)
      service().getTestCaseDefinition(request)
    }
  }

  def initiatePreliminary(sessionId: String): Future[Unit] = {
    Future {
      val bRequest:BasicCommand = new BasicCommand
      bRequest.setTcInstanceId(sessionId)
      service().initiatePreliminary(bRequest)
    }
  }

  def healthCheck(healthCheckType: String): Future[String] = {
    val request = new GetActorDefinitionRequest()
    request.setActorId(healthCheckType)
    Future {
      service().getActorDefinition(request).getActor.getDesc
    }
  }

}
