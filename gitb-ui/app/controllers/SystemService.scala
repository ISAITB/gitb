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
import scala.concurrent.ExecutionContext

class SystemService @Inject() (implicit ec: ExecutionContext, repositoryUtils: RepositoryUtils, testResultManager: TestResultManager, accountManager: AccountManager, authorizedAction: AuthorizedAction, cc: ControllerComponents, systemManager: SystemManager, parameterManager: ParameterManager, testCaseManager: TestCaseManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteSystem(systemId:Long) = authorizedAction { request =>
    authorizationManager.canDeleteSystem(request, systemId)
    systemManager.deleteSystemWrapper(systemId)
    ResponseConstructor.constructEmptyResponse
  }

  def registerSystemWithOrganization = authorizedAction { request =>
    try {
      val paramMap = ParameterExtractor.paramMap(request)
      val userId = ParameterExtractor.extractUserId(request)
      val system:Systems = ParameterExtractor.extractSystemWithOrganizationInfo(paramMap)
      authorizationManager.canCreateSystem(request, system.owner)
      val otherSystem = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.OTHER_SYSTEM)
      val copySystemParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.SYSTEM_PARAMETERS).getOrElse("false").toBoolean
      val copyStatementParameters = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.STATEMENT_PARAMETERS).getOrElse("false").toBoolean
      val values = ParameterExtractor.extractSystemParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
      val files = ParameterExtractor.extractFiles(request).map {
        case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
      }

      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        systemManager.registerSystemWrapper(userId, system, otherSystem, values, Some(files), copySystemParameters, copyStatementParameters)
        ResponseConstructor.constructEmptyResponse
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Updates the profile of a system
   */
  def updateSystemProfile(sut_id:Long) = authorizedAction { request =>
    try {
      authorizationManager.canUpdateSystem(request, sut_id)
      val systemExists = systemManager.checkSystemExists(sut_id)
      if (systemExists) {
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
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        } else {
          val userId = ParameterExtractor.extractUserId(request)
          systemManager.updateSystemProfile(userId, sut_id, sname, fname, descr, version, otherSystem, values, Some(files), copySystemParameters, copyStatementParameters)
          ResponseConstructor.constructEmptyResponse
        }
      } else{
        throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, models.Enums.LabelType.System) + " with ID '" + sut_id + "' not found.")
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def getSystemById(systemId: Long) = authorizedAction { request =>
    authorizationManager.canViewSystem(request, systemId)
    val system = systemManager.getSystemById(systemId)
    if (system.isDefined) {
      ResponseConstructor.constructJsonResponse(JsonUtil.jsSystem(system.get).toString())
    } else {
      throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, models.Enums.LabelType.System)+ " with ID '" + systemId + "' not found.")
    }
  }

  /**
   * Gets the system profile for the specific system
   */
  def getSystemProfile(sut_id:Long) = authorizedAction { request =>
    authorizationManager.canViewSystem(request, sut_id)
    val systemExists = systemManager.checkSystemExists(sut_id)
    if(systemExists) {
      val system = systemManager.getSystemProfile(sut_id)
      val json:String = JsonUtil.serializeSystem(system)
      ResponseConstructor.constructJsonResponse(json)
    } else{
      throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, models.Enums.LabelType.System)+ " with ID '" + sut_id + "' not found.")
    }
  }

  /**
   * Defines conformance statement for the system
   */
  def defineConformanceStatements(systemId:Long) = authorizedAction { request =>
    authorizationManager.canCreateConformanceStatement(request, systemId)
    val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request)
    systemManager.defineConformanceStatements(systemId, actorIds.getOrElse(List.empty))
    ResponseConstructor.constructEmptyResponse
  }

	def deleteConformanceStatement(sut_id: Long) = authorizedAction { request =>
    val actorIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canDeleteConformanceStatement(request, sut_id, actorIds)
    actorIds match {
			case Some(actorIds) =>
        if (actorIds.isEmpty) {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter should be non-empty")
        } else {
          systemManager.deleteConformanceStatementsWrapper(sut_id, actorIds)
          ResponseConstructor.constructEmptyResponse
        }
      case None =>
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter is missing")
		}

	}

  def downloadEndpointConfigurationFile() = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, systemId)
    val parameterId = ParameterExtractor.requiredQueryParameter(request, Parameters.PARAMETER_ID).toLong
    val file = repositoryUtils.getStatementParameterFile(parameterId, systemId)
    if (file.exists()) {
      Ok.sendFile(
        content = file,
        inline = false
      )
    } else {
      ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "Parameter was not found")
    }
  }

  def getSystemsByOrganization() = authorizedAction { request =>
    val orgId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewSystems(request, orgId, snapshotId)
    val list = systemManager.getSystemsByOrganization(orgId, snapshotId)
    val checkIfHasTests = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.CHECK_HAS_TESTS)
    var systemsWithTests: Option[Set[Long]] = None
    if (checkIfHasTests.isDefined && checkIfHasTests.get) {
      systemsWithTests = Some(systemManager.checkIfSystemsHaveTests(list.map(x => x.id).toSet))
    }
    val json:String = JsonUtil.jsSystems(list, systemsWithTests).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getSystems() = authorizedAction { request =>
    val systemIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewSystemsById(request, systemIds)
    val systems = systemManager.getSystems(systemIds)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def searchSystems() = authorizedAction { request =>
    authorizationManager.canViewSystemsById(request, None)
    val communityIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.COMMUNITY_IDS)
    val organisationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ORG_IDS)
    val systems = systemManager.searchSystems(communityIds, organisationIds)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def searchSystemsInCommunity() = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewSystemsByCommunityId(request, communityId)
    val organisationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ORG_IDS)
    val systems = systemManager.searchSystems(Some(List(communityId)), organisationIds)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getSystemParameterValues(systemId: Long) = authorizedAction { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId)))
    val onlySimple = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.SIMPLE)
    val values = systemManager.getSystemParameterValues(systemId, onlySimple)
    val json: String = JsonUtil.jsSystemParametersWithValues(values, includeValues = true).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def checkSystemParameterValues(systemId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId)))
    val valuesWithValidPrerequisites = PrerequisiteUtil.withValidPrerequisites(systemManager.getSystemParameterValues(systemId))
    val json: String = JsonUtil.jsSystemParametersWithValues(valuesWithValidPrerequisites, includeValues = false).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def downloadSystemParameterFile(systemId: Long, parameterId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewSystemsById(request, Some(List(systemId)))
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

  def updateSystemApiKey(systemId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateSystemApiKey(request, systemId)
    val newApiKey = systemManager.updateSystemApiKey(systemId)
    ResponseConstructor.constructStringResponse(newApiKey)
  }

  def ownSystemHasTests(systemId: Long) = authorizedAction { request =>
    authorizationManager.canViewSystem(request, systemId)
    val hasTests = testResultManager.testSessionsExistForSystem(systemId)
    ResponseConstructor.constructJsonResponse(Json.obj("hasTests" -> hasTests).toString())
  }

}