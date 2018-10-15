package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{JsonUtil, MimeUtil}

class TestResultService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestResultService])

  /**
   * Gets detailed report for the test instance
   */
  def getReportForTestInstance(sut_id:Long, test_instance:String) = Action {
    logger.info("/suts/" + sut_id + "/conformance/" + test_instance + "/report service")
    Ok("/suts/" + sut_id + "/conformance/" + test_instance + "/report service")
  }

  def getBinaryMetadata = Action.apply { request =>
    val data:String= ParameterExtractor.requiredBodyParameter(request, Parameters.DATA)
    val isBase64:Boolean = java.lang.Boolean.valueOf(ParameterExtractor.requiredBodyParameter(request, Parameters.IS_BASE64))
    val mimeType = MimeUtil.getMimeType(data, !isBase64)
    val extension = MimeUtil.getExtensionFromMimeType(mimeType)
    val json = JsonUtil.jsBinaryMetadata(mimeType, extension).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

}
