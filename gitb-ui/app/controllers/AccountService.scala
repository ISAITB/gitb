package controllers

import config.Configurations
import controllers.util._
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import play.api.mvc._
import utils.JsonUtil


class AccountService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AccountService])

  /**
   * Registers a vendor and an administrator for vendor to the system
   */
  def registerVendor = Action.apply { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    val admin = ParameterExtractor.extractAdminInfo(request)

    AccountManager.registerVendor(organization, admin)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Gets the company profile for the authenticated user
   */
  def getVendorProfile = Action.apply { request =>
    val userId = ParameterExtractor.extractUserId(request)

    val organization = AccountManager.getVendorProfile(userId)
    val json:String = JsonUtil.serializeOrganization(organization)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates the company profile
   */
  def updateVendorProfile = Action.apply { request =>
    val adminId = ParameterExtractor.extractUserId(request)
    val vendor_sname = ParameterExtractor.optionalBodyParameter(request, Parameters.VENDOR_SNAME)
    val vendor_fname = ParameterExtractor.optionalBodyParameter(request, Parameters.VENDOR_FNAME)

    AccountManager.updateVendorProfile(adminId, vendor_sname, vendor_fname)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Gets the all users for the vendor
   */
  def getVendorUsers = Action.apply { request =>
    val userId = ParameterExtractor.extractUserId(request)

    val list = AccountManager.getVendorUsers(userId)
    val json:String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * The authenticated admin registers new user for the organization
   */
  def registerUser = Action.apply { request =>
    val adminId = ParameterExtractor.extractUserId(request)
    val user = ParameterExtractor.extractUserInfo(request)

    AccountManager.registerUser(adminId, user)
    ResponseConstructor.constructEmptyResponse
  }
  /**
   * Returns the user profile of the authenticated user
   */
  def getUserProfile = Action.apply { request =>
    val userId = ParameterExtractor.extractUserId(request)

    val user = AccountManager.getUserProfile(userId)
    val json:String = JsonUtil.serializeUser(user)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Updates the user profile of the authenticated user
   */
  def updateUserProfile = Action.apply { request =>
    val userId:Long = ParameterExtractor.extractUserId(request)
    val name:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.USER_NAME)
    val passwd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    val oldPasswd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.OLD_PASSWORD)

    AccountManager.updateUserProfile(userId, name, passwd, oldPasswd)
    ResponseConstructor.constructEmptyResponse
  }

  def getConfiguration = Action.apply { request =>
    val configProperties = new java.util.HashMap[String, String]()
    configProperties.put("email.enabled", String.valueOf(Configurations.EMAIL_ENABLED))
    configProperties.put("survey.enabled", String.valueOf(Configurations.SURVEY_ENABLED))
    configProperties.put("survey.address", String.valueOf(Configurations.SURVEY_ADDRESS))
    val json = JsonUtil.serializeConfigurationProperties(configProperties)
    ResponseConstructor.constructJsonResponse(json.toString())
  }

  def submitFeedback = Action.apply { request =>
    val userId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_ID).toLong
    val userEmail:String = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_EMAIL)
    val messageTypeId: String = ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_TYPE_ID)
    val messageTypeDescription: String = ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_TYPE_DESCRIPTION)
    val messageContent: String = ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_CONTENT)

    if (Configurations.EMAIL_ENABLED) {
      AccountManager.submitFeedback(userId, userEmail, messageTypeId, messageTypeDescription, messageContent)
    }
    ResponseConstructor.constructEmptyResponse
  }
}
