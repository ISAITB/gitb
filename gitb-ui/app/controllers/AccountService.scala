package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import controllers.util._
import persistence.AccountManager
import utils.JsonUtil


class AccountService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AccountService])

  /**
   * Registers a vendor and an administrator for vendor to the system
   */
  def registerVendor = Action.async { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    val admin = ParameterExtractor.extractAdminInfo(request)

    AccountManager.registerVendor(organization, admin) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }
  /**
   * Gets the company profile for the authenticated user
   */
  def getVendorProfile = Action.async { request =>
    val userId = ParameterExtractor.extractUserId(request)

    AccountManager.getVendorProfile(userId) map { organization =>
      val json:String = JsonUtil.serializeOrganization(organization)
      ResponseConstructor.constructJsonResponse(json)
    }
  }
  /**
   * Updates the company profile
   */
  def updateVendorProfile = Action.async { request =>
    val adminId = ParameterExtractor.extractUserId(request)
    val vendor_sname = ParameterExtractor.optionalBodyParameter(request, Parameters.VENDOR_SNAME)
    val vendor_fname = ParameterExtractor.optionalBodyParameter(request, Parameters.VENDOR_FNAME)

    AccountManager.updateVendorProfile(adminId, vendor_sname, vendor_fname) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }
  /**
   * Gets the all users for the vendor
   */
  def getVendorUsers = Action.async { request =>
    val userId = ParameterExtractor.extractUserId(request)

    AccountManager.getVendorUsers(userId) map { list =>
      val json:String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }
  /**
   * The authenticated admin registers new user for the organization
   */
  def registerUser = Action.async { request =>
    val adminId = ParameterExtractor.extractUserId(request)
    val user = ParameterExtractor.extractUserInfo(request)

    AccountManager.registerUser(adminId, user) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }
  /**
   * Returns the user profile of the authenticated user
   */
  def getUserProfile = Action.async { request =>
    val userId = ParameterExtractor.extractUserId(request)

    AccountManager.getUserProfile(userId) map { user =>
      val json:String = JsonUtil.serializeUser(user)
      ResponseConstructor.constructJsonResponse(json)
    }
  }
  /**
   * Updates the user profile of the authenticated user
   */
  def updateUserProfile = Action.async { request =>
    val userId:Long = ParameterExtractor.extractUserId(request)
    val name:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.USER_NAME)
    val passwd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    val oldPasswd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.OLD_PASSWORD)

    AccountManager.updateUserProfile(userId, name, passwd, oldPasswd) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

}
