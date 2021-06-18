package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, OrganizationManager, TestResultManager, UserManager}
import models.prerequisites.PrerequisiteUtil
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import utils.JsonUtil

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, organizationManager: OrganizationManager, userManager: UserManager, authorizationManager: AuthorizationManager, testResultManager: TestResultManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[OrganizationService])

  /**
   * Gets all organizations except the default organization for system administrators
   */
  def getOrganizations() = authorizedAction { request =>
    authorizationManager.canViewAllOrganisations(request)
    val list = organizationManager.getOrganizations()
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organization with specified id
   */
  def getOrganizationById(orgId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisation(request, orgId)
    val organization = organizationManager.getOrganizationById(orgId)
    val json: String = JsonUtil.jsOrganization(organization).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getOrganizationBySystemId(systemId: Long) = authorizedAction { request =>
    authorizationManager.canViewSystem(request, systemId)

    val organization = organizationManager.getOrganizationBySystemId(systemId)
    val json: String = JsonUtil.jsOrganization(organization).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisationsByCommunity(request, communityId)
    val list = organizationManager.getOrganizationsByCommunity(communityId)
    val json: String = JsonUtil.jsOrganizations(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new organization
   */
  def createOrganization() = authorizedAction { request =>
    val organization = ParameterExtractor.extractOrganizationInfo(request)
    val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_ORGANISATION)

    authorizationManager.canCreateOrganisation(request, organization, otherOrganisation)

    val copyOrganisationParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS).getOrElse("false").toBoolean
    val copySystemParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
    val copyStatementParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean

    val values = ParameterExtractor.extractOrganisationParameterValues(request, Parameters.PROPERTIES, true)
    var response: Result = ParameterExtractor.checkOrganisationParameterValues(values)
    if (response == null) {
      if (organization.template && !organizationManager.isTemplateNameUnique(organization.templateName.get, organization.community, None)) {
        response = ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
      } else {
        organizationManager.createOrganization(organization, otherOrganisation, values, copyOrganisationParameters, copySystemParameters, copyStatementParameters)
        response = ResponseConstructor.constructEmptyResponse
      }
    }
    response
  }

  /**
   * Updates organization
   */
  def updateOrganization(orgId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateOrganisation(request, orgId)
    val shortName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val fullName = ParameterExtractor.requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LEGAL_NOTICE_ID)
    val errorTemplateId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ERROR_TEMPLATE_ID)
    val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_ORGANISATION)
    val copyOrganisationParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS).getOrElse("false").toBoolean
    val copySystemParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
    val copyStatementParameters = ParameterExtractor.optionalBodyParameter(request, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean
    val values = ParameterExtractor.extractOrganisationParameterValues(request, Parameters.PROPERTIES, true)
    var response: Result = ParameterExtractor.checkOrganisationParameterValues(values)
    if (response == null) {
      var template: Boolean = false
      var templateName: Option[String] = None
      if (Configurations.REGISTRATION_ENABLED) {
        template = ParameterExtractor.requiredBodyParameter(request, Parameters.TEMPLATE).toBoolean
        templateName = ParameterExtractor.optionalBodyParameter(request, Parameters.TEMPLATE_NAME)
      }
      if (template && !organizationManager.isTemplateNameUnique(templateName.get, organizationManager.getById(orgId).get.community, Some(orgId))) {
        response = ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
      } else {
        organizationManager.updateOrganization(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, template, templateName, values, copyOrganisationParameters, copySystemParameters, copyStatementParameters)
        response = ResponseConstructor.constructEmptyResponse
      }
    }
    response
  }

  /**
   * Deletes organization by id
   */
  def deleteOrganization(orgId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteOrganisation(request, orgId)
    organizationManager.deleteOrganizationWrapper(orgId)
    ResponseConstructor.constructEmptyResponse
  }

  def getOwnOrganisationParameterValues() = authorizedAction { request =>
    authorizationManager.canViewOwnOrganisation(request)
    val user = userManager.getById(ParameterExtractor.extractUserId(request))
    val values = organizationManager.getOrganisationParameterValues(user.organization)
    val json: String = JsonUtil.jsOrganisationParametersWithValues(values, true).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getOrganisationParameterValues(orgId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisation(request, orgId)
    val values = organizationManager.getOrganisationParameterValues(orgId)
    val json: String = JsonUtil.jsOrganisationParametersWithValues(values, includeValues = true).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def checkOrganisationParameterValues(orgId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisation(request, orgId)
    val valuesWithValidPrerequisites = PrerequisiteUtil.withValidPrerequisites(organizationManager.getOrganisationParameterValues(orgId))
    val json: String = JsonUtil.jsOrganisationParametersWithValues(valuesWithValidPrerequisites, includeValues = false).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def updateOrganisationParameterValues(orgId: Long) = authorizedAction { request =>
    authorizationManager.canManageOrganisationBasic(request, orgId)
    val userId = ParameterExtractor.extractUserId(request)
    val values = ParameterExtractor.extractOrganisationParameterValues(request, Parameters.VALUES, false)
    var response: Result = ParameterExtractor.checkOrganisationParameterValues(values)
    if (response == null) {
      organizationManager.saveOrganisationParameterValuesWrapper(userId, orgId, values.get)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  def ownOrganisationHasTests() = authorizedAction { request =>
    authorizationManager.canViewOwnOrganisation(request)
    val userId = ParameterExtractor.extractUserId(request)
    val hasTests = testResultManager.testSessionsExistForUserOrganisation(userId)
    ResponseConstructor.constructJsonResponse(Json.obj("hasTests" -> hasTests).toString())
  }

}