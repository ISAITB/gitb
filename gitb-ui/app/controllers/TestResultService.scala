package controllers

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._

class TestResultService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestResultService])

  /**
   * Gets detailed report for the test instance
   */
  def getReportForTestInstance(sut_id:Long, test_instance:String) = Action {
    logger.info("/suts/" + sut_id + "/conformance/" + test_instance + "/report service")
    Ok("/suts/" + sut_id + "/conformance/" + test_instance + "/report service")
  }
}
