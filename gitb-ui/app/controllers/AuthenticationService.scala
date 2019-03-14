package controllers

import play.api.mvc._
import org.slf4j.{Logger, LoggerFactory}
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import persistence.{AccountManager, AuthManager}
import play.api.libs.concurrent.Execution.Implicits._
import exceptions._
import javax.inject.Inject

class AuthenticationService @Inject() (accountManager: AccountManager, authManager: AuthManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  /**
   * OAuth2.0 request (Resource Owner Password Credentials Grant) for getting or refreshing access token
   */
  def access_token = Action.apply { request =>
    val grantType = ParameterExtractor.requiredBodyParameter(request, Parameters.GRANT_TYPE)
    grantType match {
      //if user wants access_token, check his credentials first, then give one if they are valid
      case Parameters.GRANT_TYPE_PASSWORD =>
        val email  = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
        val passwd = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)

        val result = authManager.checkUserByEmail(email, passwd)
        //user found
        if(result.isDefined){
          val tokens = authManager.generateTokens(result.get.id)
          ResponseConstructor.constructOauthResponse(tokens)
        }
        //no user with given credentials
        else{
          throw InvalidAuthorizationException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
        }

      //if user wants to refresh his access_token, check his refresh token first, then give new one
      case Parameters.REFRESH_TOKEN =>
        val refreshToken  = ParameterExtractor.requiredBodyParameter(request, Parameters.REFRESH_TOKEN)

        val tokens = authManager.refreshTokens(refreshToken)
        ResponseConstructor.constructOauthResponse(tokens)

      //Invalid grant type
      case _ =>
        throw InvalidRequestException(ErrorCodes.INVALID_REQUEST, "Unrecognized grant_type '" + grantType + "'")
    }
  }
  /**
   * Check email availability
   */
  def checkEmail = Action.apply { request =>
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }
}
