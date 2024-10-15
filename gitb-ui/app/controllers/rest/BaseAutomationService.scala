package controllers.rest

import com.fasterxml.jackson.core.JsonParseException
import controllers.util.{RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc._

abstract class BaseAutomationService(protected val cc: ControllerComponents) extends AbstractController(cc) {

  private val LOG = LoggerFactory.getLogger(classOf[BaseAutomationService])

  private def bodyToJson(request: Request[AnyContent]): Option[JsValue] = {
    var result: Option[JsValue] = request.body.asJson
    if (result.isEmpty) {
      val text = request.body.asText
      if (text.isDefined) {
        result = Some(Json.parse(text.get))
      }
    }
    result
  }

  protected def process(request: RequestWithAttributes[AnyContent], authorisationFn: Option[RequestWithAttributes[AnyContent] => _], processFn: Any => Result): Result = {
    if (authorisationFn.isDefined) {
      authorisationFn.get.apply(request)
    }
    var result: Result = null
    try {
      result = processFn(())
    } catch {
      case e: Throwable => result = handleException(e)
    }
    result
  }

  protected def processAsJson(request: RequestWithAttributes[AnyContent], authorisationFn: Option[RequestWithAttributes[AnyContent] => _], processFn: JsValue => Result): Result = {
    if (authorisationFn.isDefined) {
      authorisationFn.get.apply(request)
    }
    var result: Result = null
    try {
      val json = bodyToJson(request)
      if (json.isEmpty) {
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
      } else {
        result = processFn(json.get)
      }
    } catch {
      case e: Throwable => result = handleException(e)
    }
    result
  }

  protected def handleException(cause: Throwable): Result = {
    cause match {
      case e: JsonParseException =>
        LOG.warn("Failed to parse automation API payload: "+e.getMessage)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
      case e: JsResultException =>
        LOG.warn("Failure while parsing JSON payload: "+e.getMessage)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Provided JSON payload was not valid for the requested operation")
      case e: MissingRequiredParameterException =>
        LOG.warn(e.getMessage)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_REQUIRED_CONFIGURATION, e.getMessage)
      case e: AutomationApiException =>
        LOG.warn("Failure while processing automation API call: " + e.getMessage)
        ResponseConstructor.constructBadRequestResponse(e.getCode, e.getMessage)
      case e: Throwable =>
        LOG.warn("Unexpected error while processing automation API call: " + e.getMessage, e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected error occurred while processing your request")
    }
  }

}
