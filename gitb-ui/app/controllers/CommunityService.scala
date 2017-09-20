package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.CommunityManager

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil

import scala.concurrent.Future

class CommunityService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CommunityService])

  /**
   * Gets all communities with given ids or all if none specified
   */
  def getCommunities() = Action.async { request =>
    val communityIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    CommunityManager.getCommunities(communityIds) map { communities =>
      val json = JsonUtil.jsCommunities(communities).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Creates new community
   */
  def createCommunity() = Action.async { request =>
    val community = ParameterExtractor.extractCommunityInfo(request)
    CommunityManager.createCommunity(community) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Gets the community with specified id
   */
  def getCommunityById(communityId: Long) = Action.async { request =>
    CommunityManager.getCommunityById(communityId) map { community =>
      val json: String = JsonUtil.serializeCommunity(community)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Updates community
   */
  def updateCommunity(communityId: Long) = Action.async { request =>
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    CommunityManager.updateCommunity(communityId, shortName, fullName, domainId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes the community with specified id
   */
  def deleteCommunity(communityId: Long) = Action.async { request =>
    CommunityManager.deleteCommunity(communityId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
    * Returns the community of the authenticated user
    */
  def getUserCommunity = Action.async { request =>
    val userId = ParameterExtractor.extractUserId(request)

    CommunityManager.getUserCommunity(userId) map { community =>
      val json:String = JsonUtil.serializeCommunity(community)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

}