/*
 * Copyright (C) 2026 European Union
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

import config.Configurations
import controllers.rest.BaseAutomationService.EndpointSignature
import controllers.util.{ParameterExtractor, RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException, UnacceptableUriException, UnauthorizedAccessException}
import managers.ratelimit.RateLimitManager
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc._
import tools.jackson.core.JacksonException

import scala.concurrent.{ExecutionContext, Future}

object BaseAutomationService {

  trait EndpointSignature {
    def path(): String
    def method(): String
  }
  case class GetEndpoint(path: String) extends EndpointSignature { override def method(): String = "get" }
  case class PostEndpoint(path: String) extends EndpointSignature { override def method(): String = "post" }
  case class PutEndpoint(path: String) extends EndpointSignature { override def method(): String = "put" }
  case class DeleteEndpoint(path: String) extends EndpointSignature { override def method(): String = "delete" }

}

abstract class BaseAutomationService(protected val cc: ControllerComponents,
                                     protected val rateLimitManager: RateLimitManager)
                                    (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val LOG = LoggerFactory.getLogger(classOf[BaseAutomationService])

  private def bodyToJson(request: Request[AnyContent]): Option[JsValue] = {
    var result: Option[JsValue] = try {
      request.body.asJson
    } catch {
      case _: Throwable => None
    }
    if (result.isEmpty) {
      val text = request.body.asText
      if (text.isDefined) {
        try {
          result = Some(Json.parse(text.get))
        } catch {
          case e: Exception =>
            val detail = if (e.getMessage != null && !e.getMessage.isBlank) s": ${e.getMessage}" else ""
            throw AutomationApiException(ErrorCodes.INVALID_REQUEST, s"Failed to parse provided payload as JSON$detail")
        }
      }
    }
    result
  }

  protected def process(request: RequestWithAttributes[AnyContent], signature: EndpointSignature, authorisationFn: () => Future[_], processFn: Any => Future[Result]): Future[Result] = {
    (for {
      _ <- authorisationFn.apply()
      rateResult <- checkRateLimit(request, signature)
      result <- {
        rateResult match {
          case Some(errorResponse) => Future.successful(errorResponse)
          case None => processFn.apply(())
        }
      }
    } yield result).recover {
      handleException(_)
    }
  }

  protected def processAsJson(request: RequestWithAttributes[AnyContent], signature: EndpointSignature, authorisationFn: () => Future[_], processFn: JsValue => Future[Result]): Future[Result] = {
    processAsJsonWithAuth[Any](request, signature, authorisationFn, (_, json) => processFn(json))
  }

  /**
   * Same as processAsJson but passing the result of the authorisation check (e.g. information resolved while
   * authorising) to the processing function. If emptyBodyAsJsonObject is set a completely missing body is processed as
   * an empty JSON object ("{}"), otherwise it is considered invalid.
   */
  protected def processAsJsonWithAuth[A](request: RequestWithAttributes[AnyContent], signature: EndpointSignature, authorisationFn: () => Future[A], processFn: (A, JsValue) => Future[Result], emptyBodyAsJsonObject: Boolean = false): Future[Result] = {
    (for {
      authResult <- authorisationFn.apply()
      rateResult <- checkRateLimit(request, signature)
      result <- {
        rateResult match {
          case Some(errorResponse) => Future.successful(errorResponse)
          case None =>
            val json = bodyToJson(request).orElse(if (emptyBodyAsJsonObject && isBodyMissing(request)) Some(Json.obj()) else None)
            if (json.isEmpty) {
              Future.successful {
                ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
              }
            } else {
              processFn(authResult, json.get)
            }
        }
      }
    } yield result).recover {
      handleException(_)
    }
  }

  private def isBodyMissing(request: Request[AnyContent]): Boolean = {
    request.body match {
      case AnyContentAsEmpty => true
      case AnyContentAsText(text) => text.isBlank
      case AnyContentAsRaw(raw) => raw.size == 0
      case _ => false
    }
  }

  protected def handleException(cause: Throwable): Result = {
    cause match {
      case e: JacksonException =>
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
      case e: UnacceptableUriException =>
        LOG.warn("Unacceptable URI in automation API call: " + e.getMessage)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, e.getMessage)
      case e: UnauthorizedAccessException =>
        ResponseConstructor.constructAccessDeniedResponse(403, e.msg)
      case e: Throwable =>
        LOG.warn("Unexpected error while processing automation API call: " + e.getMessage, e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected error occurred while processing your request")
    }
  }

  protected def checkRateLimit(request: RequestWithAttributes[AnyContent], signature: EndpointSignature): Future[Option[Result]] = {
    ParameterExtractor.extractApiKeyHeader(request) match {
      case Some(apiKey) =>
        Future.successful {
          rateLimitManager.tryConsume(apiKey, signature.path(), signature.method()) match {
            case Some(retryAfter) =>
              // Not allowed.
              Some(ResponseConstructor.constructTooManyRequestsResponse(ErrorCodes.API_RATE_LIMIT_ENFORCED, "Too many requests made for this endpoint for the provided API key", retryAfter))
            case None =>
              // All OK.
              None
          }
        }
      case None =>
        throw AutomationApiException(ErrorCodes.API_KEY_MISSING, "Missing API key header (%s)".formatted(Configurations.HEADER_NAME_ITB_API_KEY))
    }
  }

}
