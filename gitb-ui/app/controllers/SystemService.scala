package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.slf4j._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import scala.concurrent.Future
import controllers.util._
import managers.{TestCaseManager, SystemManager}
import exceptions._
import utils.JsonUtil
import models.{Config, Systems}

class SystemService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemService])

  /**
   * Register a system for the authenticated vendor
   */
  def registerSystem = Action.async { request =>
    Future{
      val adminId:Long = ParameterExtractor.extractUserId(request)
      val system:Systems = ParameterExtractor.extractSystemInfo(request)

      SystemManager.registerSystem(adminId, system)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def registerSystemWithOrganization = Action.async { request =>
    Future{
      val system:Systems = ParameterExtractor.extractSystemWithOrganizationInfo(request)

      SystemManager.registerSystem(system)
      ResponseConstructor.constructEmptyResponse
    }
  }


  /**
   * Updates the profile of a system
   */
  def updateSystemProfile(sut_id:Long) = Action.async { request =>
    SystemManager.checkSystemExists(sut_id) map { systemExists =>
      if(systemExists) {
        val sname:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_SNAME)
        val fname:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_FNAME)
        val descr:Option[String]   = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_DESC)
        val version:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_VERSION)

        SystemManager.updateSystemProfile(sut_id, sname, fname, descr, version)
        ResponseConstructor.constructEmptyResponse
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "System with ID '" + sut_id + "' not found")
      }
    }
  }
  /**
   * Gets the system profile for the specific system
   */
  def getSystemProfile(sut_id:Long) = Action.async { request =>
    SystemManager.checkSystemExists(sut_id) flatMap { systemExists =>
      if(systemExists) {
        SystemManager.getSystemProfile(sut_id) map { system =>
          val json:String = JsonUtil.serializeSystem(system)
          ResponseConstructor.constructJsonResponse(json)
        }
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "System with ID '" + sut_id + "' not found")
      }
    }
  }
  /**
   * Assigns an administrator or tester for the system
   */
  def assignSystemAdminOrTester(sut_id:Long) = Action { request =>
    logger.info("suts/" + sut_id + "/assign service")
    Ok("suts/" + sut_id + "/assign")
  }
  /**
   * Defines conformance statement for the system
   */
  def defineConformanceStatement(sut_id:Long) = Action.async { request =>
    val spec:Long  = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
    val actor = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR).toLong
	  val optionIds = ParameterExtractor.optionalBodyParameter(request, Parameters.OPTIONS) match {
	    case Some(str) => Some(str.split(",").map(_.toLong).toList)
	    case _ => None
	  }


    SystemManager.defineConformanceStatement(sut_id, spec, actor, optionIds) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

	def getConformanceStatements(sut_id: Long) = Action.async { request =>
    val specification = ParameterExtractor.optionalQueryParameter(request, Parameters.SPEC)
    val actor = ParameterExtractor.optionalQueryParameter(request, Parameters.ACTOR)

		SystemManager.getConformanceStatements(sut_id, specification, actor) map { conformanceStatements =>
			val json:String = JsonUtil.jsConformanceStatements(conformanceStatements).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def deleteConformanceStatement(sut_id: Long) = Action.async { request =>
		ParameterExtractor.extractLongIdsQueryParameter(request) match {
			case Some(actorIds) => {
				if(actorIds.size == 0) {
					Future {
						ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter should be non-empty")
					}
				} else {
					SystemManager.deleteConformanceStatments(sut_id, actorIds) map { unit =>
						ResponseConstructor.constructEmptyResponse
					}
				}
			}
			case None => Future {
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "'ids' parameter is missing")
			}
		}

	}

	def getLastExecutionResultsForTestCases(sut_id:Long) = Action.async { request =>
		val testCaseIdsParam = ParameterExtractor.requiredQueryParameter(request, Parameters.IDS)
		val testCaseIds = testCaseIdsParam.split(",").map(_.toLong).toList

		TestCaseManager.getLastExecutionResultsForTestCases(sut_id, testCaseIds) map { results =>
			val json = JsonUtil.jsTestResultStatuses(testCaseIds, results).toString()

			ResponseConstructor.constructJsonResponse(json)
		}
	}

  def getLastExecutionResultsForTestSuite(sut_id:Long) = Action.async { request =>
    val testCaseIdsParam = ParameterExtractor.requiredQueryParameter(request, Parameters.IDS)
    val testCaseIds = testCaseIdsParam.split(",").map(_.toLong).toList
    val testSuiteId = ParameterExtractor.requiredQueryParameter(request, Parameters.ID).toLong

    TestCaseManager.getLastExecutionResultsForTestCases(sut_id, testCaseIds) map { results =>
      val json = JsonUtil.jsTestResultStatuses(testSuiteId, testCaseIds, results).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

	def getImplementedActors(sut_id:Long) = Action.async { request =>
		SystemManager.getImplementedActors(sut_id) map { actors =>
			val json:String = JsonUtil.jsActors(actors).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}
  /**
   * Overall Results for each test case that the system has performed
   */
  def getConformanceResults(sut_id:Long) = Action {
    logger.info("suts/" + sut_id + "/conformance/results")
    Ok("suts/" + sut_id + "/conformance/results")
  }

  /**
   * Gets required configurations for the system
   */
  /*def getSystemConfigurations(sut_id:Long) = Action.async { request =>
    SystemManager.getRequiredConfigurations(sut_id) map { configs =>
      val json:String = JsonUtil.jsConfigs(configs).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }*/
  /**
   * Updates required configurations for the system
   */
  /*def updateSystemConfigurations(sut_id:Long) = Action.async { request =>
    val jsConfigs:String  = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIGS)
    val configs:List[Config] = JsonUtil.parseJsConfigs(jsConfigs)
    SystemManager.saveConfigurations(configs) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }*/

	def getEndpointConfigurations(endpointId: Long) = Action.async { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
	  SystemManager.getEndpointConfigurations(endpointId, systemId) map { configs =>
		  val json = JsonUtil.jsConfigs(configs).toString()
		  ResponseConstructor.constructJsonResponse(json)
	  }
  }

	def saveEndpointConfiguration(endpointId: Long) = Action.async { request =>
		val jsConfig = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
		val config = JsonUtil.parseJsConfig(jsConfig)
		SystemManager.saveEndpointConfiguration(config) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def getConfigurationsWithEndpointIds() = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
		ids match {
			case Some(list) => SystemManager.getConfigurationsWithEndpointIds(system, list) map { configurations =>
				val json = JsonUtil.jsConfigs(configurations).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
			case None => Future {
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, Parameters.IDS+" parameter is missing")
			}
		}
	}

  def getSystemsByOrganization() = Action.async { request =>
    val orgId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    SystemManager.getSystemsByOrganization(orgId) map { list =>
      val json:String = JsonUtil.jsSystems(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Get the SUTs registered for the authenticated vendor
   */
  def getVendorSystems() = Action.async { request =>
    val userId:Long = ParameterExtractor.extractUserId(request)

    SystemManager.getVendorSystems(userId) map { list =>
      val json:String = JsonUtil.jsSystems(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def getSystems() = Action.async { request =>
    val systemIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    SystemManager.getSystems(systemIds) map { systems =>
      val json = JsonUtil.jsSystems(systems).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

}