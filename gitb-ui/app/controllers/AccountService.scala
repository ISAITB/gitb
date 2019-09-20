package controllers

import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AttachmentType, AuthorizationManager, OrganizationManager, UserManager}
import org.apache.commons.codec.binary.Base64
import org.apache.tika.Tika
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import play.api.mvc._
import utils.{ClamAVClient, HtmlUtil, JsonUtil}


class AccountService @Inject() (accountManager: AccountManager, userManager: UserManager, organisationManager: OrganizationManager, authorizationManager: AuthorizationManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AccountService])
  private final val tika = new Tika()

  /**
   * Gets the company profile for the authenticated user
   */
  def getVendorProfile = AuthorizedAction { request =>
    authorizationManager.canViewOwnOrganisation(request)
    val userId = ParameterExtractor.extractUserId(request)

    val organization = accountManager.getVendorProfile(userId)
    val json:String = JsonUtil.serializeOrganization(organization)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates the company profile
   */
  def updateVendorProfile = AuthorizedAction { request =>
    authorizationManager.canUpdateOwnOrganisation(request)
    val adminId = ParameterExtractor.extractUserId(request)

    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val values = ParameterExtractor.extractOrganisationParameterValues(request, Parameters.PROPERTIES, true)
    var response: Result = ParameterExtractor.checkOrganisationParameterValues(values)
    if (response == null) {
      organisationManager.updateOwnOrganization(adminId, shortName, fullName, values)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  /**
   * Gets the all users for the vendor
   */
  def getVendorUsers = AuthorizedAction { request =>
    authorizationManager.canViewOwnOrganisationnUsers(request)
    val userId = ParameterExtractor.extractUserId(request)

    val list = accountManager.getVendorUsers(userId)
    val json:String = JsonUtil.jsUsers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * The authenticated admin registers new user for the organization
   */
  def registerUser = AuthorizedAction { request =>
    authorizationManager.canCreateUserInOwnOrganisation(request)
    val adminId = ParameterExtractor.extractUserId(request)
    val user = ParameterExtractor.extractUserInfo(request)

    accountManager.registerUser(adminId, user)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Returns the user profile of the authenticated user
   */
  def getUserProfile = AuthorizedAction { request =>
    authorizationManager.canViewOwnProfile(request)
    val userId = ParameterExtractor.extractUserId(request)

    val user = accountManager.getUserProfile(userId)
    val json:String = JsonUtil.serializeUser(user)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Updates the user profile of the authenticated user
   */
  def updateUserProfile = AuthorizedAction { request =>
    authorizationManager.canUpdateOwnProfile(request)
    val userId = ParameterExtractor.extractUserId(request)

    val name:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.USER_NAME)
    val passwd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
    val oldPasswd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.OLD_PASSWORD)

    accountManager.updateUserProfile(userId, name, passwd, oldPasswd)
    ResponseConstructor.constructEmptyResponse
  }

  def getConfiguration = AuthorizedAction { request =>
    authorizationManager.canViewConfiguration(request)
    val configProperties = new java.util.HashMap[String, String]()
    configProperties.put("email.enabled", String.valueOf(Configurations.EMAIL_ENABLED))
    configProperties.put("email.attachments.maxCount", String.valueOf(Configurations.EMAIL_ATTACHMENTS_MAX_COUNT))
    configProperties.put("email.attachments.maxSize", String.valueOf(Configurations.EMAIL_ATTACHMENTS_MAX_SIZE))
    configProperties.put("email.attachments.allowedTypes", String.valueOf(Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR))
    configProperties.put("survey.enabled", String.valueOf(Configurations.SURVEY_ENABLED))
    configProperties.put("survey.address", String.valueOf(Configurations.SURVEY_ADDRESS))
    configProperties.put("userguide.ou", String.valueOf(Configurations.USERGUIDE_OU))
    configProperties.put("userguide.oa", String.valueOf(Configurations.USERGUIDE_OA))
    configProperties.put("userguide.ta", String.valueOf(Configurations.USERGUIDE_TA))
    configProperties.put("userguide.ca", String.valueOf(Configurations.USERGUIDE_CA))
    configProperties.put("sso.enabled", String.valueOf(Configurations.AUTHENTICATION_SSO_ENABLED))
    configProperties.put("sso.inMigration", String.valueOf(Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD))
    configProperties.put("demos.enabled", String.valueOf(Configurations.DEMOS_ENABLED))
    configProperties.put("demos.account", String.valueOf(Configurations.DEMOS_ACCOUNT))
    configProperties.put("registration.enabled", String.valueOf(Configurations.REGISTRATION_ENABLED))
    configProperties.put("savedFile.maxSize", String.valueOf(Configurations.SAVED_FILE_MAX_SIZE))
    val json = JsonUtil.serializeConfigurationProperties(configProperties)
    ResponseConstructor.constructJsonResponse(json.toString())
  }

  def submitFeedback = AuthorizedAction { request =>
    authorizationManager.canSubmitFeedback(request)
    val userId = ParameterExtractor.extractOptionalUserId(request)
    val userEmail: String = ParameterExtractor.requiredBodyParameter(request, Parameters.USER_EMAIL)
    val messageTypeId: String = ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_TYPE_ID)
    val messageTypeDescription: String = ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_TYPE_DESCRIPTION)
    val messageContent: String = HtmlUtil.sanitizeMinimalEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.MESSAGE_CONTENT))

    var response:Result = null

    // Extract attachments
    val attachments = scala.collection.mutable.ArrayBuffer.empty[AttachmentType]
    val messageAttachmentCount = ParameterExtractor.optionalIntBodyParameter(request, "msg_attachments_count")
    var totalAttachmentSize = 0

    if (messageAttachmentCount.isDefined) {
      for (index <- 0 until messageAttachmentCount.get) {
        val name = ParameterExtractor.requiredBodyParameter(request, "msg_attachments[" + index + "][name]")
        var content = ParameterExtractor.requiredBodyParameter(request, "msg_attachments[" + index + "][data]")
        if (content.startsWith("data:")) {
          // Data URL
          content = content.substring(content.indexOf(",") + 1)
        }
        val bytes = Base64.decodeBase64(content)
        attachments += new AttachmentType(name, bytes)
        totalAttachmentSize += bytes.length
      }
      // Validate attachments
      if (attachments.length > Configurations.EMAIL_ATTACHMENTS_MAX_COUNT) {
        // Count.
        response = ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_COUNT_EXCEEDED, "A maximum of " + Configurations.EMAIL_ATTACHMENTS_MAX_COUNT + " attachments can be provided")
      } else if (totalAttachmentSize > (Configurations.EMAIL_ATTACHMENTS_MAX_SIZE * 1024 * 1024)) {
        // Size.
        response = ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_COUNT_EXCEEDED, "The total size of attachments cannot exceed " + Configurations.EMAIL_ATTACHMENTS_MAX_SIZE + " MBs.")
      } else {
        var index = 0
        while (index < messageAttachmentCount.get && response == null) {
          val detectedMimeType = tika.detect(attachments(index).getContent)
          if (!Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES.contains(detectedMimeType)) {
            logger.warn("Attachment type [" + detectedMimeType + "] of file [" + attachments(index).getName + "] not allowed.")
            response = ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_TYPE_NOT_ALLOWED, "Attachment ["+attachments(index).getName+"] not allowed. Allowed types are images, text files and PDFs.")
          } else {
            attachments(index).setType(detectedMimeType);
            index += 1
          }
        }
        if (response == null && Configurations.ANTIVIRUS_SERVER_ENABLED) {
          val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
          index = 0
          while (index < messageAttachmentCount.get && response == null) {
            val scanResult = virusScanner.scan(attachments(index).getContent)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              logger.warn("Attachment [" + attachments(index).getName + "] found to contain virus.")
              response = ResponseConstructor.constructErrorResponse(ErrorCodes.VIRUS_FOUND, "Attachments failed virus scan.")
            } else {
              index += 1
            }
          }
        }
      }
    }
    if (response == null) {
      accountManager.submitFeedback(userId, userEmail, messageTypeId, messageTypeDescription, messageContent, attachments.toArray)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }
}
