package controllers.util

import exceptions.ErrorCodes
import play.api.mvc._
import play.api.http.HeaderNames._
import play.api.http.MimeTypes._
import models.Token
import config.Configurations

object ResponseConstructor extends Results{

  def constructUnauthorizedResponse(errorCode: Int, errorDesc: String): Result = {
    Unauthorized(constructErrorMessage(errorCode, errorDesc))
      .withHeaders(WWW_AUTHENTICATE ->  "Bearer realm=\"GITB\"").as(JSON)
  }

  def constructBadRequestResponse(errorCode: Int, errorDesc: String): Result = {
    BadRequest(constructErrorMessage(errorCode, errorDesc)).as(JSON)
  }

  def constructNotFoundResponse(errorCode: Int, errorDesc: String):Result = {
    NotFound(constructErrorMessage(errorCode, errorDesc)).as(JSON)
  }

  def constructServerError(errorCode: String, errorDesc:String):Result = {
    InternalServerError(constructErrorMessage(errorCode, errorDesc)).as(JSON)
  }

  def constructTimeoutResponse:Result = {
    GatewayTimeout(constructErrorMessage(ErrorCodes.GATEWAY_TIMEOUT,
      "An operation could not be completed within " +
      Configurations.SERVER_REQUEST_TIMEOUT_IN_SECONDS + " seconds"))
  }

  def constructErrorMessage(errorCode: Any, errorDesc:String): String = {
    val code = {
      var code = "\"\""

      if(errorCode.isInstanceOf[Int])
        code =  "" + errorCode
      else if (errorCode.isInstanceOf[String])
        code = "\"" + errorCode + "\""

      code
    }

    "{" +
      "\"error_code\":" + code + ", " +
      "\"error_description\":\"" + errorDesc + "\"" +
    "}"
  }

  def constructErrorResponse(errorCode: Int, errorDesc: String):Result = {
    Ok(constructErrorMessage(errorCode, errorDesc)).as(JSON)
  }

  def constructEmptyResponse:Result = {
    Ok("")
  }

  def constructStringResponse(string:String): Result = {
    Ok(string)
  }

  def constructCssResponse(string:String): Result = {
    Ok(string).withHeaders(CONTENT_TYPE -> "text/css")
  }

  def constructJsonResponse(json:String):Result = {
    Ok(json).as(JSON)
  }

  def constructAvailabilityResponse(isAvailable:Boolean):Result = {
    Ok("{"+"\"available\":"+isAvailable+"}").as(JSON)
  }

  def constructOauthResponse(tokens:Token):Result = {
    Ok("{" +
      "\"access_token\":\"" + tokens.access_token + "\"," +
      "\"token_type\":\"Bearer\"," +
      "\"expires_in\":" + Configurations.TOKEN_LIFETIME_IN_SECONDS + "," + //in seconds ~ 30 days
      "\"refresh_token\":\"" + tokens.refresh_token + "\"," +
      "\"registered\":true}").as(JSON)
  }

}
