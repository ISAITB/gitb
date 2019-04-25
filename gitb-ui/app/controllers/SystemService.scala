package controllers

import controllers.util._
import exceptions._
import javax.inject.Inject
import managers.{ParameterManager, SystemManager, TestCaseManager}
import models.Systems
import org.slf4j._
import play.api.mvc._
import utils.{JsonUtil, MimeUtil}

import scala.collection.mutable.ListBuffer

class SystemService @Inject() (systemManager: SystemManager, parameterManager: ParameterManager, testCaseManager: TestCaseManager) extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemService])

  def deleteSystem(systemId:Long) = Action.apply { request =>
    val organisationId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    systemManager.deleteSystemWrapper(systemId)
    // Return updated list of systems
    val systems = systemManager.getSystemsByOrganization(organisationId)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def registerSystemWithOrganization = Action.apply { request =>
    val system:Systems = ParameterExtractor.extractSystemWithOrganizationInfo(request)
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
  def updateSystemProfile(sut_id:Long) = Action.apply { request =>
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
  def getSystemProfile(sut_id:Long) = Action.apply { request =>
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
  def defineConformanceStatement(sut_id:Long) = Action.apply { request =>
    val spec:Long  = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
    val actor = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR).toLong
	  val optionIds = ParameterExtractor.optionalBodyParameter(request, Parameters.OPTIONS) match {
	    case Some(str) => Some(str.split(",").map(_.toLong).toList)
	    case _ => None
	  }

    val matchingStatements = systemManager.getConformanceStatements(sut_id, Some(spec), Some(actor))
    if (matchingStatements.isEmpty) {
      systemManager.defineConformanceStatementWrapper(sut_id, spec, actor, optionIds)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.CONFORMANCE_STATEMENT_EXISTS, "This conformance statement is already defined.")
    }
  }

	def getConformanceStatements(sut_id: Long) = Action.apply { request =>
    val specification = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SPEC)
    val actor = ParameterExtractor.optionalLongQueryParameter(request, Parameters.ACTOR)

		val conformanceStatements = systemManager.getConformanceStatements(sut_id, specification, actor)
    val json:String = JsonUtil.jsConformanceStatements(conformanceStatements).toString()
    ResponseConstructor.constructJsonResponse(json)
	}

	def deleteConformanceStatement(sut_id: Long) = Action.apply { request =>
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

	def getEndpointConfigurations(endpointId: Long) = Action.apply { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
	  val configs = systemManager.getEndpointConfigurations(endpointId, systemId)
    val json = JsonUtil.jsConfigs(configs).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteEndpointConfiguration(endpointId: Long) = Action.apply { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val parameterId = ParameterExtractor.requiredQueryParameter(request, Parameters.PARAMETER_ID).toLong
    systemManager.deleteEndpointConfiguration(systemId, parameterId, endpointId)
    ResponseConstructor.constructEmptyResponse
  }

  def saveEndpointConfiguration(endpointId: Long) = Action.apply { request =>
		val jsConfig = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
		val config = JsonUtil.parseJsConfig(jsConfig)
		systemManager.saveEndpointConfiguration(config)
    val parameter = parameterManager.getParameterById(config.parameter)
    if (parameter.isDefined && parameter.get.kind == "BINARY") {
      // Get the metadata for the parameter.
      val detectedMimeType = MimeUtil.getMimeType(config.value, false)
      val extension = MimeUtil.getExtensionFromMimeType(detectedMimeType)
      val json = JsonUtil.jsBinaryMetadata(detectedMimeType, extension).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
	}

	def getConfigurationsWithEndpointIds() = Action.apply { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
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

  def getSystemsByOrganization() = Action.apply { request =>
    val orgId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    val list = systemManager.getSystemsByOrganization(orgId)
    val json:String = JsonUtil.jsSystems(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getSystems() = Action.apply { request =>
    val systemIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    val systems = systemManager.getSystems(systemIds)
    val json = JsonUtil.jsSystems(systems).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

}