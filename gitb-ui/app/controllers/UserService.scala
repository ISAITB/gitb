package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, UserManager}
import models.Constants
import models.Enums.UserRole
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.JsonUtil

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, userManager: UserManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators() = authorizedAction { request =>
    authorizationManager.canViewTestBedAdministrators(request)
    val list = userManager.getSystemAdministrators()
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets community administrator users
   */
  def getCommunityAdministrators() = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewCommunityAdministrators(request, communityId)

    val list = userManager.getCommunityAdministrators(communityId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisationUsers(request, orgId)
    val list = userManager.getUsersByOrganization(orgId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets users by organization
   */
  def getBasicUsersByOrganization(orgId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisationUsers(request, orgId)
    val list = userManager.getUsersByOrganization(orgId, Some(UserRole.VendorUser))
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the user with specified id
   */
  def getUserById(userId: Long) = authorizedAction { request =>
    authorizationManager.canViewUser(request, userId)
    val user = userManager.getUserById(userId)
    if (user.isDefined) {
      val json: String = JsonUtil.serializeUser(user.get)
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Gets the user with specified id
   */
  def getOwnOrganisationUserById(userId: Long) = authorizedAction { request =>
    authorizationManager.canViewOwnOrganisationUser(request, userId)
    val callingUserId = ParameterExtractor.extractUserId(request)
    val user = userManager.getUserByIdInSameOrganisationAsUser(userId, callingUserId)
    if (user.isDefined) {
      val json: String = JsonUtil.serializeUser(user.get)
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "User not found")
    }
  }

  /**
   * Creates new system administrator
   */
  def createSystemAdmin = authorizedAction { request =>
    authorizationManager.canCreateTestBedAdministrator(request)
    val user = ParameterExtractor.extractSystemAdminInfo(request)
    userManager.createAdmin(user, Constants.DefaultCommunityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Creates new community administrator
    */
  def createCommunityAdmin = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canCreateCommunityAdministrator(request, communityId)
    val user = ParameterExtractor.extractCommunityAdminInfo(request)
    userManager.createAdmin(user, communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Creates new vendor user/admin
   */
  def createUser(orgId: Long) = authorizedAction { request =>
    authorizationManager.canCreateOrganisationUser(request, orgId)

    val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
    val user = UserRole(roleId) match {
      case UserRole.VendorUser => ParameterExtractor.extractUserInfo(request)
      case UserRole.VendorAdmin => ParameterExtractor.extractAdminInfo(request)
      case _ =>  throw new IllegalArgumentException("Cannot create user with role " + roleId)
    }
    userManager.createUser(user, orgId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Updates system admin profile
   */
  def updateSystemAdminProfile(userId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateTestBedAdministrator(request)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateSystemAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Updates community admin profile
    */
  def updateCommunityAdminProfile(userId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateCommunityAdministrator(request, userId)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateCommunityAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Updates user profile
   */
  def updateUserProfile(userId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateOrganisationUser(request, userId)
    val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
    var name: Option[String] = None
    if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
      name = Some(ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME))
    }
    if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == userId && roleId != UserRole.VendorUser.id) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_UPDATE, "Cannot update the role of the configured demo account.")
    } else {
      val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
      userManager.updateUserProfile(userId, name, roleId, password)
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes system administrator with specified id
   */
  def deleteAdmin(userId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteAdministrator(request, userId)
    val authUserId = ParameterExtractor.extractUserId(request)
    if (authUserId == userId) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
    } else {
      userManager.deleteUser(userId)
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes vendor user/admin with specified id
   */
  def deleteVendorUser(userId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteOrganisationUser(request, userId)
    val authUserId = ParameterExtractor.extractUserId(request)
    if (authUserId == userId) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
    } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == userId) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the configured demo account.")
    } else {
      userManager.deleteUser(userId)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
