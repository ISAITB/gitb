package managers

import com.gitb.tbs.{TestbedService, TestbedService_Service}
import config.Configurations
import javax.inject.Singleton
import jaxws.HeaderHandlerResolver
import org.slf4j.{Logger, LoggerFactory}

@Singleton
class TestbedBackendClient {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestbedBackendClient])

  private var portInternal: TestbedService = null

  def service():TestbedService = {
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

}
