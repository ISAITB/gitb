package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.OrganizationManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[OrganizationService])

  /**
   * Gets all organizations except the default organization for system administrators
   */
  def getOrganizations() = Action.apply {
    val list = OrganizationManager.getOrganizations()
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organization with specified id
   */
  def getOrganizationById(orgId: Long) = Action.apply { request =>
    val organization = OrganizationManager.getOrganizationById(orgId)
    val json: String = JsonUtil.serializeOrganization(organization)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long) = Action.apply { request =>
    val list = OrganizationManager.getOrganizationsByCommunity(communityId)
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new organization
   */
  def createOrganization() = Action.apply { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    OrganizationManager.createOrganization(organization)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Updates organization
   */
  def updateOrganization(orgId: Long) = Action.apply { request =>
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LEGAL_NOTICE_ID)
    OrganizationManager.updateOrganization(orgId, shortName, fullName, landingPageId, legalNoticeId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Deletes organization by id
   */
  def deleteOrganization(orgId: Long) = Action.apply { request =>
    OrganizationManager.deleteOrganization(orgId)
    ResponseConstructor.constructEmptyResponse
  }
}