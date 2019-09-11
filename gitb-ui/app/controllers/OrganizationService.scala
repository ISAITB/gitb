package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, OrganizationManager}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller
import utils.JsonUtil

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService @Inject() (organizationManager: OrganizationManager, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[OrganizationService])

  /**
   * Gets all organizations except the default organization for system administrators
   */
  def getOrganizations() = AuthorizedAction { request =>
    authorizationManager.canViewAllOrganisations(request)
    val list = organizationManager.getOrganizations()
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organization with specified id
   */
  def getOrganizationById(orgId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewOrganisation(request, orgId)
    val organization = organizationManager.getOrganizationById(orgId)
    val json: String = JsonUtil.serializeOrganization(organization)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewOrganisationsByCommunity(request, communityId)
    val list = organizationManager.getOrganizationsByCommunity(communityId)
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new organization
   */
  def createOrganization() = AuthorizedAction { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_ORGANISATION)
    authorizationManager.canCreateOrganisation(request, organization, otherOrganisation)

    if (organization.template && !organizationManager.isTemplateNameUnique(organization.templateName.get, organization.community, None)) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
    } else {
      organizationManager.createOrganization(organization, otherOrganisation)
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Updates organization
   */
  def updateOrganization(orgId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateOrganisation(request, orgId)
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LEGAL_NOTICE_ID)
    val errorTemplateId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ERROR_TEMPLATE_ID)
    val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_ORGANISATION)
    var template: Boolean = false
    var templateName: Option[String] = None
    if (Configurations.REGISTRATION_ENABLED) {
      template = ParameterExtractor.requiredBodyParameter(request, Parameters.TEMPLATE).toBoolean
      templateName = ParameterExtractor.optionalBodyParameter(request, Parameters.TEMPLATE_NAME)
    }
    if (template && !organizationManager.isTemplateNameUnique(templateName.get, organizationManager.getById(orgId).get.community, Some(orgId))) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
    } else {
      organizationManager.updateOrganization(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, template, templateName)
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Deletes organization by id
   */
  def deleteOrganization(orgId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteOrganisation(request, orgId)
    organizationManager.deleteOrganization(orgId)
    ResponseConstructor.constructEmptyResponse
  }

}