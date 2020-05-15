package controllers

import config.Configurations
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityManager, OrganizationManager}
import models.Enums.{SelfRegistrationRestriction, SelfRegistrationType}
import models.{ActualUserInfo, Communities, Organizations, Users}
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.AuthenticationManager
import play.api.mvc.{Controller, Result}
import utils.{EmailUtil, HtmlUtil, JsonUtil}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class CommunityService @Inject() (communityManager: CommunityManager, authorizationManager: AuthorizationManager, organisationManager: OrganizationManager, authenticationManager: AuthenticationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities() = AuthorizedAction { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds)
    val skipDefault = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SKIP_DEFAULT)
    val communities = communityManager.getCommunities(communityIds, skipDefault.isDefined && skipDefault.get)
    val json = JsonUtil.jsCommunities(communities).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Creates new community
    */
  def createCommunity() = AuthorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    val community = ParameterExtractor.extractCommunityInfo(request)
    communityManager.createCommunity(community)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the community with specified id
    */
  def getCommunityById(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewCommunityFull(request, communityId)
    val community = communityManager.getCommunityById(communityId)
    val json: String = JsonUtil.serializeCommunity(community, None)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Updates community
    */
  def updateCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateCommunity(request, communityId)
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val email = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_EMAIL)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    var selfRegType: Short = SelfRegistrationType.NotSupported.id.toShort
    var selfRegRestriction: Short = SelfRegistrationRestriction.NoRestriction.id.toShort
    var selfRegToken: Option[String] = None
    var selfRegTokenHelpText: Option[String] = None
    var selfRegNotification: Boolean = false
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
      if (selfRegType != SelfRegistrationType.NotSupported.id.toShort && Configurations.EMAIL_ENABLED) {
        selfRegNotification = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_NOTIFICATION).toBoolean
      }
      if (Configurations.AUTHENTICATION_SSO_ENABLED) {
        selfRegRestriction = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_RESTRICTION).toShort
      }
    }
    val domainId: Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    communityManager.updateCommunity(communityId, shortName, fullName, email, selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification, description, selfRegRestriction, domainId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Deletes the community with specified id
    */
  def deleteCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteCommunity(request, communityId)
    communityManager.deleteCommunity(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  def selfRegister() = AuthorizedAction { request =>
    var response: Result = null
    val selfRegToken = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN)
    val organisation = ParameterExtractor.extractOrganizationInfo(request)
    var organisationAdmin: Users = null
    var actualUserInfo: Option[ActualUserInfo] = None
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      actualUserInfo = Some(authorizationManager.getPrincipal(request))
      organisationAdmin = ParameterExtractor.extractAdminInfo(request, Some(actualUserInfo.get.email), None)
    } else {
      organisationAdmin = ParameterExtractor.extractAdminInfo(request, None, Some(false))
    }
    val templateId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.TEMPLATE_ID)
    authorizationManager.canSelfRegister(request, organisation, organisationAdmin, selfRegToken, templateId)
    val community = communityManager.getById(organisation.community)
    if (community.isDefined) {
      // Check that the token (if required) matches
      if (community.get.selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort
        && selfRegToken.isDefined && community.get.selfRegToken.isDefined
        && community.get.selfRegToken.get != selfRegToken.get) {
        response = ResponseConstructor.constructErrorResponse(ErrorCodes.INCORRECT_SELFREG_TOKEN, "The provided token is incorrect.")
      }
      if (response == null) {
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          // Check to see whether self-registration restrictions are in force (only if SSO)
          if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmail.id.toShort && communityManager.existsOrganisationWithSameUserEmail(community.get.id, actualUserInfo.get.email)) {
            response = ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL, "You are already registered in this community.")
          } else if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmailDomain.id.toShort && communityManager.existsOrganisationWithSameUserEmailDomain(community.get.id, actualUserInfo.get.email)) {
            response = ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL_DOMAIN, "Your organisation is already registered in this community.")
          }
        } else {
          // Check the user email (only if not SSO)
          if (!authenticationManager.checkEmailAvailability(organisationAdmin.email, None, None, None)) {
            response = ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_EXISTS, "The provided email is already defined.")
          }
        }
      }
      if (response == null) {
        val customPropertyValues = ParameterExtractor.extractOrganisationParameterValues(request, Parameters.PROPERTIES, true)
        response = ParameterExtractor.checkOrganisationParameterValues(customPropertyValues)
        if (response == null) {
          val userId = communityManager.selfRegister(organisation, organisationAdmin, templateId, actualUserInfo, customPropertyValues)
          if (Configurations.AUTHENTICATION_SSO_ENABLED) {
            val json: String = JsonUtil.jsActualUserInfo(authorizationManager.getAccountInfo(request)).toString
            response = ResponseConstructor.constructJsonResponse(json)
          } else {
            response = ResponseConstructor.constructJsonResponse(JsonUtil.jsId(userId).toString)
          }
          // Self registration successful - notify support email if configured to do so.
          if (Configurations.EMAIL_ENABLED && community.get.selfregNotification) {
            notifyForSelfRegistration(community.get, organisation)
          }
        }
      }
    }
    if (response == null) {
      response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_SELFREG_DATA, "Unable to self-register due to error in provided configuration.")
    }
    response
  }

  private def notifyForSelfRegistration(community: Communities, organisation: Organizations) = {
    if (Configurations.EMAIL_ENABLED && community.selfregNotification && community.supportEmail.isDefined) {
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
          EmailUtil.sendEmail(Configurations.EMAIL_FROM, Array[String](community.supportEmail.get), null, subject, content, null)
        } catch {
          case e:Exception => {
            logger.error("Error while sending self registration notification for community ["+community.id+"]", e)
          }
        }
      }
    }
  }

  def getSelfRegistrationOptions() = AuthorizedAction { request =>
    authorizationManager.canViewSelfRegistrationOptions(request)
    val options = communityManager.getSelfRegistrationOptions()
    val json: String = JsonUtil.jsSelfRegOptions(options).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity = AuthorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    authorizationManager.canViewOwnCommunity(request)

    val community = communityManager.getUserCommunity(userId)
    val labels = communityManager.getCommunityLabels(community.id)
    val json: String = JsonUtil.serializeCommunity(community, Some(labels))
    ResponseConstructor.constructJsonResponse(json)
  }

  def createOrganisationParameter() = AuthorizedAction { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)
    if (communityManager.checkOrganisationParameterExists(parameter, false)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      val id = communityManager.createOrganisationParameter(parameter)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
    }
  }

  def createSystemParameter() = AuthorizedAction { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)
    if (communityManager.checkSystemParameterExists(parameter, false)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      val id = communityManager.createSystemParameter(parameter)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
    }
  }

  def deleteOrganisationParameter(parameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageOrganisationParameter(request, parameterId)
    communityManager.deleteOrganisationParameterWrapper(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteSystemParameter(parameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageSystemParameter(request, parameterId)
    communityManager.deleteSystemParameterWrapper(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateOrganisationParameter(parameterId: Long) = AuthorizedAction { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)

    if (communityManager.checkOrganisationParameterExists(parameter, true)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      communityManager.updateOrganisationParameter(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateSystemParameter(parameterId: Long) = AuthorizedAction { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community)

    if (communityManager.checkSystemParameterExists(parameter, true)) {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
    } else {
      communityManager.updateSystemParameter(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getOrganisationParameters(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsOrganisationParameters(communityManager.getOrganisationParameters(communityId)).toString)
  }

  def getSystemParameters(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewCommunityBasic(request, communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSystemParameters(communityManager.getSystemParameters(communityId)).toString)
  }

  def getCommunityLabels(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewCommunityBasic(request, communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityLabels(communityManager.getCommunityLabels(communityId)).toString)
  }

  def setCommunityLabels(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val labels = JsonUtil.parseJsCommunityLabels(communityId, requiredBodyParameter(request, Parameters.VALUES))
    communityManager.setCommunityLabels(communityId, labels)
    ResponseConstructor.constructEmptyResponse
  }

}