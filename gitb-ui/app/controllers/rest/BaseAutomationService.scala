/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package controllers.rest

import com.fasterxml.jackson.core.JsonParseException
import controllers.util.{RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException, UnauthorizedAccessException}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseAutomationService(protected val cc: ControllerComponents)
                                    (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val LOG = LoggerFactory.getLogger(classOf[BaseAutomationService])

  private def bodyToJson(request: Request[AnyContent]): Option[JsValue] = {
    try {
      var result: Option[JsValue] = request.body.asJson
      if (result.isEmpty) {
        val text = request.body.asText
        if (text.isDefined) {
          result = Some(Json.parse(text.get))
        }
      }
      result
    } catch {
      case _: Throwable => None
    }
  }

  protected def process(authorisationFn: () => Future[_], processFn: Any => Future[Result]): Future[Result] = {
    (for {
      _ <- authorisationFn.apply()
      result <- {
        processFn.apply(())
      }
    } yield result).recover {
      handleException(_)
    }
  }

  protected def processAsJson(request: RequestWithAttributes[AnyContent], authorisationFn: () => Future[_], processFn: JsValue => Future[Result]): Future[Result] = {
    (for {
      _ <- authorisationFn.apply()
      result <- {
        val json = bodyToJson(request)
        if (json.isEmpty) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
          }
        } else {
          processFn(json.get)
        }
      }
    } yield result).recover {
      handleException(_)
    }
  }

  private def handleException(cause: Throwable): Result = {
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
      case e: UnauthorizedAccessException =>
        ResponseConstructor.constructAccessDeniedResponse(403, e.msg)
      case e: Throwable =>
        LOG.warn("Unexpected error while processing automation API call: " + e.getMessage, e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected error occurred while processing your request")
    }
  }

}
