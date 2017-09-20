package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.{OrganizationManager, UserManager}
import models.Enums.UserRole
import org.slf4j.{Logger, LoggerFactory}
import utils.JsonUtil
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import exceptions.{ErrorCodes, InvalidAuthorizationException, NotFoundException}

import scala.concurrent.Future
import org.mindrot.jbcrypt.BCrypt

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[UserService])

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators() = Action.async {
    UserManager.getSystemAdministrators() map { list =>
      val json: String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets community administrator users
   */
  def getCommunityAdministrators() = Action.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    UserManager.getCommunityAdministrators(communityId) map { list =>
      val json: String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long) = Action.async {
    UserManager.getUsersByOrganization(orgId) map { list =>
      val json: String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets the user with specified id
   */
  def getUserById(userId: Long) = Action.async { request =>
    UserManager.getUserById(userId) map { user =>
      val json: String = JsonUtil.serializeUser(user)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Creates new system administrator
   */
  def createSystemAdmin = Action.async { request =>
    val user = ParameterExtractor.extractSystemAdminInfo(request)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    UserManager.createAdmin(user, communityId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
    * Creates new community administrator
    */
  def createCommunityAdmin = Action.async { request =>
    val user = ParameterExtractor.extractCommunityAdminInfo(request)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    UserManager.createAdmin(user, communityId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Creates new vendor user/admin
   */
  def createUser(orgId: Long) = Action.async { request =>
    val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
    val user = UserRole(roleId) match {
      case UserRole.VendorUser => ParameterExtractor.extractUserInfo(request)
      case UserRole.VendorAdmin => ParameterExtractor.extractAdminInfo(request)
      case _ =>  throw new IllegalArgumentException("Cannot create user with role " + roleId)
    }
    UserManager.createUser(user, orgId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Updates system admin profile
   */
  def updateSystemAdminProfile(userId: Long) = Action.async { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    UserManager.updateSystemAdminProfile(userId, name) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
    * Updates community admin profile
    */
  def updateCommunityAdminProfile(userId: Long) = Action.async { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
    UserManager.updateCommunityAdminProfile(userId, name) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Updates user profile
   */
  def updateUserProfile(userId: Long) = Action.async { request =>
    UserManager.isLastAdmin(userId) map { isLastAdmin =>
      val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
      val name = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_NAME)
      if (isLastAdmin && UserRole(roleId) == UserRole.VendorUser) {
        ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the only administrator of the organization.")
      } else {
        UserManager.updateUserProfile(userId, name, roleId)
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Deletes system administrator with specified id
   */
  def deleteAdmin(userId: Long) = Action.async { request =>
    val authUserId = ParameterExtractor.extractUserId(request)
    if (authUserId == userId) {
      Future {
        ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete your own account.")
      }
    } else {
      UserManager.deleteUser(userId) map { unit =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Deletes vendor user/admin with specified id
   */
  def deleteVendorUser(userId: Long) = Action.async { request =>
    UserManager.isLastAdmin(userId) map { isLastAdmin =>
      if (isLastAdmin) {
        ResponseConstructor.constructErrorResponse(ErrorCodes.CANNOT_DELETE, "Cannot delete the only administrator of the organization.")
      } else {
        UserManager.deleteUser(userId)
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

}
