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
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationService @Inject() (authorizedAction: AuthorizedAction,
                                       cc: ControllerComponents,
                                       accountManager: AccountManager,
                                       authManager: AuthenticationManager,
                                       authorizationManager: AuthorizationManager,
                                       userManager: UserManager,
                                       repositoryUtils: RepositoryUtils)
                                      (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationService])
  private final val BEARER = "Bearer"

  def getUserFunctionalAccounts: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewUserFunctionalAccounts(request).flatMap { _ =>
      authorizationManager.getAccountInfo(request).map { accountInfo =>
        val json: String = JsonUtil.jsActualUserInfo(accountInfo).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getUserUnlinkedFunctionalAccounts: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewUserFunctionalAccounts(request).flatMap { _ =>
      authorizationManager.getPrincipal(request).flatMap { accountInfo =>
        accountManager.getUnlinkedUserAccountsForEmail(accountInfo.email).map { userAccounts =>
          val json: String = JsonUtil.jsUserAccounts(userAccounts).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def linkFunctionalAccount: Action[AnyContent] = authorizedAction.async { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canLinkFunctionalAccount(request, userId).flatMap { _ =>
      authorizationManager.getPrincipal(request).flatMap { principal =>
        accountManager.linkAccount(userId, principal).flatMap { _ =>
          // Return new account info
          authorizationManager.getAccountInfo(request).map { accountInfo =>
            val json: String = JsonUtil.jsActualUserInfo(accountInfo).toString
            ResponseConstructor.constructJsonResponse(json)
          }
        }
      }
    }
  }

  def migrateFunctionalAccount: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canMigrateAccount(request).flatMap { _ =>
      val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL).trim
      val password = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD).trim
      authManager.checkUserByEmail(email, password).flatMap { result =>
        if (result.isDefined) {
          if (result.get.ssoUid.isDefined || result.get.ssoEmail.isDefined) {
            // User already migrated.
            Future.successful {
              ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials match an already migrated account.", Some("username"))
            }
          } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == result.get.id) {
            // Attempt to migrate the demo account. Return message as if the user doesn't exist.
            authorizationManager.getPrincipal(request).map { principal =>
              logger.warn("Attempt made by ["+principal.uid+"] to migrate the demo account ["+Configurations.DEMOS_ACCOUNT+"]")
              ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account.", Some("username"))
            }
          } else {
            // Link the account.
            authorizationManager.getPrincipal(request).flatMap { principal =>
              accountManager.migrateAccount(result.get.id, principal).flatMap { _ =>
                authorizationManager.getAccountInfo(request).map { accountInfo =>
                  val json: String = JsonUtil.jsActualUserInfo(accountInfo).toString
                  ResponseConstructor.constructJsonResponse(json)
                }
              }
            }
          }
        } else {
          // User not found
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided credentials did not match a previously existing account.", Some("username"))
          }
        }
      }
    }
  }

  def selectFunctionalAccount: Action[AnyContent] = authorizedAction.async { request =>
    val userId = ParameterExtractor.requiredBodyParameter(request, Parameters.ID).toLong
    authorizationManager.canSelectFunctionalAccount(request, userId).map { _ =>
      completeAccessTokenLogin(userId)
    }
  }

  def disconnectFunctionalAccount: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDisconnectFunctionalAccount(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      authorizationManager.getPrincipal(request).flatMap { userInfo =>
        val option = ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toShort
        val action = if (option == 1) {
          // Current partial.
          accountManager.disconnectAccount(userId)
        } else if (option == 2) {
          // Current full.
          userManager.deleteUserExceptTestBedAdmin(userId)
        } else if (option == 3) {
          // All.
          userManager.deleteUsersByUidExceptTestBedAdmin(userInfo.uid, userInfo.email)
        } else {
          Future.successful(true)
        }
        action.map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  private def completeAccessTokenLogin(userId: Long): Result = {
    val tokens = authManager.generateTokens(userId)
    disableDataBootstrap()
    ResponseConstructor.constructOauthResponse(tokens, userId)
  }

  def replaceOnetimePassword: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canLogin(request).flatMap { _ =>
      val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
      val newPassword = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)
      if (CryptoUtil.isAcceptedPassword(newPassword)) {
        val oldPassword = ParameterExtractor.requiredBodyParameter(request, Parameters.OLD_PASSWORD)
        authManager.replaceOnetimePassword(email, newPassword, oldPassword).map { result =>
          if (result.isEmpty) {
            ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Incorrect password.", Some("current"))
          } else {
            completeAccessTokenLogin(result.get)
          }
        }
      } else {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.", Some("new"))
        }
      }
    }
  }

  /**
    * OAuth2.0 request (Resource Owner Password Credentials Grant) for getting or refreshing access token
    */
  def access_token: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canLogin(request).flatMap { _ =>
      val email = ParameterExtractor.requiredBodyParameter(request, Parameters.EMAIL)
      val passwd = ParameterExtractor.requiredBodyParameter(request, Parameters.PASSWORD)
      authManager.checkUserByEmail(email, passwd).map { result =>
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
    }
  }

  /**
    * Check email availability
    */
  def checkEmail: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckAnyUserEmail(request).flatMap { _ =>
      val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
      authManager.checkEmailAvailability(email, None, None, Some(Enums.UserRole.VendorUser.id.toShort)).map { isAvailable =>
        ResponseConstructor.constructAvailabilityResponse(isAvailable)
      }
    }
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationMember(): Action[AnyContent] = authorizedAction.async { request =>
    val userId = ParameterExtractor.extractUserId(request)
    accountManager.getUserProfile(userId).flatMap { userInfo =>
      authorizationManager.canCheckUserEmail(request, userInfo.organization.get.id).flatMap { _ =>
        val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
        val roleId = if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          Some(ParameterExtractor.requiredQueryParameter(request, Parameters.ROLE_ID).toShort)
        } else {
          None
        }
        authManager.checkEmailAvailability(email, Some(userInfo.organization.get.id), None, roleId).map { isAvailable =>
          ResponseConstructor.constructAvailabilityResponse(isAvailable)
        }
      }
    }
  }

  /**
    * Check email availability
    */
  def checkEmailOfSystemAdmin: Action[AnyContent] = authorizedAction.async { request =>
    val userId = ParameterExtractor.extractUserId(request)
    accountManager.getUserProfile(userId).flatMap { userInfo =>
      authorizationManager.canCheckSystemAdminEmail(request).flatMap { _ =>
        val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
        authManager.checkEmailAvailability(email, Some(userInfo.organization.get.id), None, None).map { isAvailable =>
          ResponseConstructor.constructAvailabilityResponse(isAvailable)
        }
      }
    }
  }

  /**
    * Check email availability
    */
  def checkEmailOfCommunityAdmin: Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canCheckCommunityAdminEmail(request, communityId).flatMap { _ =>
      val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
      authManager.checkEmailAvailability(email, None, Some(communityId), None).map { isAvailable =>
        ResponseConstructor.constructAvailabilityResponse(isAvailable)
      }
    }
  }

  /**
    * Check email availability
    */
  def checkEmailOfOrganisationUser(): Action[AnyContent] = authorizedAction.async { request =>
    val organisationId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    authorizationManager.canCheckOrganisationUserEmail(request, organisationId).flatMap { _ =>
      val email = ParameterExtractor.requiredQueryParameter(request, Parameters.EMAIL)
      val roleId = ParameterExtractor.requiredQueryParameter(request, Parameters.ROLE_ID).toShort
      authManager.checkEmailAvailability(email, Some(organisationId), None, Some(roleId)).map { isAvailable =>
        ResponseConstructor.constructAvailabilityResponse(isAvailable)
      }
    }
  }

  /**
    * Logout the specific user.
    *
    * @return
    */
  def logout: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canLogout(request).map { _ =>
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

  private def disableDataBootstrap() = {
    if (Configurations.DATA_WEB_INIT_ENABLED) {
      repositoryUtils.createDataLockFile()
    }
  }
}
