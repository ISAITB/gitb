package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions._
import javax.inject.Inject
import managers.AuthorizationManager
import org.slf4j.{Logger, LoggerFactory}
import persistence.cache.TokenCache
import persistence.{AccountManager, AuthenticationManager}
import play.api.mvc._

class AuthenticationService @Inject() (accountManager: AccountManager, authManager: AuthenticationManager, authorizationManager: AuthorizationManager) extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  private final val BEARER = "Bearer"

  /**
    * OAuth2.0 request (Resource Owner Password Credentials Grant) for getting or refreshing access token
    */
  def access_token = AuthorizedAction { request =>
    authorizationManager.canLogin(request)
    val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
    val passwd = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)

    val result = authManager.checkUserByEmail(email, passwd)
    //user found
    if (result.isDefined) {
      val tokens = authManager.generateTokens(result.get.id)
      ResponseConstructor.constructOauthResponse(tokens)
    }
    //no user with given credentials
    else {
      throw InvalidAuthorizationException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
    }
  }

  /**
    * Check email availability
    */
  def checkEmail = AuthorizedAction { request =>
    authorizationManager.canCheckUserEmail(request)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Logout the specific user.
    *
    * @return
    */
  def logout = AuthorizedAction { request =>
    authorizationManager.canLogout(request)
    val authzHeader = request.headers.get(AUTHORIZATION)
    if (authzHeader.isDefined){
      val list = authzHeader.get.split(BEARER + " ")
      if(list.length == 2) {
        val accessToken = list(1)
        TokenCache.deleteOAthToken(accessToken)
      }
    }
    ResponseConstructor.constructEmptyResponse
  }
}
