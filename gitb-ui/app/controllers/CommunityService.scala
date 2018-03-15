package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.CommunityManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

class CommunityService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
   * Gets all communities with given ids or all if none specified
   */
  def getCommunities() = Action.apply { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    val communities = CommunityManager.getCommunities(communityIds)
    val json = JsonUtil.jsCommunities(communities).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new community
   */
  def createCommunity() = Action.apply { request =>
    val community = ParameterExtractor.extractCommunityInfo(request)
    CommunityManager.createCommunity(community)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Gets the community with specified id
   */
  def getCommunityById(communityId: Long) = Action.apply { request =>
    val community = CommunityManager.getCommunityById(communityId)
    val json: String = JsonUtil.serializeCommunity(community)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates community
   */
  def updateCommunity(communityId: Long) = Action.apply { request =>
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    CommunityManager.updateCommunity(communityId, shortName, fullName, domainId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Deletes the community with specified id
   */
  def deleteCommunity(communityId: Long) = Action.apply { request =>
    CommunityManager.deleteCommunity(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity = Action.apply { request =>
    val userId = ParameterExtractor.extractUserId(request)

    val community = CommunityManager.getUserCommunity(userId)
    val json:String = JsonUtil.serializeCommunity(community)
    ResponseConstructor.constructJsonResponse(json)
  }

}