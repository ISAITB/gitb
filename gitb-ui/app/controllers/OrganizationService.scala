package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes

import javax.inject.Inject
import managers.{AuthorizationManager, OrganizationManager, TestResultManager, UserManager}
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import utils.{JsonUtil, RepositoryUtils}

import scala.concurrent.ExecutionContext

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService @Inject() (implicit ec: ExecutionContext, repositoryUtils: RepositoryUtils, authorizedAction: AuthorizedAction, cc: ControllerComponents, organizationManager: OrganizationManager, userManager: UserManager, authorizationManager: AuthorizationManager, testResultManager: TestResultManager) extends AbstractController(cc) {
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
   * Gets the organizations with specified community
   */
  def searchOrganizationsByCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisationsByCommunity(request, communityId)

    val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
    val sortOrder = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_ORDER)
    val sortColumn = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_COLUMN)
    val page = ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE) match {
      case Some(v) => v.toLong
      case None => 1L
    }
    val limit = ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT) match {
      case Some(v) => v.toLong
      case None => 10L
    }
    var creationOrderSort: Option[String] = None
    val creationOrderSortParam = ParameterExtractor.optionalQueryParameter(request, Parameters.CREATION_ORDER_SORT).getOrElse("none")
    if (!creationOrderSortParam.equals("none")) {
      creationOrderSort = Some(creationOrderSortParam)
    }
    val result = organizationManager.searchOrganizationsByCommunity(communityId, page, limit, filter, sortOrder, sortColumn, creationOrderSort)
    val json: String = JsonUtil.jsOrganizationSearchResults(result._1, result._2).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new organization
   */
  def createOrganization() = authorizedAction { request =>
    try {
      val paramMap = ParameterExtractor.paramMap(request)
      val organization = ParameterExtractor.extractOrganizationInfo(paramMap)
      val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_ORGANISATION)
      authorizationManager.canCreateOrganisation(request, organization, otherOrganisation)

      val copyOrganisationParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.ORGANISATION_PARAMETERS).getOrElse("false").toBoolean
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean

      val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        if (organization.template && !organizationManager.isTemplateNameUnique(organization.templateName.get, organization.community, None)) {
          ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
        } else {
          organizationManager.createOrganization(organization, otherOrganisation, values, Some(files), copyOrganisationParameters, copySystemParameters, copyStatementParameters)
          ResponseConstructor.constructEmptyResponse
        }
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Updates organization
   */
  def updateOrganization(orgId: Long) = authorizedAction { request =>
    try {
      authorizationManager.canUpdateOrganisation(request, orgId)
      val paramMap = ParameterExtractor.paramMap(request)
      val shortName = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.VENDOR_SNAME)
      val fullName = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.VENDOR_FNAME)
      val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.LANDING_PAGE_ID)
      val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.LEGAL_NOTICE_ID)
      val errorTemplateId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.ERROR_TEMPLATE_ID)
      val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_ORGANISATION)
      val copyOrganisationParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.ORGANISATION_PARAMETERS).getOrElse("false").toBoolean
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean
      val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        var template: Boolean = false
        var templateName: Option[String] = None
        if (Configurations.REGISTRATION_ENABLED) {
          template = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TEMPLATE).toBoolean
          templateName = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.TEMPLATE_NAME)
        }
        if (template && !organizationManager.isTemplateNameUnique(templateName.get, organizationManager.getById(orgId).get.community, Some(orgId))) {
          ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.")
        } else {
          organizationManager.updateOrganization(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, template, templateName, values, Some(files), copyOrganisationParameters, copySystemParameters, copyStatementParameters)
          ResponseConstructor.constructEmptyResponse
        }
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
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
    try {
      authorizationManager.canManageOrganisationBasic(request, orgId)
      val paramMap = ParameterExtractor.paramMap(request)
      val userId = ParameterExtractor.extractUserId(request)
      val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.VALUES, optional = false)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        organizationManager.saveOrganisationParameterValuesWrapper(userId, orgId, values.get, files)
        ResponseConstructor.constructEmptyResponse
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def downloadOrganisationParameterFile(orgId: Long, parameterId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisation(request, orgId)
    val file = repositoryUtils.getOrganisationPropertyFile(parameterId, orgId)
    if (file.exists()) {
      Ok.sendFile(
        content = file,
        inline = false
      )
    } else {
      ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "Property was not found")
    }
  }

  def ownOrganisationHasTests() = authorizedAction { request =>
    authorizationManager.canViewOwnOrganisation(request)
    val userId = ParameterExtractor.extractUserId(request)
    val hasTests = testResultManager.testSessionsExistForUserOrganisation(userId)
    ResponseConstructor.constructJsonResponse(Json.obj("hasTests" -> hasTests).toString())
  }

  def updateOrganisationApiKey(organisationId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateOrganisationApiKey(request, organisationId)
    val newApiKey = organizationManager.updateOrganisationApiKey(organisationId)
    ResponseConstructor.constructStringResponse(newApiKey)
  }

  def deleteOrganisationApiKey(organisationId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateOrganisationApiKey(request, organisationId)
    organizationManager.deleteOrganisationApiKey(organisationId)
    ResponseConstructor.constructEmptyResponse
  }

  def getAutomationKeysForOrganisation(organisationId: Long) = authorizedAction { request =>
    authorizationManager.canViewOrganisationAutomationKeys(request, organisationId)
    val result = organizationManager.getAutomationKeysForOrganisation(organisationId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKeyInfo(result).toString)
  }

}