package controllers.util

import config.Configurations
import exceptions.ErrorCodes
import models.Token
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import play.api.mvc._

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

  def constructErrorMessage(errorCode: Any, errorDesc:String, errorIdentifier: Option[String]): String = {
    val code = {
      var code = "\"\""
      if(errorCode.isInstanceOf[Int])
        code =  "" + errorCode
      else if (errorCode.isInstanceOf[String])
        code = "\"" + errorCode + "\""
      code
    }
    var msg = "{\"error_code\":" + code + ", \"error_description\":\"" + errorDesc + "\""
    if (errorIdentifier.isDefined) {
      msg += ", \"error_id\": \""+errorIdentifier.get+"\""
    }
    msg += "}"
    msg
  }

  def constructErrorResponse(errorCode: Int, errorDesc: String):Result = {
    Ok(constructErrorMessage(errorCode, errorDesc, None)).as(JSON)
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

  def constructOauthResponse(tokens:Token):Result = {
    Ok("{" +
      "\"path\":\"" + Configurations.AUTHENTICATION_COOKIE_PATH + "\"," +
      "\"access_token\":\"" + tokens.access_token + "\"," +
      "\"token_type\":\"Bearer\"," +
      "\"expires_in\":" + Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME + "," + //in seconds ~ 30 days
      "\"registered\":true}").as(JSON)
  }

}
