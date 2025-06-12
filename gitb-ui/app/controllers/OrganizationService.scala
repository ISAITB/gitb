package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, OrganizationManager, TestResultManager, UserManager}
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{JsonUtil, RepositoryUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService @Inject() (repositoryUtils: RepositoryUtils,
                                     authorizedAction: AuthorizedAction,
                                     cc: ControllerComponents,
                                     organizationManager: OrganizationManager,
                                     userManager: UserManager,
                                     authorizationManager: AuthorizationManager,
                                     testResultManager: TestResultManager)
                                    (implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Gets all organizations except the default organization for system administrators
   */
  def getOrganizations(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewAllOrganisations(request).flatMap { _ =>
      organizationManager.getOrganizations().map { list =>
        val json: String = JsonUtil.jsOrganizations(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the organization with specified id
   */
  def getOrganizationById(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisation(request, orgId).flatMap { _ =>
      organizationManager.getOrganizationById(orgId).map { organization =>
        val json: String = JsonUtil.jsOrganization(organization).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getOrganizationBySystemId(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, systemId).flatMap { _ =>
      organizationManager.getOrganizationBySystemId(systemId).map { organization =>
        val json: String = JsonUtil.jsOrganization(organization).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisationsByCommunity(request, communityId).flatMap { _ =>
      val includeAdmin = ParameterExtractor.optionalQueryParameter(request, Parameters.ADMIN).exists(_.toBoolean)
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      organizationManager.getOrganizationsByCommunity(communityId, includeAdmin, snapshotId).map { list =>
        val json: String = JsonUtil.jsOrganizations(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchOrganizations(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewAllOrganisations(request).flatMap { _ =>
      val communityIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.COMMUNITY_IDS)
      organizationManager.searchOrganizations(communityIds).map { list =>
        val json: String = JsonUtil.jsOrganizations(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the organizations with specified community
   */
  def searchOrganizationsByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisationsByCommunity(request, communityId).flatMap { _ =>
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
      organizationManager.searchOrganizationsByCommunity(communityId, page, limit, filter, sortOrder, sortColumn, creationOrderSort).map { result =>
        val json: String = JsonUtil.jsOrganizationSearchResults(result._1, result._2).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization(): Action[AnyContent] = authorizedAction.async { request =>
    val paramMap = ParameterExtractor.paramMap(request)
    val organization = ParameterExtractor.extractOrganizationInfo(paramMap)
    val otherOrganisation = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_ORGANISATION)
    authorizationManager.canCreateOrganisation(request, organization, otherOrganisation).flatMap { _ =>
      val copyOrganisationParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.ORGANISATION_PARAMETERS).getOrElse("false").toBoolean
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean

      val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        for {
          error <- {
            if (organization.template) {
              organizationManager.isTemplateNameUnique(organization.templateName.get, organization.community, None).map { isUnique =>
                if (isUnique) {
                  None
                } else {
                  Some(ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.", Some("template")))
                }
              }
            } else {
              Future.successful(None)
            }
          }
          result <- {
            if (error.isEmpty) {
              organizationManager.createOrganization(organization, otherOrganisation, values, Some(files), copyOrganisationParameters, copySystemParameters, copyStatementParameters).map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            } else {
              Future.successful(error.get)
            }
          }
        } yield result
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Updates organization
   */
  def updateOrganization(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateOrganisation(request, orgId).flatMap { _ =>
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
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        for {
          templateInfo <- {
            var template: Boolean = false
            var templateName: Option[String] = None
            if (Configurations.REGISTRATION_ENABLED) {
              template = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TEMPLATE).toBoolean
              templateName = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.TEMPLATE_NAME)
            }
            Future.successful((template, templateName))
          }
          error <- {
            if (templateInfo._1) {
              organizationManager.getById(orgId).flatMap { organisation =>
                organizationManager.isTemplateNameUnique(templateInfo._2.get, organisation.get.community, Some(orgId)).map { isUnique =>
                  if (isUnique) {
                    None
                  } else {
                    Some(ResponseConstructor.constructErrorResponse(ErrorCodes.DUPLICATE_ORGANISATION_TEMPLATE, "The provided template name is already in use.", Some("template")))
                  }
                }
              }
            } else {
              Future.successful(None)
            }
          }
          result <- {
            if (error.isEmpty) {
              organizationManager.updateOrganization(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, templateInfo._1, templateInfo._2, values, Some(files), copyOrganisationParameters, copySystemParameters, copyStatementParameters).map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            } else {
              Future.successful(error.get)
            }
          }
        } yield result
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Deletes organization by id
   */
  def deleteOrganization(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteOrganisation(request, orgId).flatMap { _ =>
      organizationManager.deleteOrganizationWrapper(orgId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def getOwnOrganisationParameterValues(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnOrganisation(request).flatMap { _ =>
      userManager.getById(ParameterExtractor.extractUserId(request)).flatMap { user =>
        organizationManager.getOrganisationParameterValues(user.organization).map { values =>
          val json: String = JsonUtil.jsOrganisationParametersWithValues(values, includeValues = true).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def getOrganisationParameterValues(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisation(request, orgId).flatMap { _ =>
      val onlySimple = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SIMPLE)
      organizationManager.getOrganisationParameterValues(orgId, onlySimple).map { values =>
        val json: String = JsonUtil.jsOrganisationParametersWithValues(values, includeValues = true).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def checkOrganisationParameterValues(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisation(request, orgId).flatMap { _ =>
      organizationManager.getOrganisationParameterValues(orgId).map { values =>
        val valuesWithValidPrerequisites = PrerequisiteUtil.withValidPrerequisites(values)
        val json: String = JsonUtil.jsOrganisationParametersWithValues(valuesWithValidPrerequisites, includeValues = false).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def updateOrganisationParameterValues(orgId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val task = for {
      _ <- authorizationManager.canManageOrganisationBasic(request, orgId)
      result <- {
        val paramMap = ParameterExtractor.paramMap(request)
        val userId = ParameterExtractor.extractUserId(request)
        val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.VALUES, optional = false)
        val files = ParameterExtractor.extractFiles(request).map {
          case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
        }
        if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
          }
        } else {
          organizationManager.saveOrganisationParameterValuesWrapper(userId, orgId, values.get, files).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def downloadOrganisationParameterFile(orgId: Long, parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisation(request, orgId).map { _ =>
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
  }

  def ownOrganisationHasTests(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnOrganisation(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      testResultManager.testSessionsExistForUserOrganisation(userId).map { hasTests =>
        ResponseConstructor.constructJsonResponse(Json.obj("hasTests" -> hasTests).toString())
      }
    }
  }

  def updateOrganisationApiKey(organisationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateOrganisationApiKey(request, organisationId).flatMap { _ =>
      organizationManager.updateOrganisationApiKey(organisationId).map { newApiKey =>
        ResponseConstructor.constructStringResponse(newApiKey)
      }
    }
  }

  def deleteOrganisationApiKey(organisationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateOrganisationApiKey(request, organisationId).flatMap { _ =>
      organizationManager.deleteOrganisationApiKey(organisationId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def getAutomationKeysForOrganisation(organisationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    for {
      _ <- {
        if (snapshotId.isEmpty) {
          authorizationManager.canViewOrganisationAutomationKeys(request, organisationId)
        } else {
          authorizationManager.canViewOrganisationAutomationKeysInSnapshot(request, organisationId, snapshotId.get)
        }
      }
      result <- organizationManager.getAutomationKeysForOrganisation(organisationId, snapshotId).map { apiKeyInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKeyInfo(apiKeyInfo).toString)
      }
    } yield result
  }

}