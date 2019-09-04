package controllers

import controllers.util.ParameterExtractor.{optionalBodyParameter, requiredBodyParameter, validCommunitySelfRegType}
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityManager}
import models.Enums.SelfRegistrationType
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Controller, Result}
import utils.JsonUtil

class CommunityService @Inject() (communityManager: CommunityManager, authorizationManager: AuthorizationManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
   * Gets all communities with given ids or all if none specified
   */
  def getCommunities() = AuthorizedAction { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewCommunities(request, communityIds)

    val communities = communityManager.getCommunities(communityIds)
    val json = JsonUtil.jsCommunities(communities).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new community
   */
  def createCommunity() = AuthorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    var result: Result = null
    val community = ParameterExtractor.extractCommunityInfo(request)
    var proceed = false
    if (community.selfRegType == SelfRegistrationType.Token.id.toShort || community.selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort) {
      if (communityManager.isSelfRegTokenUnique(community.selfRegToken.get, None)) {
        proceed = true
      } else {
        result = ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_SELFREG_TOKEN, "The provided self-registration token is already in use.")
      }
    } else {
      proceed = true
    }
    if (proceed) {
      communityManager.createCommunity(community)
      result = ResponseConstructor.constructEmptyResponse
    }
    result
  }

  /**
   * Gets the community with specified id
   */
  def getCommunityById(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewCommunityFull(request, communityId)
    val community = communityManager.getCommunityById(communityId)
    val json: String = JsonUtil.serializeCommunity(community)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates community
   */
  def updateCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateCommunity(request, communityId)
    var result: Result = null
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val email = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_EMAIL)
    val selfRegType = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_TYPE).toShort
    if (!ParameterExtractor.validCommunitySelfRegType(selfRegType)) {
      throw new IllegalArgumentException("Unsupported value ["+selfRegType+"] for self-registration type")
    }
    var selfRegToken = ParameterExtractor.optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN)
    var proceed = false
    if (selfRegType == SelfRegistrationType.Token.id.toShort || selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort) {
      if (selfRegToken.isEmpty || StringUtils.isBlank(selfRegToken.get)) {
        throw new IllegalArgumentException("Missing self-registration token")
      } else {
        // Ensure that the token is unique.
        if (communityManager.isSelfRegTokenUnique(selfRegToken.get, Some(communityId))) {
          proceed = true
        } else {
          result = ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_SELFREG_TOKEN, "The provided self-registration token is already in use.")
        }
      }
    } else {
      selfRegToken = None
      proceed = true
    }
    if (proceed) {
      val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      communityManager.updateCommunity(communityId, shortName, fullName, email, selfRegType, selfRegToken, domainId)
      result = ResponseConstructor.constructEmptyResponse
    }
    result
  }

  /**
   * Deletes the community with specified id
   */
  def deleteCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteCommunity(request, communityId)
    communityManager.deleteCommunity(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity = AuthorizedAction { request =>
    val userId = ParameterExtractor.extractUserId(request)
    authorizationManager.canViewOwnCommunity(request)

    val community = communityManager.getUserCommunity(userId)
    val json:String = JsonUtil.serializeCommunity(community)
    ResponseConstructor.constructJsonResponse(json)
  }

}