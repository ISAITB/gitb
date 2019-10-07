package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, UserManager}
import models.Constants
import models.Enums.UserRole
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller
import utils.JsonUtil

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService @Inject() (userManager: UserManager, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[UserService])

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators() = AuthorizedAction { request =>
    authorizationManager.canViewTestBedAdministrators(request)
    val list = userManager.getSystemAdministrators()
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets community administrator users
   */
  def getCommunityAdministrators() = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewCommunityAdministrators(request, communityId)

    val list = userManager.getCommunityAdministrators(communityId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewOrganisationUsers(request, orgId)
    val list = userManager.getUsersByOrganization(orgId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the user with specified id
   */
  def getUserById(userId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewUser(request, userId)
    val user = userManager.getUserById(userId)
    val json: String = JsonUtil.serializeUser(user)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new system administrator
   */
  def createSystemAdmin = AuthorizedAction { request =>
    authorizationManager.canCreateTestBedAdministrator(request)
    val user = ParameterExtractor.extractSystemAdminInfo(request)
    userManager.createAdmin(user, Constants.DefaultCommunityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Creates new community administrator
    */
  def createCommunityAdmin = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canCreateCommunityAdministrator(request, communityId)
    val user = ParameterExtractor.extractCommunityAdminInfo(request)
    userManager.createAdmin(user, communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Creates new vendor user/admin
   */
  def createUser(orgId: Long) = AuthorizedAction { request =>
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
  def updateSystemAdminProfile(userId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateTestBedAdministrator(request)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateSystemAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Updates community admin profile
    */
  def updateCommunityAdminProfile(userId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateCommunityAdministrator(request, userId)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateCommunityAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Updates user profile
   */
  def updateUserProfile(userId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateOrganisationUser(request, userId)
    val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
    var name: Option[String] = None
    if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
      name = Some(ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME))
    }
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateUserProfile(userId, name, roleId, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Deletes system administrator with specified id
   */
  def deleteAdmin(userId: Long) = AuthorizedAction { request =>
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
  def deleteVendorUser(userId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteOrganisationUser(request, userId)
    val isLastAdmin = userManager.isLastAdmin(userId)
    val authUserId = ParameterExtractor.extractUserId(request)
    if (isLastAdmin) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the only administrator of the organization.")
    } else if (authUserId == userId) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
    } else if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == userId) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the configured demo account.")
    } else {
      userManager.deleteUser(userId)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
