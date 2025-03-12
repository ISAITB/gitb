package controllers

import config.Configurations
import controllers.util._
import exceptions._
import managers._
import models.Systems
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import play.api.mvc._
import utils.{JsonUtil, RepositoryUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SystemService @Inject() (repositoryUtils: RepositoryUtils,
                               testResultManager: TestResultManager,
                               authorizedAction: AuthorizedAction, cc:
                               ControllerComponents, systemManager: SystemManager,
                               authorizationManager: AuthorizationManager,
                               communityLabelManager: CommunityLabelManager)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def deleteSystem(systemId:Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteSystem(request, systemId).flatMap { _ =>
      systemManager.deleteSystemWrapper(systemId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def registerSystemWithOrganization: Action[AnyContent] = authorizedAction.async { request =>
    val paramMap = ParameterExtractor.paramMap(request)
    val userId = ParameterExtractor.extractUserId(request)
    val system:Systems = ParameterExtractor.extractSystemWithOrganizationInfo(paramMap)
    authorizationManager.canCreateSystem(request, system.owner).flatMap { _ =>
      val otherSystem = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_SYSTEM)
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean
      val values = ParameterExtractor.extractSystemParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        systemManager.registerSystemWrapper(userId, system, otherSystem, values, Some(files), copySystemParameters, copyStatementParameters).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Updates the profile of a system
   */
  def updateSystemProfile(sut_id:Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateSystem(request, sut_id).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val sname = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SYSTEM_SNAME)
      val fname = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SYSTEM_FNAME)
      val descr = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_DESC)
      val version = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_VERSION)
      val otherSystem = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_SYSTEM)
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean
      val values = ParameterExtractor.extractSystemParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        val userId = ParameterExtractor.extractUserId(request)
        systemManager.updateSystemProfile(userId, sut_id, sname, fname, descr, version, otherSystem, values, Some(files), copySystemParameters, copyStatementParameters).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def getSystemById(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, systemId).flatMap { _ =>
      systemManager.getSystemById(systemId).flatMap { system =>
        if (system.isDefined) {
          Future.successful {
            ResponseConstructor.constructJsonResponse(JsonUtil.jsSystem(system.get).toString())
          }
        } else {
          communityLabelManager.getLabel(request, models.Enums.LabelType.System).map { systemLabel =>
            throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, systemLabel + " with ID '" + systemId + "' not found.")
          }
        }
      }
    }
  }

  /**
   * Gets the system profile for the specific system
   */
  def getSystemProfile(sut_id:Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, sut_id).flatMap { _ =>
      systemManager.getSystemProfile(sut_id).map { system =>
        val json:String = JsonUtil.serializeSystem(system)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Defines conformance statement for the system
   */
  def defineConformanceStatements(systemId:Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateConformanceStatement(request, systemId).flatMap { _ =>
      val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request)
      systemManager.defineConformanceStatements(systemId, actorIds.getOrElse(List.empty)).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

	def deleteConformanceStatement(sut_id: Long): Action[AnyContent] = authorizedAction.async { request =>
    val actorIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canDeleteConformanceStatement(request, sut_id, actorIds).flatMap { _ =>
      actorIds match {
        case Some(actorIds) =>
          if (actorIds.isEmpty) {
            Future.successful {
              ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter should be non-empty")
            }
          } else {
            systemManager.deleteConformanceStatementsWrapper(sut_id, actorIds).map { _ =>
              ResponseConstructor.constructEmptyResponse
            }
          }
        case None =>
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter is missing")
          }
      }
		}
	}

  def downloadEndpointConfigurationFile(): Action[AnyContent] = authorizedAction.async { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, systemId).flatMap { _ =>
      val parameterId = ParameterExtractor.requiredQueryParameter(request, Parameters.PARAMETER_ID).toLong
      Future.successful {
        repositoryUtils.getStatementParameterFile(parameterId, systemId)
      }.map { file =>
        if (file.exists()) {
          Ok.sendFile(
            content = file,
            inline = false
          )
        } else {
          ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "Parameter was not found")
        }
      }
    }
  }

  def getSystemsByOrganization(): Action[AnyContent] = authorizedAction.async { request =>
    val orgId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewSystems(request, orgId, snapshotId).flatMap { _ =>
      systemManager.getSystemsByOrganization(orgId, snapshotId).flatMap { list =>
        val checkIfHasTests = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.CHECK_HAS_TESTS)
        val systemsWithTests = if (checkIfHasTests.isDefined && checkIfHasTests.get) {
          systemManager.checkIfSystemsHaveTests(list.map(x => x.id).toSet).map { hasTests =>
            Some(hasTests)
          }
        } else {
          Future.successful(None)
        }
        systemsWithTests.map { results =>
          val json:String = JsonUtil.jsSystems(list, results).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def getSystems(): Action[AnyContent] = authorizedAction.async { request =>
    val systemIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewSystemsById(request, systemIds).flatMap { _ =>
      systemManager.getSystems(systemIds).map { systems =>
        val json = JsonUtil.jsSystems(systems).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchSystems(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemsById(request, None).flatMap { _ =>
      val communityIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.COMMUNITY_IDS)
      val organisationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ORG_IDS)
      systemManager.searchSystems(communityIds, organisationIds).map { systems =>
        val json = JsonUtil.jsSystems(systems).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchSystemsInCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewSystemsByCommunityId(request, communityId).flatMap { _ =>
      val organisationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ORG_IDS)
      systemManager.searchSystems(Some(List(communityId)), organisationIds).map { systems =>
        val json = JsonUtil.jsSystems(systems).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getSystemParameterValues(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId))).flatMap { _ =>
      val onlySimple = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SIMPLE)
      systemManager.getSystemParameterValues(systemId, onlySimple).map { values =>
        val json: String = JsonUtil.jsSystemParametersWithValues(values, includeValues = true).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def checkSystemParameterValues(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId))).flatMap { _ =>
      systemManager.getSystemParameterValues(systemId).map { values =>
        val valuesWithValidPrerequisites = PrerequisiteUtil.withValidPrerequisites(values)
        val json: String = JsonUtil.jsSystemParametersWithValues(valuesWithValidPrerequisites, includeValues = false).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def downloadSystemParameterFile(systemId: Long, parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId))).map { _ =>
      val file = repositoryUtils.getSystemPropertyFile(parameterId, systemId)
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

  def updateSystemApiKey(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateSystemApiKey(request, systemId).flatMap { _ =>
      systemManager.updateSystemApiKey(systemId).map { newApiKey =>
        ResponseConstructor.constructStringResponse(newApiKey)
      }
    }
  }

  def ownSystemHasTests(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, systemId).flatMap { _ =>
      testResultManager.testSessionsExistForSystem(systemId).map { hasTests =>
        ResponseConstructor.constructJsonResponse(Json.obj("hasTests" -> hasTests).toString())
      }
    }
  }

}