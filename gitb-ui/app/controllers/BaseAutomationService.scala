package controllers

import com.fasterxml.jackson.core.JsonParseException
import controllers.util.{RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request, Result}

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

  protected def processAsJson(request: RequestWithAttributes[AnyContent], authorisationFn: RequestWithAttributes[AnyContent] => _, processFn: JsValue => Result): Result = {
    authorisationFn(request)
    var result: Result = null
    try {
      val json = bodyToJson(request)
      if (json.isEmpty) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
      } else {
        result = processFn(json.get)
      }
    } catch {
      case e: JsonParseException =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
        LOG.warn("Failed to parse automation API payload: "+e.getMessage)
      case e: AutomationApiException =>
        result = ResponseConstructor.constructBadRequestResponse(e.getCode, e.getMessage)
        LOG.warn("Failure while processing automation API call: "+e.getMessage)
      case e: MissingRequiredParameterException =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_REQUIRED_CONFIGURATION, e.getMessage)
        LOG.warn(e.getMessage)
      case e: Exception =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Provided JSON payload was not defined as expected")
        LOG.warn("Unexpected error while processing automation API call: "+e.getMessage)
    }
    result
  }

}
