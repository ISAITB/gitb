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
      "An operation could not be completed within " +
      Configurations.SERVER_REQUEST_TIMEOUT_IN_SECONDS + " seconds", None))
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
      "\"path\":\"" + Configurations.AUTHENTICATION_COOKIE_PATH + "\"," +
      "\"access_token\":\"" + tokens.access_token + "\"," +
      "\"token_type\":\"Bearer\"," +
      "\"expires_in\":" + Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME + "," + //in seconds ~ 30 days
      "\"registered\":true" +
      "}"
    ).as(JSON)
  }

}
