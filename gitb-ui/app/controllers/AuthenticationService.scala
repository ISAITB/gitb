package controllers

import config.Configurations
import controllers.util._
import exceptions._
import javax.inject.Inject
import managers.AuthorizationManager
import models.Enums
import org.pac4j.play.store.PlaySessionStore
import org.slf4j.{Logger, LoggerFactory}
import persistence.cache.TokenCache
import persistence.{AccountManager, AuthenticationManager}
import play.api.mvc._
import utils.JsonUtil

class AuthenticationService @Inject() (accountManager: AccountManager, authManager: AuthenticationManager, authorizationManager: AuthorizationManager, playSessionStore: PlaySessionStore) extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  private final val BEARER = "Bearer"

  def getUserFunctionalAccounts = AuthorizedAction { request =>
    authorizationManager.canViewUserFunctionalAccounts(request)
    val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getUserUnlinkedFunctionalAccounts = AuthorizedAction { request =>
    authorizationManager.canViewUserFunctionalAccounts(request)
    val accountInfo = authorizationManager.getPrincipal(request)
    val userAccounts = accountManager.getUnlinkedUserAccountsForEmail(accountInfo.email)
    val json: String = JsonUtil.jsUserAccounts(userAccounts).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def linkFunctionalAccount = AuthorizedAction { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canLinkFunctionalAccount(request, userId)
    accountManager.linkAccount(userId, authorizationManager.getPrincipal(request))
    // Return new account info
    val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def migrateFunctionalAccount = AuthorizedAction { request =>
    authorizationManager.canMigrateAccount(request)
    val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
    val password = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)
    val result = authManager.checkUserByEmail(email, password)
    if (result.isDefined) {
      if (result.get.ssoUid.isDefined || result.get.ssoEmail.isDefined) {
        // User already migrated.
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials match an already migrated account")
      } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == result.get.id) {
        // Attempt to migrate the demo account. Return message as if the user doesn't exist.
        logger.warn("Attempt made by ["+authorizationManager.getPrincipal(request).uid+"] to migrate the demo account ["+Configurations.DEMOS_ACCOUNT+"]")
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account")
      } else {
        // Link the account.
        accountManager.migrateAccount(result.get.id, authorizationManager.getPrincipal(request))
        val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    } else {
      // User not found
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account")
    }
  }

  def selectFunctionalAccount = AuthorizedAction { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canSelectFunctionalAccount(request, userId)
    val tokens = authManager.generateTokens(userId)
    ResponseConstructor.constructOauthResponse(tokens)
  }

  def disconnectFunctionalAccount = AuthorizedAction { request =>
    authorizationManager.canDisconnectFunctionalAccount(request)
    val userId = ParameterExtractor.extractUserId(request)
    val uid = authorizationManager.getPrincipal(request).uid
    accountManager.disconnectAccount(userId, uid)
    ResponseConstructor.constructEmptyResponse
  }

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
    authorizationManager.canCheckAnyUserEmail(request)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, None, None, Some(Enums.UserRole.VendorUser.id.toShort))
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationMember = AuthorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    val userInfo = accountManager.getUserProfile(userId)
    authorizationManager.canCheckUserEmail(request, userInfo.organization.get.id)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, Some(userInfo.organization.get.id), None, None)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfSystemAdmin = AuthorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    val userInfo = accountManager.getUserProfile(userId)
    authorizationManager.canCheckSystemAdminEmail(request)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, Some(userInfo.organization.get.id), None, None)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfCommunityAdmin = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canCheckCommunityAdminEmail(request, communityId)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, None, Some(communityId), None)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationUser = AuthorizedAction { request =>
    val organisationId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    authorizationManager.canCheckOrganisationUserEmail(request, organisationId)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val roleId = ParameterExtractor.requiredQueryParameter(request, Parameters.ROLE_ID).toShort
    val isAvailable = authManager.checkEmailAvailability(email, Some(organisationId), None, Some(roleId))
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Logout the specific user.
    *
    * @return
    */
  def logout = AuthorizedAction { request =>
    authorizationManager.canLogout(request)
    val isFullLogout = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL).toBoolean
    val authzHeader = request.headers.get(AUTHORIZATION)
    if (authzHeader.isDefined){
      val list = authzHeader.get.split(BEARER + " ")
      if(list.length == 2) {
        val accessToken = list(1)
        TokenCache.deleteOAthToken(accessToken)
      }
    }
    if (isFullLogout) {
      ResponseConstructor.constructEmptyResponse.withNewSession
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }
}
