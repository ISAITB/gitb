package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityManager}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller
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
    val json: String = JsonUtil.serializeCommunity(community)
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
    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    communityManager.updateCommunity(communityId, shortName, fullName, email, domainId)
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