package controllers

import config.Configurations
import controllers.util._
import exceptions._
import javax.inject.Inject
import managers.{AuthorizationManager, ParameterManager, SystemManager, TestCaseManager}
import models.Systems
import org.apache.commons.codec.binary.Base64
import org.slf4j._
import play.api.mvc._
import utils.{ClamAVClient, JsonUtil, MimeUtil}

import scala.collection.mutable.ListBuffer

class SystemService @Inject() (systemManager: SystemManager, parameterManager: ParameterManager, testCaseManager: TestCaseManager, authorizationManager: AuthorizationManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemService])

  def deleteSystem(systemId:Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteSystem(request, systemId)
    val organisationId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    systemManager.deleteSystemWrapper(systemId)
    // Return updated list of systems
    val systems = systemManager.getSystemsByOrganization(organisationId)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def registerSystemWithOrganization = AuthorizedAction { request =>
    val system:Systems = ParameterExtractor.extractSystemWithOrganizationInfo(request)
    authorizationManager.canCreateSystem(request, system.owner)
    val otherSystem = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_SYSTEM)
    systemManager.registerSystemWrapper(system, otherSystem)
    // Return updated list of systems
    val systems = systemManager.getSystemsByOrganization(system.owner)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates the profile of a system
   */
  def updateSystemProfile(sut_id:Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateSystem(request, sut_id)
    val systemExists = systemManager.checkSystemExists(sut_id)
    if(systemExists) {
      val sname:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_SNAME)
      val fname:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_FNAME)
      val descr:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_DESC)
      val version:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_VERSION)
      val organisationId = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong
      val otherSystem = ParameterExtractor.optionalLongBodyParameter(request, Parameters.OTHER_SYSTEM)
      systemManager.updateSystemProfile(sut_id, sname, fname, descr, version, otherSystem)
      // Return updated list of systems
      val systems = systemManager.getSystemsByOrganization(organisationId)
      val json = JsonUtil.jsSystems(systems).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else{
      throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "System with ID '" + sut_id + "' not found")
    }
  }
  /**
   * Gets the system profile for the specific system
   */
  def getSystemProfile(sut_id:Long) = AuthorizedAction { request =>
    authorizationManager.canViewSystem(request, sut_id)
    val systemExists = systemManager.checkSystemExists(sut_id)
    if(systemExists) {
      val system = systemManager.getSystemProfile(sut_id)
      val json:String = JsonUtil.serializeSystem(system)
      ResponseConstructor.constructJsonResponse(json)
    } else{
      throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "System with ID '" + sut_id + "' not found")
    }
  }

  /**
   * Defines conformance statement for the system
   */
  def defineConformanceStatement(sut_id:Long) = AuthorizedAction { request =>
    authorizationManager.canCreateConformanceStatement(request, sut_id)
    val spec:Long  = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
    val actor = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR).toLong
	  val optionIds = ParameterExtractor.optionalBodyParameter(request, Parameters.OPTIONS) match {
	    case Some(str) => Some(str.split(",").map(_.toLong).toList)
	    case _ => None
	  }

    val matchingStatements = systemManager.getConformanceStatements(sut_id, Some(spec), Some(actor))
    if (matchingStatements.isEmpty) {
      if (systemManager.sutTestCasesExistForActor(actor)) {
        systemManager.defineConformanceStatementWrapper(sut_id, spec, actor, optionIds)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NO_SUT_TEST_CASES_FOR_ACTOR, "No test cases are defined for the selected actor.")
      }
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CONFORMANCE_STATEMENT_EXISTS, "This conformance statement is already defined.")
    }
  }

	def getConformanceStatements(sut_id: Long) = AuthorizedAction { request =>
    authorizationManager.canViewConformanceStatements(request, sut_id)
    val specification = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SPEC)
    val actor = ParameterExtractor.optionalLongQueryParameter(request, Parameters.ACTOR)

		val conformanceStatements = systemManager.getConformanceStatements(sut_id, specification, actor)
    val json:String = JsonUtil.jsConformanceStatements(conformanceStatements).toString()
    ResponseConstructor.constructJsonResponse(json)
	}

	def deleteConformanceStatement(sut_id: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteConformanceStatement(request, sut_id)
		ParameterExtractor.extractLongIdsQueryParameter(request) match {
			case Some(actorIds) => {
				if(actorIds.size == 0) {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter should be non-empty")
				} else {
					systemManager.deleteConformanceStatmentsWrapper(sut_id, actorIds)
          ResponseConstructor.constructEmptyResponse
				}
			}
			case None =>
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter is missing")
		}

	}

	def getEndpointConfigurations(endpointId: Long) = AuthorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurations(request, systemId, endpointId)
	  val configs = systemManager.getEndpointConfigurations(endpointId, systemId)
    val json = JsonUtil.jsConfigs(configs).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteEndpointConfiguration(endpointId: Long) = AuthorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canDeleteEndpointConfiguration(request, systemId, endpointId)
    val parameterId = ParameterExtractor.requiredQueryParameter(request, Parameters.PARAMETER_ID).toLong
    systemManager.deleteEndpointConfiguration(systemId, parameterId, endpointId)
    ResponseConstructor.constructEmptyResponse
  }

  def saveEndpointConfiguration(endpointId: Long) = AuthorizedAction { request =>
		val jsConfig = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val config = JsonUtil.parseJsConfig(jsConfig)
    authorizationManager.canEditEndpointConfiguration(request, config)

    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED && MimeUtil.isDataURL(config.value)) {
      // Check for virus. Do this regardless of the type of parameter as this can be changed later on.
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      val scanResult = virusScanner.scan(Base64.decodeBase64(MimeUtil.getBase64FromDataURL(config.value)))
      if (!ClamAVClient.isCleanReply(scanResult)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Configuration file failed virus scan.")
      }
    }
    if (response == null) {
      systemManager.saveEndpointConfiguration(config)
      val parameter = parameterManager.getParameterById(config.parameter)
      if (parameter.isDefined && parameter.get.kind == "BINARY") {
        // Get the metadata for the parameter.
        val detectedMimeType = MimeUtil.getMimeType(config.value, false)
        val extension = MimeUtil.getExtensionFromMimeType(detectedMimeType)
        val json = JsonUtil.jsBinaryMetadata(detectedMimeType, extension).toString()
        response = ResponseConstructor.constructJsonResponse(json)
      } else {
        response = ResponseConstructor.constructEmptyResponse
      }
    }
    response
	}

	def getConfigurationsWithEndpointIds() = AuthorizedAction { request =>
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, system)
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
		ids match {
			case Some(list) => {
        val configurations = systemManager.getConfigurationsWithEndpointIds(system, list)
        // Add mime-type and file name extension for binary ones.
        val configsToReturn = new ListBuffer[models.Config]
        configurations.foreach { config =>
          // config.
          if (MimeUtil.isDataURL(config.value)) {
            val detectedMimeType = MimeUtil.getMimeTypeFromDataURL(config.value)
            val extension = MimeUtil.getExtensionFromMimeType(detectedMimeType);
            configsToReturn += new models.Config(config, detectedMimeType, extension)
          } else {
            configsToReturn += new models.Config(config, null, null)
          }
        }
        val json = JsonUtil.jsConfigList(configsToReturn.toList).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
			case None =>
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, Parameters.IDS+" parameter is missing")
		}
	}

  def getSystemsByOrganization() = AuthorizedAction { request =>
    val orgId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    authorizationManager.canViewSystems(request, orgId)
    val list = systemManager.getSystemsByOrganization(orgId)
    val json:String = JsonUtil.jsSystems(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getSystemsByCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewSystemsByCommunityId(request, communityId)
    val list = systemManager.getSystemsByCommunity(communityId)
    val json:String = JsonUtil.jsSystems(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getSystems() = AuthorizedAction { request =>
    val systemIds = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewSystemsById(request, systemIds)

    val systems = systemManager.getSystems(systemIds)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

}