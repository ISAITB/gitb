package controllers

import config.Configurations
import controllers.util._
import exceptions._
import managers.{AccountManager, AuthenticationManager, AuthorizationManager, UserManager}
import models.Enums
import org.slf4j.{Logger, LoggerFactory}
import persistence.cache.TokenCache
import play.api.mvc._
import utils.{CryptoUtil, JsonUtil, RepositoryUtils}

import javax.inject.Inject

class AuthenticationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, accountManager: AccountManager, authManager: AuthenticationManager, authorizationManager: AuthorizationManager, userManager: UserManager, repositoryUtils: RepositoryUtils) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  private final val BEARER = "Bearer"

  def getUserFunctionalAccounts = authorizedAction { request =>
    authorizationManager.canViewUserFunctionalAccounts(request)
    val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getUserUnlinkedFunctionalAccounts = authorizedAction { request =>
    authorizationManager.canViewUserFunctionalAccounts(request)
    val accountInfo = authorizationManager.getPrincipal(request)
    val userAccounts = accountManager.getUnlinkedUserAccountsForEmail(accountInfo.email)
    val json: String = JsonUtil.jsUserAccounts(userAccounts).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def linkFunctionalAccount = authorizedAction { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canLinkFunctionalAccount(request, userId)
    accountManager.linkAccount(userId, authorizationManager.getPrincipal(request))
    // Return new account info
    val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def migrateFunctionalAccount = authorizedAction { request =>
    authorizationManager.canMigrateAccount(request)
    val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL).trim
    val password = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD).trim
    val result = authManager.checkUserByEmail(email, password)
    if (result.isDefined) {
      if (result.get.ssoUid.isDefined || result.get.ssoEmail.isDefined) {
        // User already migrated.
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials match an already migrated account.", Some("username"))
      } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == result.get.id) {
        // Attempt to migrate the demo account. Return message as if the user doesn't exist.
        logger.warn("Attempt made by ["+authorizationManager.getPrincipal(request).uid+"] to migrate the demo account ["+Configurations.DEMOS_ACCOUNT+"]")
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account.", Some("username"))
      } else {
        // Link the account.
        accountManager.migrateAccount(result.get.id, authorizationManager.getPrincipal(request))
        val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    } else {
      // User not found
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account.", Some("username"))
    }
  }

  def selectFunctionalAccount = authorizedAction { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canSelectFunctionalAccount(request, userId)
    completeAccessTokenLogin(userId)
  }

  def disconnectFunctionalAccount = authorizedAction { request =>
    authorizationManager.canDisconnectFunctionalAccount(request)
    val userId = ParameterExtractor.extractUserId(request)
    val userInfo = authorizationManager.getPrincipal(request)
    val option = ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toShort
    if (option == 1) {
      // Current partial.
      accountManager.disconnectAccount(userId, userInfo.uid)
    } else if (option == 2) {
      // Current full.
      userManager.deleteUserExceptTestBedAdmin(userId)
    } else if (option == 3) {
      // All.
      userManager.deleteUsersByUidExceptTestBedAdmin(userInfo.uid, userInfo.email)
    }
    ResponseConstructor.constructEmptyResponse
  }

  private def completeAccessTokenLogin(userId: Long): Result = {
    val tokens = authManager.generateTokens(userId)
    disableDataBootstrap()
    ResponseConstructor.constructOauthResponse(tokens)
  }

  def replaceOnetimePassword = authorizedAction { request =>
    authorizationManager.canLogin(request)
    val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
    val newPassword = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)
    if (CryptoUtil.isAcceptedPassword(newPassword)) {
      val oldPassword = ParameterExtractor.requiredBodyParameter(request, Parameters.OLD_PASSWORD)
      val result = authManager.replaceOnetimePassword(email, newPassword, oldPassword)
      if (result.isEmpty) {
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Incorrect password.", Some("current"))
      } else {
        completeAccessTokenLogin(result.get)
      }
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.", Some("new"))
    }
  }

  /**
    * OAuth2.0 request (Resource Owner Password Credentials Grant) for getting or refreshing access token
    */
  def access_token = authorizedAction { request =>
    authorizationManager.canLogin(request)
    val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
    val passwd = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)
    val result = authManager.checkUserByEmail(email, passwd)
    // User found
    if (result.isDefined) {
      if (result.get.onetimePassword) {
        // Onetime password needs to be replaced first.
        Ok("{\"onetime\": true}").as(JSON)
      } else if (!CryptoUtil.isAcceptedPassword(passwd)) {
        Ok("{\"weakPassword\": true}").as(JSON)
      } else {
        // All ok.
        completeAccessTokenLogin(result.get.id)
      }
    } else {
      // No user with given credentials
      throw InvalidAuthorizationException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
    }
  }

  /**
    * Check email availability
    */
  def checkEmail = authorizedAction { request =>
    authorizationManager.canCheckAnyUserEmail(request)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, None, None, Some(Enums.UserRole.VendorUser.id.toShort))
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationMember = authorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    val userInfo = accountManager.getUserProfile(userId)
    authorizationManager.canCheckUserEmail(request, userInfo.organization.get.id)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val roleId = if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      Some(ParameterExtractor.requiredQueryParameter(request, Parameters.ROLE_ID).toShort)
    } else {
      None
    }
    val isAvailable = authManager.checkEmailAvailability(email, Some(userInfo.organization.get.id), None, roleId)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfSystemAdmin = authorizedAction { request =>
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
  def checkEmailOfCommunityAdmin = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canCheckCommunityAdminEmail(request, communityId)
    val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
    val isAvailable = authManager.checkEmailAvailability(email, None, Some(communityId), None)
    ResponseConstructor.constructAvailabilityResponse(isAvailable)
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationUser = authorizedAction { request =>
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
  def logout = authorizedAction { request =>
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

  private def disableDataBootstrap() = {
    if (Configurations.DATA_WEB_INIT_ENABLED) {
      repositoryUtils.createDataLockFile()
    }
  }
}
