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
import controllers.CommunityService.SelfRegistrationInfo
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
import scala.concurrent.{ExecutionContext, Future}

object CommunityService {

  object SelfRegistrationInfo {

    def create(): SelfRegistrationInfo = {
      SelfRegistrationInfo(None, None, None, None)
    }

  }

  case class SelfRegistrationInfo(failure: Option[Result], community: Option[Communities], actualUserInfo: Option[ActualUserInfo], organisationAdmin: Option[Users]) {

    def withCommunity(community: Communities): SelfRegistrationInfo = {
      SelfRegistrationInfo(failure, Some(community), actualUserInfo, organisationAdmin)
    }

    def withFailure(result: Result): SelfRegistrationInfo = {
      SelfRegistrationInfo(Some(result), community, actualUserInfo, organisationAdmin)
    }

    def withFailure(result: Result, community: Communities): SelfRegistrationInfo = {
      SelfRegistrationInfo(Some(result), Some(community), actualUserInfo, organisationAdmin)
    }

    def withUser(actualUserInfo: Option[ActualUserInfo], organisationAdmin: Users): SelfRegistrationInfo = {
      SelfRegistrationInfo(failure, community, actualUserInfo, Some(organisationAdmin))
    }

  }

}

class CommunityService @Inject() (authorizedAction: AuthorizedAction,
                                  cc: ControllerComponents,
                                  communityManager: CommunityManager,
                                  authorizationManager: AuthorizationManager,
                                  authenticationManager: AuthenticationManager)
                                 (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities(): Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds).flatMap { _ =>
      val skipDefault = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SKIP_DEFAULT)
      communityManager.getCommunities(communityIds, skipDefault.isDefined && skipDefault.get).map { communities =>
        val json = JsonUtil.jsCommunities(communities).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getUserCommunities(): Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds).flatMap { _ =>
      communityManager.getCommunities(communityIds, skipDefault = true).map { communities =>
        val json = JsonUtil.jsCommunities(communities).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
    * Creates new community
    */
  def createCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateCommunity(request).flatMap { _ =>
      val community = ParameterExtractor.extractCommunityInfo(request)
      communityManager.createCommunity(community).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Gets the community with specified id
    */
  def getCommunityById(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewCommunityFull(request, communityId).flatMap { _ =>
      communityManager.getCommunityById(communityId).map { community =>
        val json: String = JsonUtil.serializeCommunity(community, None, includeAdminInfo = true)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
    * Updates community
    */
  def updateCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateCommunity(request, communityId).flatMap { _ =>
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
      val allowCommunityView = requiredBodyParameter(request, Parameters.ALLOW_COMMUNITY_VIEW).toBoolean
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
        allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate, allowAutomationApi, allowCommunityView,
        domainId
      ).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Deletes the community with specified id
    */
  def deleteCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteCommunity(request, communityId).flatMap { _ =>
      communityManager.deleteCommunity(communityId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def selfRegister(): Action[AnyContent] = authorizedAction.async { request =>
    val task = for {
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      selfRegToken <- Future.successful(ParameterExtractor.optionalBodyParameter(paramMap, Parameters.COMMUNITY_SELFREG_TOKEN))
      organisation <- Future.successful(ParameterExtractor.extractOrganizationInfo(paramMap))
      templateId  <- Future.successful(ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.TEMPLATE_ID))
      info <- {
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          authorizationManager.getPrincipal(request).map { actualUserInfo =>
            val organisationAdmin = ParameterExtractor.extractAdminInfo(paramMap, Some(actualUserInfo.email), None)
            SelfRegistrationInfo.create().withUser(Some(actualUserInfo), organisationAdmin)
          }
        } else {
          val organisationAdmin = ParameterExtractor.extractAdminInfo(paramMap, None, Some(false))
          if (CryptoUtil.isAcceptedPassword(organisationAdmin.password)) {
            Future.successful {
              SelfRegistrationInfo.create().withUser(None, organisationAdmin)
            }
          } else {
            Future.successful {
              SelfRegistrationInfo.create().withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided password does not match minimum complexity requirements.", Some("new")))
            }
          }
        }
      }
      info <- {
        if (info.failure.isEmpty) {
          authorizationManager.canSelfRegister(request, organisation, selfRegToken, templateId).flatMap { _ =>
            communityManager.getById(organisation.community).flatMap { community =>
              if (community.isDefined) {
                // Check in case template selection is required
                if (community.get.selfRegForceTemplateSelection && templateId.isEmpty) {
                  Future.successful {
                    info.withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.MISSING_PARAMS, "A configuration template must be selected.", Some("template")), community.get)
                  }
                } else {
                  // Check that the token (if required) matches
                  if (community.get.selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort
                    && selfRegToken.isDefined && community.get.selfRegToken.isDefined
                    && community.get.selfRegToken.get != selfRegToken.get) {
                    Future.successful {
                      info.withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.INCORRECT_SELFREG_TOKEN, "The provided token is incorrect.", Some("token")), community.get)
                    }
                  } else {
                    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
                      // Check to see whether self-registration restrictions are in force (only if SSO)
                      if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmail.id.toShort) {
                        communityManager.existsOrganisationWithSameUserEmail(community.get.id, info.actualUserInfo.get.email).map { exists =>
                          if (exists) {
                            info.withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL, "You are already registered in this community.", Some("community")), community.get)
                          } else {
                            info.withCommunity(community.get)
                          }
                        }
                      } else if (community.get.selfRegRestriction == SelfRegistrationRestriction.UserEmailDomain.id.toShort) {
                        communityManager.existsOrganisationWithSameUserEmailDomain(community.get.id, info.actualUserInfo.get.email).map { exists =>
                          if (exists) {
                            info.withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.SELF_REG_RESTRICTION_USER_EMAIL_DOMAIN, "Your organisation is already registered in this community.", Some("community")), community.get)
                          } else {
                            info.withCommunity(community.get)
                          }
                        }
                      } else {
                        Future.successful(info.withCommunity(community.get))
                      }
                    } else {
                      // Check the user email (only if not SSO)
                      authenticationManager.checkEmailAvailability(info.organisationAdmin.get.email, None, None, None).map { available =>
                        if (!available) {
                          info.withFailure(ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_EXISTS, "The provided username is already defined.", Some("adminEmail")), community.get)
                        } else {
                          info.withCommunity(community.get)
                        }
                      }
                    }
                  }
                }
              } else {
                Future.successful(info)
              }
            }
          }
        } else {
          Future.successful(info)
        }
      }
      result <- {
        if (info.failure.isEmpty) {
          val customPropertyValues = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
          val customPropertyFiles = ParameterExtractor.extractFiles(request).map {
            case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
          }
          if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(customPropertyFiles.map(entry => entry._2.file))) {
            Future.successful {
              ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
            }
          } else {
            communityManager.selfRegister(organisation, info.organisationAdmin.get, templateId, info.actualUserInfo, customPropertyValues, Some(customPropertyFiles), info.community.get.selfRegForceRequiredProperties).flatMap { userId =>
              // Self registration successful - notify support email if configured to do so.
              if (Configurations.EMAIL_ENABLED && info.community.get.selfRegNotification) {
                notifyForSelfRegistration(info.community.get, organisation)
              }
              if (Configurations.AUTHENTICATION_SSO_ENABLED) {
                authorizationManager.getAccountInfo(request).map { accountInfo =>
                  val json: String = JsonUtil.jsActualUserInfo(accountInfo).toString
                  ResponseConstructor.constructJsonResponse(json)
                }
              } else {
                Future.successful {
                  ResponseConstructor.constructJsonResponse(JsonUtil.jsId(userId).toString)
                }
              }
            }
          }
        } else {
          Future.successful(info.failure.get)
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def notifyForSelfRegistration(community: Communities, organisation: Organizations): Future[Unit] = {
    if (Configurations.EMAIL_ENABLED && community.selfRegNotification && community.supportEmail.isDefined) {
      Future {
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
          case e:Exception =>
            logger.error("Error while sending self registration notification for community ["+community.id+"]", e)
        }
      }
    } else {
      Future.successful(())
    }
  }

  def getSelfRegistrationOptions(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSelfRegistrationOptions(request).flatMap { _ =>
      communityManager.getSelfRegistrationOptions().map { options =>
        val json: String = JsonUtil.jsSelfRegOptions(options).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity: Action[AnyContent] = authorizedAction.async { request =>
    val userId = ParameterExtractor.extractUserId(request)
    authorizationManager.canViewOwnCommunity(request).flatMap { _ =>
      communityManager.getUserCommunity(userId).flatMap { community =>
        communityManager.getCommunityLabels(community.id).map { labels =>
          val json: String = JsonUtil.serializeCommunity(community, Some(labels), includeAdminInfo = false)
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def orderOrganisationParameters(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
      communityManager.orderOrganisationParameters(communityId, orderedIds.getOrElse(List[Long]())).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def orderSystemParameters(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
      communityManager.orderSystemParameters(communityId, orderedIds.getOrElse(List[Long]())).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def createOrganisationParameter(): Action[AnyContent] = authorizedAction.async { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community).flatMap { _ =>
      communityManager.checkOrganisationParameterExists(parameter, isUpdate = false).flatMap { exists =>
        if (exists) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
          }
        } else {
          communityManager.createOrganisationParameter(parameter).map { id =>
            ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
          }
        }
      }
    }
  }

  def createSystemParameter(): Action[AnyContent] = authorizedAction.async { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community).flatMap { _ =>
      communityManager.checkSystemParameterExists(parameter, isUpdate = false).flatMap { exists =>
        if (exists) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
          }
        } else {
          communityManager.createSystemParameter(parameter).map { id =>
            ResponseConstructor.constructJsonResponse(JsonUtil.jsId(id).toString)
          }
        }
      }
    }
  }

  def deleteOrganisationParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageOrganisationParameter(request, parameterId).flatMap { _ =>
      communityManager.deleteOrganisationParameterWrapper(parameterId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteSystemParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemParameter(request, parameterId).flatMap { _ =>
      communityManager.deleteSystemParameterWrapper(parameterId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateOrganisationParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val parameter = ParameterExtractor.extractOrganisationParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community).flatMap { _ =>
      communityManager.checkOrganisationParameterExists(parameter, isUpdate = true).flatMap { exists =>
        if (exists) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
          }
        } else {
          communityManager.updateOrganisationParameter(parameter).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def updateSystemParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val parameter = ParameterExtractor.extractSystemParameter(request)
    authorizationManager.canManageCommunity(request, parameter.community).flatMap { _ =>
      communityManager.checkSystemParameterExists(parameter, isUpdate = true).flatMap { exists =>
        if (exists) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.NAME_EXISTS, "The name and key of the property must be unique.")
          }
        } else {
          communityManager.updateSystemParameter(parameter).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def getOrganisationParameters(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val onlyPublic = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.PUBLIC).getOrElse(false)
    for {
      _ <- {
        if (onlyPublic) {
          authorizationManager.canViewCommunityBasic(request, communityId)
        } else {
          authorizationManager.canManageCommunity(request, communityId)
        }
      }
      result <- {
        val forFiltering = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.FILTERING)
        communityManager.getOrganisationParameters(communityId, forFiltering, onlyPublic).map { parameters =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsOrganisationParameters(parameters).toString)
        }
      }
    } yield result
  }

  def getSystemParameters(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val onlyPublic = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.PUBLIC).getOrElse(false)
    for {
      _ <- {
        if (onlyPublic) {
          authorizationManager.canViewCommunityBasic(request, communityId)
        } else {
          authorizationManager.canManageCommunity(request, communityId)
        }
      }
      result <- {
        val forFiltering = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.FILTERING)
        communityManager.getSystemParameters(communityId, forFiltering, onlyPublic).map { parameters =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsSystemParameters(parameters).toString)
        }
      }
    } yield result
  }

  def getCommunityLabels(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewCommunityBasic(request, communityId).flatMap { _ =>
      communityManager.getCommunityLabels(communityId).map { labels =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityLabels(labels).toString)
      }
    }
  }

  def setCommunityLabels(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val labels = JsonUtil.parseJsCommunityLabels(communityId, requiredBodyParameter(request, Parameters.VALUES))
      communityManager.setCommunityLabels(communityId, labels).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def getCommunityIdOfDomain: Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      communityManager.getCommunityIdOfDomain(domainId).map { communityId =>
        if (communityId.isDefined) {
          ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getCommunityIdOfActor: Action[AnyContent] = authorizedAction.async { request =>
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      communityManager.getCommunityIdOfActor(actorId).map { communityId =>
        if (communityId.isDefined) {
          ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getCommunityIdOfSnapshot: Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.requiredQueryParameter(request, Parameters.SNAPSHOT).toLong
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      communityManager.getCommunityIdOfSnapshot(snapshotId).map { communityId =>
        if (communityId.isDefined) {
          ResponseConstructor.constructJsonResponse(JsonUtil.jsId(communityId.get).toString())
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def searchCommunities(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewAllCommunities(request).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      communityManager.searchCommunities(page, limit, filter).map { result =>
        val json = JsonUtil.jsSearchResult(result, JsonUtil.jsCommunitiesLimited).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}