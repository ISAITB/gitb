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

package controllers.util

import config.Configurations
import exceptions.ErrorCodes
import models.Token
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.mvc._
import utils.JsonUtil

object ResponseConstructor extends Results{

  def constructUnauthorizedResponse(errorCode: Int, errorDesc: String): Result = {
    Unauthorized(constructErrorMessage(errorCode, errorDesc, None))
      .withHeaders(WWW_AUTHENTICATE ->  "Bearer realm=\"GITB\"").as(JSON)
  }

  def constructAccessDeniedResponse(errorCode: Int, errorDesc: String): Result = {
    Forbidden(constructErrorMessage(errorCode, errorDesc, None)).as(JSON)
  }

  def constructBadRequestResponse(errorCode: Int, errorDesc: String): Result = {
    BadRequest(constructErrorMessage(errorCode, errorDesc, None)).as(JSON)
  }

  def constructNotFoundResponse(errorCode: Int, errorDesc: String):Result = {
    NotFound(constructErrorMessage(errorCode, errorDesc, None)).as(JSON)
  }

  def constructServerError(errorCode: String, errorDesc:String, errorIdentifier:Option[String]):Result = {
    InternalServerError(constructErrorMessage(errorCode, errorDesc, errorIdentifier)).as(JSON)
  }

  def constructTimeoutResponse:Result = {
    GatewayTimeout(constructErrorMessage(ErrorCodes.GATEWAY_TIMEOUT,
      "The server was unable to produce a timely response to your request. Please try again later.", None))
  }

  private def constructErrorMessage(errorCode: Any, errorDesc:String, errorIdentifier: Option[String], errorHint: Option[String] = None): String = {
    JsonUtil.constructErrorMessage(errorCode, errorDesc, errorIdentifier, errorHint).toString()
  }

  def constructErrorResponse(errorCode: Int, errorDesc: String, errorHint: Option[String] = None):Result = {
    Ok(constructErrorMessage(errorCode, errorDesc, None, errorHint)).as(JSON)
  }

  def constructEmptyResponse:Result = {
    Ok("")
  }

  def constructStringResponse(string:String): Result = {
    Ok(string)
  }

  def constructCssResponse(string:String): Result = {
    Ok(string).as(CSS)
  }

  def constructJsonResponse(json:String):Result = {
    Ok(json).as(JSON)
  }

  def constructAvailabilityResponse(isAvailable:Boolean):Result = {
    Ok("{"+"\"available\":"+isAvailable+"}").as(JSON)
  }

  def constructOauthResponse(tokens: Token, userId: Long):Result = {
    Ok("{" +
      "\"user_id\":"+userId+"," +
      "\"path\":\"" + Configurations.PUBLIC_CONTEXT_ROOT + "\"," +
      "\"access_token\":\"" + tokens.access_token + "\"," +
      "\"token_type\":\"Bearer\"," +
      "\"expires_in\":" + Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME + "," + //in seconds ~ 30 days
      "\"registered\":true" +
      "}"
    ).as(JSON)
  }

  def createRequestedUrlCookie(requestedUrl: String): Option[Cookie] = {
    var urlToUse = requestedUrl
    val appIndex = urlToUse.indexOf("/app")
    if (appIndex != -1) {
      urlToUse = urlToUse.substring(appIndex+4)
    }
    if (urlToUse.isBlank) {
      None
    } else {
      Some(Cookie("ITB_REQUESTED_URL", urlToUse, None, "/", None, Configurations.SESSION_COOKIE_SECURE, httpOnly = false, Some(Cookie.SameSite.Strict)))
    }
  }

}
