package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.AuthorizationManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{JsonUtil, MimeUtil}

class TestResultService @Inject() (authorizationManager: AuthorizationManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestResultService])

  def getBinaryMetadata = AuthorizedAction { request =>
    authorizationManager.canGetBinaryFileMetadata(request)
    val data:String= ParameterExtractor.requiredBodyParameter(request, Parameters.DATA)
    val isBase64:Boolean = java.lang.Boolean.valueOf(ParameterExtractor.requiredBodyParameter(request, Parameters.IS_BASE64))
    val mimeType = MimeUtil.getMimeType(data, !isBase64)
    val extension = MimeUtil.getExtensionFromMimeType(mimeType)
    val json = JsonUtil.jsBinaryMetadata(mimeType, extension).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

}
