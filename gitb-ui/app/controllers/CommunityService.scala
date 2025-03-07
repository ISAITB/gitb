package controllers

import config.Configurations
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthenticationManager, AuthorizationManager, CommunityManager}
import models.Enums.{SelfRegistrationRestriction, SelfRegistrationType}
import models.{ActualUserInfo, Communities, Organizations, Users}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{CryptoUtil, EmailUtil, HtmlUtil, JsonUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class CommunityService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, communityManager: CommunityManager, authorizationManager: AuthorizationManager, authenticationManager: AuthenticationManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities() = authorizedAction { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds)
    val skipDefault = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SKIP_DEFAULT)
    val communities = communityManager.getCommunities(communityIds, skipDefault.isDefined && skipDefault.get)
    val json = JsonUtil.jsCommunities(communities).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getUserCommunities() = authorizedAction { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds)
    val communities = communityManager.getCommunities(communityIds, skipDefault = true)
    val json = JsonUtil.jsCommunities(communities).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Creates new community
    */
  def createCommunity() = authorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    val community = ParameterExtractor.extractCommunityInfo(request)
    communityManager.createCommunity(community)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the community with specified id
    */
  def getCommunityById(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewCommunityFull(request, communityId)
    val community = communityManager.getCommunityById(communityId)
    val json: String = JsonUtil.serializeCommunity(community, None, includeAdminInfo = true)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Updates community
    */
  def updateCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateCommunity(request, communityId)
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val email = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_EMAIL)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val allowCertificateDownload = ParameterExtractor.requiredBodyParameter(request, Parameters.ALLOW_CERTIFICATE_DOWNLOAD).toBoolean
    val allowStatementManagement = requiredBodyParameter(request, Parameters.ALLOW_STATEMENT_MANAGEMENT).toBoolean
    val allowSystemManagement = requiredBodyParameter(request, Parameters.ALLOW_SYSTEM_MANAGEMENT).toBoolean
    val allowPostTestOrganisationUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_ORG_UPDATE).toBoolean
    val allowPostTestSystemUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_SYS_UPDATE).toBoolean
    val allowPostTestStatementUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_STM_UPDATE).toBoolean
    var allowAutomationApi: Option[Boolean] = None
    if (Configurations.AUTOMATION_API_ENABLED) {
      allowAutomationApi = Some(requiredBodyParameter(request, Parameters.ALLOW_AUTOMATION_API).toBoolean)
    }
    val interactionNotification = requiredBodyParameter(request, Parameters.COMMUNITY_INTERACTION_NOTIFICATION).toBoolean
    var selfRegType: Short = SelfRegistrationType.NotSupported.id.toShort
    var selfRegRestriction: Short = SelfRegistrationRestriction.NoRestriction.id.toShort
    var selfRegToken: Option[String] = None
    var selfRegTokenHelpText: Option[String] = None
    var selfRegNotification: Boolean = false
    var selfRegForceTemplateSelection: Boolean = false
    var selfRegForceRequiredProperties: Boolean = false
    if (Configurations.REGISTRATION_ENABLED) {
      selfRegType = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_TYPE).toShort
      if (!ParameterExtractor.validCommunitySelfRegType(selfRegType)) {
        throw new IllegalArgumentException("Unsupported value [" + selfRegType + "] for self-registration type")
      }
      selfRegToken = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN)
      selfRegTokenHelpText = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN_HELP_TEXT)
      if (selfRegTokenHelpText.isDefined) {
        selfRegTokenHelpText = Some(HtmlUtil.sanitizeEditorContent(selfRegTokenHelpText.get))
      }
      if (selfRegType == SelfRegistrationType.Token.id.toShort || selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort) {
        if (selfRegToken.isEmpty || StringUtils.isBlank(selfRegToken.get)) {
          throw new IllegalArgumentException("Missing self-registration token")
        }
      } else {
        selfRegToken = None
        selfRegTokenHelpText = None
      }
      if (selfRegType != SelfRegistrationType.NotSupported.id.toShort) {
        selfRegForceTemplateSelection = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_FORCE_TEMPLATE).toBoolean
        selfRegForceRequiredProperties = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_FORCE_PROPERTIES).toBoolean
        if (Configurations.EMAIL_ENABLED) {
          selfRegNotification = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_NOTIFICATION).toBoolean
        }
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          selfRegRestriction = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_RESTRICTION).toShort
        }
      }
    }
    val domainId: Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    communityManager.updateCommunity(
      communityId, shortName, fullName, email, selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification,
      interactionNotification, description, selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
      allowCertificateDownload, allowStatementManagement, allowSystemManagement,
      allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate, allowAutomationApi,
      domainId
    )
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Deletes the community with specified id
    */
  def deleteCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteCommunity(request, communityId)
    communityManager.deleteCommunity(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  def selfRegister() = authorizedAction { request =>
    try {
      val paramMap = ParameterExtractor.paramMap(request)
      var response: Result = null
      val selfRegToken = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.COMMUNITY_SELFREG_TOKEN)
      val organisation = ParameterExtractor.extractOrganizationInfo(paramMap)
      var organisationAdmin: Users = null
      var actualUserInfo: Option[ActualUserInfo] = None
      if (Configurations.AUTHENTICATION_SSO_ENABLED) {
        actualUserInfo = Some(authorizationManager.getPrincipal(request))
        organisationAdmin = ParameterExtractor.extractAdminInfo(paramMap, Some(actualUserInfo.get.email), None)
      } else {
        organisationAdmin = ParameterExtractor.extractAdminInfo(paramMap, None, Some(false))
        if (!CryptoUtil.isAcceptedPassword(organisationAdmin.password)) {
          response = ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided password does not match minimum complexity requirements.", Some("new"))
        }
      }

      if (response == null) {
        val templateId = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.TEMPLATE_ID)
        authorizationManager.canSelfRegister(request, organisation, organisationAdmin, selfRegToken, templateId)
        val community = communityManager.getById(organisation.community)
        if (community.isDefined) {
          // Check in case template selection is required
          if (community.get.selfRegForceTemplateSelection && templateId.isEmpty) {
            response = ResponseConstructor.constructErrorResponse(ErrorCodes.MISSING_PARAMS, "A configuration template must be selected.", Some("template"))
          }
          if (response == null) {
            // Check that the token (if required) matches
            if (community.get.selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort
              && selfRegToken.isDefined && community.get.selfRegToken.isDefined
              && community.get.selfRegToken.get != selfRegToken.get) {
              response = ResponseConstructor.constructErrorResponse(ErrorCodes.INCORRECT_SELFREG_TOKEN, "The provided token is incorrect.", Some("token"))
            }
          }
          if (response == null) {
            if (Configurations.AUTHENTICATION_SSO_ENABLED) {
              // Check to see whether self-registration restrictions are in force (only if SSO)
              if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmail.id.toShort && communityManager.existsOrganisationWithSameUserEmail(community.get.id, actualUserInfo.get.email)) {
                response = ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL, "You are already registered in this community.", Some("community"))
              } else if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmailDomain.id.toShort && communityManager.existsOrganisationWithSameUserEmailDomain(community.get.id, actualUserInfo.get.email)) {
                response = ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL_DOMAIN, "Your organisation is already registered in this community.", Some("community"))
              }
            } else {
              // Check the user email (only if not SSO)
              if (!authenticationManager.checkEmailAvailability(organisationAdmin.email, None, None, None)) {
                response = ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_EXISTS, "The provided username is already defined.", Some("adminEmail"))
              }
            }
          }
          if (response == null) {
            val customPropertyValues = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
            val customPropertyFiles = ParameterExtractor.extractFiles(request).map {
              case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
            }
            if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(customPropertyFiles.map(entry => entry._2.file))) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
            }
            if (response == null) {
              val userId = communityManager.selfRegister(organisation, organisationAdmin, templateId, actualUserInfo, customPropertyValues, Some(customPropertyFiles), community.get.selfRegForceRequiredProperties)
              if (Configurations.AUTHENTICATION_SSO_ENABLED) {
                val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
                response = ResponseConstructor.constructJsonResponse(json)
              } else {
                response = ResponseConstructor.constructJsonResponse(JsonUtil.jsId(userId).toString)
              }
              // Self registration successful - notify support email if configured to do so.
              if (Configurations.EMAIL_ENABLED && community.get.selfRegNotification) {
                notifyForSelfRegistration(community.get, organisation)
              }
            }
          }
        }
      }
      if (response == null) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_SELFREG_DATA, "Unable to self-register due to error in provided configuration.")
      }
      response


    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def notifyForSelfRegistration(community: Communities, organisation: Organizations) = {
    if (Configurations.EMAIL_ENABLED && community.selfRegNotification && community.supportEmail.isDefined) {
      implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
      scala.concurrent.Future {
        val subject = "Test Bed self-registration notification"
        var content = "<h2>New Test Bed community member</h2>"
        // User information not included to avoid data privacy statements
        content +=
          "A new organisation has joined your Test Bed community through the self-registration process.<br/>" +
          "<br/><b>Organisation:</b> "+organisation.fullname +
          "<br/><b>Community:</b> "+community.fullname +
          "<br/><br/>Click <a href=\""+Configurations.TESTBED_HOME_LINK+"\">here</a> to connect and view the update on the Test Bed."
        try {
          EmailUtil.sendEmail(Array[String](community.supportEmail.get), null, subject, content, null)
        } catch {
          case e:Exception => {
            logger.error("Error while sending self registration notification for community ["+community.id+"]", e)
          }
        }
      }
    }
  }

  def getSelfRegistrationOptions() = authorizedAction { request =>
    authorizationManager.canViewSelfRegistrationOptions(request)
    val options = communityManager.getSelfRegistrationOptions()
    val json: String = JsonUtil.jsSelfRegOptions(options).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity = authorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    authorizationManager.canViewOwnCommunity(request)

    val community = communityManager.getUserCommunity(userId)
    val labels = communityManager.getCommunityLabels(community.id)
    val json: String = JsonUtil.serializeCommunity(community, Some(labels), includeAdminInfo = false)
    ResponseConstructor.constructJsonResponse(json)
  }

  def orderOrganisationParameters(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
    communityManager.orderOrganisationParameters(communityId, orderedIds.getOrElse(List[Long]()))
    ResponseConstructor.constructEmptyResponse
  }

  def orderSystemParameters(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
    communityManager.orderSystemParameters(communityId, orderedIds.getOrElse(List[Long]()))
    ResponseConstructor.constructEmptyResponse
  }

  def createOrganisationParameter(): Action[AnyContent] = authorizedAction { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)
    if (communityManager.checkOrganisationParameterExists(parameter, isUpdate = false)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      val id = communityManager.createOrganisationParameter(parameter)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
    }
  }

  def createSystemParameter() = authorizedAction { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)
    if (communityManager.checkSystemParameterExists(parameter, false)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      val id = communityManager.createSystemParameter(parameter)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
    }
  }

  def deleteOrganisationParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canManageOrganisationParameter(request, parameterId)
    communityManager.deleteOrganisationParameterWrapper(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteSystemParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canManageSystemParameter(request, parameterId)
    communityManager.deleteSystemParameterWrapper(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateOrganisationParameter(parameterId: Long) = authorizedAction { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)

    if (communityManager.checkOrganisationParameterExists(parameter, true)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      communityManager.updateOrganisationParameter(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateSystemParameter(parameterId: Long) = authorizedAction { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)

    if (communityManager.checkSystemParameterExists(parameter, true)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      communityManager.updateSystemParameter(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getOrganisationParameters(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val forFiltering = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.FILTERING)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsOrganisationParameters(communityManager.getOrganisationParameters(communityId, forFiltering)).toString)
  }

  def getSystemParameters(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewCommunityBasic(request, communityId)
    val forFiltering = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.FILTERING)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSystemParameters(communityManager.getSystemParameters(communityId, forFiltering)).toString)
  }

  def getCommunityLabels(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewCommunityBasic(request, communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityLabels(communityManager.getCommunityLabels(communityId)).toString)
  }

  def setCommunityLabels(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val labels = JsonUtil.parseJsCommunityLabels(communityId, requiredBodyParameter(request, Parameters.VALUES))
    communityManager.setCommunityLabels(communityId, labels)
    ResponseConstructor.constructEmptyResponse
  }

  def getCommunityIdOfDomain: Action[AnyContent] = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.checkTestBedAdmin(request)
    val communityId = communityManager.getCommunityIdOfDomain(domainId)
    if (communityId.isDefined) {
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCommunityIdOfActor: Action[AnyContent] = authorizedAction { request =>
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.checkTestBedAdmin(request)
    val communityId = communityManager.getCommunityIdOfActor(actorId)
    if (communityId.isDefined) {
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCommunityIdOfSnapshot: Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.requiredQueryParameter(request, Parameters.SNAPSHOT).toLong
    authorizationManager.checkTestBedAdmin(request)
    val communityId = communityManager.getCommunityIdOfSnapshot(snapshotId)
    if (communityId.isDefined) {
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

}