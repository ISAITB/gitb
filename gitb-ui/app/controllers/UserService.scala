package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.UserManager
import models.Enums.UserRole
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService @Inject() (userManager: UserManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[UserService])

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators() = Action.apply {
    val list = userManager.getSystemAdministrators()
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets community administrator users
   */
  def getCommunityAdministrators() = Action.apply { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val list = userManager.getCommunityAdministrators(communityId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long) = Action.apply {
    val list = userManager.getUsersByOrganization(orgId)
    val json: String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the user with specified id
   */
  def getUserById(userId: Long) = Action.apply { request =>
    val user = userManager.getUserById(userId)
    val json: String = JsonUtil.serializeUser(user)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new system administrator
   */
  def createSystemAdmin = Action.apply { request =>
    val user = ParameterExtractor.extractSystemAdminInfo(request)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    userManager.createAdmin(user, communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Creates new community administrator
    */
  def createCommunityAdmin = Action.apply { request =>
    val user = ParameterExtractor.extractCommunityAdminInfo(request)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    userManager.createAdmin(user, communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Creates new vendor user/admin
   */
  def createUser(orgId: Long) = Action.apply { request =>
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
  def updateSystemAdminProfile(userId: Long) = Action.apply { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateSystemAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Updates community admin profile
    */
  def updateCommunityAdminProfile(userId: Long) = Action.apply { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    userManager.updateCommunityAdminProfile(userId, name, password)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Updates user profile
   */
  def updateUserProfile(userId: Long) = Action.apply { request =>
    val isLastAdmin = userManager.isLastAdmin(userId)
    val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    val password = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    if (isLastAdmin && UserRole(roleId) == UserRole.VendorUser) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the only administrator of the organization.")
    } else {
      userManager.updateUserProfile(userId, name, roleId, password)
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes system administrator with specified id
   */
  def deleteAdmin(userId: Long) = Action.apply { request =>
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
  def deleteVendorUser(userId: Long) = Action.apply { request =>
    val isLastAdmin = userManager.isLastAdmin(userId)
    if (isLastAdmin) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the only administrator of the organization.")
    } else {
      userManager.deleteUser(userId)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
