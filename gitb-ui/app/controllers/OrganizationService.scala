package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.OrganizationManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[OrganizationService])

  /**
   * Gets all organizations except the default organization for system administrators
   */
  def getOrganizations() = Action.async {
    OrganizationManager.getOrganizations() map { list =>
      val json: String = JsonUtil.jsOrganizations(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets the organization with specified id
   */
  def getOrganizationById(orgId: Long) = Action.async { request =>
    OrganizationManager.getOrganizationById(orgId) map { organization =>
      val json: String = JsonUtil.serializeOrganization(organization)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization() = Action.async { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    OrganizationManager.createOrganization(organization) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Updates user profile
   */
  def updateOrganization(orgId: Long) = Action.async { request =>
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    OrganizationManager.updateOrganization(orgId, shortName, fullName, landingPageId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes organization by id
   */
  def deleteOrganization(orgId: Long) = Action.async { request =>
    OrganizationManager.deleteOrganization(orgId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }
}
