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
import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, UserManager}
import models.Constants
import models.Enums.UserRole
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService @Inject() (authorizedAction: AuthorizedAction,
                             cc: ControllerComponents,
                             userManager: UserManager,
                             authorizationManager: AuthorizationManager)
                            (implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestBedAdministrators(request).flatMap { _ =>
      userManager.getSystemAdministrators().map { list =>
        val json: String = JsonUtil.jsUsers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets community administrator users
   */
  def getCommunityAdministrators(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, ParameterNames.COMMUNITY_ID).toLong
    authorizationManager.canViewCommunityAdministrators(request, communityId).flatMap { _ =>
      userManager.getCommunityAdministrators(communityId).map { list =>
        val json: String = JsonUtil.jsUsers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisationUsers(request, orgId).flatMap { _ =>
      userManager.getUsersByOrganization(orgId).map { list =>
        val json: String = JsonUtil.jsUsers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets users by organization
   */
  def getBasicUsersByOrganization(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisationUsers(request, orgId).flatMap { _ =>
      userManager.getUsersByOrganization(orgId, Some(UserRole.VendorUser)).map { list =>
        val json: String = JsonUtil.jsUsers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the user with specified id
   */
  def getUserById(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewUser(request, userId).flatMap { _ =>
      userManager.getUserByIdAsync(userId).map { user =>
        if (user.isDefined) {
          val json: String = JsonUtil.serializeUser(user.get)
          ResponseConstructor.constructJsonResponse(json)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Gets the user with specified id
   */
  def getOwnOrganisationUserById(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnOrganisationUser(request, userId).flatMap { _ =>
      val callingUserId = ParameterExtractor.extractUserId(request)
      userManager.getUserByIdInSameOrganisationAsUser(userId, callingUserId).map { user =>
        if (user.isDefined) {
          val json: String = JsonUtil.serializeUser(user.get)
          ResponseConstructor.constructJsonResponse(json)
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "User not found")
        }
      }
    }
  }

  /**
   * Creates new system administrator
   */
  def createSystemAdmin: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateTestBedAdministrator(request).flatMap { _ =>
      val user = ParameterExtractor.extractSystemAdminInfo(request)
      userManager.createAdmin(user, Constants.DefaultCommunityId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Creates new community administrator
    */
  def createCommunityAdmin: Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    authorizationManager.canCreateCommunityAdministrator(request, communityId).flatMap { _ =>
      val user = ParameterExtractor.extractCommunityAdminInfo(request)
      userManager.createAdmin(user, communityId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Creates new vendor user/admin
   */
  def createUser(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateOrganisationUser(request, orgId).flatMap { _ =>
      val roleId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ROLE_ID).toShort
      val user = UserRole(roleId) match {
        case UserRole.VendorUser => ParameterExtractor.extractUserInfo(request)
        case UserRole.VendorAdmin => ParameterExtractor.extractAdminInfo(request)
        case _ =>  throw new IllegalArgumentException("Cannot create user with role " + roleId)
      }
      userManager.createUser(user, orgId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Updates system admin profile
   */
  def updateSystemAdminProfile(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateTestBedAdministrator(request).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, ParameterNames.USER_NAME)
      val password = ParameterExtractor.optionalBodyParameter(request, ParameterNames.PASSWORD)
      userManager.updateSystemAdminProfile(userId, name, password).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Updates community admin profile
    */
  def updateCommunityAdminProfile(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateCommunityAdministrator(request, userId).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, ParameterNames.USER_NAME)
      val password = ParameterExtractor.optionalBodyParameter(request, ParameterNames.PASSWORD)
      userManager.updateCommunityAdminProfile(userId, name, password).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Updates user profile
   */
  def updateUserProfile(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateOrganisationUser(request, userId).flatMap { _ =>
      val roleId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ROLE_ID).toShort
      var name: Option[String] = None
      if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
        name = Some(ParameterExtractor.requiredBodyParameter(request, ParameterNames.USER_NAME))
      }
      if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == userId && roleId != UserRole.VendorUser.id) {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_UPDATE, "Cannot update the role of the configured demo account.")
        }
      } else {
        val password = ParameterExtractor.optionalBodyParameter(request, ParameterNames.PASSWORD)
        userManager.updateUserProfile(userId, name, roleId, password).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Deletes system administrator with specified id
   */
  def deleteAdmin(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteAdministrator(request, userId).flatMap { _ =>
      val authUserId = ParameterExtractor.extractUserId(request)
      if (authUserId == userId) {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
        }
      } else {
        userManager.deleteUser(userId).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Deletes vendor user/admin with specified id
   */
  def deleteVendorUser(userId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteOrganisationUser(request, userId).flatMap { _ =>
      val authUserId = ParameterExtractor.extractUserId(request)
      if (authUserId == userId) {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
        }
      } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == userId) {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the configured demo account.")
        }
      } else {
        userManager.deleteUser(userId).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

}
