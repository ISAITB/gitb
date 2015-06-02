package controllers

import play.api.mvc._
import org.slf4j.{LoggerFactory, Logger}
import controllers.util.{ResponseConstructor, ParameterExtractor, Parameters}
import persistence.{AccountManager, AuthManager}
import play.api.libs.concurrent.Execution.Implicits._
import exceptions._

class AuthenticationService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  /**
   * OAuth2.0 request (Resource Owner Password Credentials Grant) for getting or refreshing access token
   */
  def access_token = Action.async { request =>
    val grantType = ParameterExtractor.requiredBodyParameter(request, Parameters.GRANT_TYPE)
    grantType match {
      //if user wants access_token, check his credentials first, then give one if they are valid
      case Parameters.GRANT_TYPE_PASSWORD =>
        val email  = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
        val passwd = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)

        AuthManager.checkUserByEmail(email, passwd) flatMap{ result =>
          //user found
          if(result.isDefined){
            AuthManager.generateTokens(result.get.id) map{ tokens =>
              ResponseConstructor.constructOauthResponse(tokens)
            }
          }
          //no user with given credentials
          else{
            throw InvalidAuthorizationException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
          }
        }

      //if user wants to refresh his access_token, check his refresh token first, then give new one
      case Parameters.REFRESH_TOKEN =>
        val refreshToken  = ParameterExtractor.requiredBodyParameter(request, Parameters.REFRESH_TOKEN)

        AuthManager.refreshTokens(refreshToken) map{ tokens =>
          ResponseConstructor.constructOauthResponse(tokens)
        }

      //Invalid grant type
      case _ =>
        throw InvalidRequestException(ErrorCodes.INVALID_REQUEST, "Unrecognized grant_type '" + grantType + "'")
    }
  }
  /**
   * Check email availability
   */
  def checkEmail = Action.async { request =>
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val future = AuthManager.checkEmailAvailability(email)
    future map { isAvailable =>
      ResponseConstructor.constructAvailabilityResponse(isAvailable)
    }
  }
}
