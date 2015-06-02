package controllers

import exceptions.ErrorCodes
import org.apache.commons.lang.RandomStringUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import managers.{TestSuiteManager, TestCaseManager, ConformanceManager}
import utils.JsonUtil
import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}

import scala.concurrent.Future
import scala.reflect.io.File

class ConformanceService extends Controller {
	private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

	/**
	 * Gets the list of domains
	 */
	def getDomains = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
		ConformanceManager.getDomains(ids) map { result =>
			val json = JsonUtil.jsDomains(result).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets the list of specifications
	 */
	def getSpecs = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
		ConformanceManager.getSpecifications(ids) map { result =>
			val json = JsonUtil.jsSpecifications(result).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets the list of specifications
	 */
	def getActors = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
		ConformanceManager.getActors(ids) map { result =>
			val json = JsonUtil.jsActors(result).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets the specifications that are defined/tested in the platform
	 */
	def getDomainSpecs(domain_id: Long) = Action.async {
		ConformanceManager.getSpecifications(domain_id) map { specs =>
			val json = JsonUtil.jsSpecifications(specs).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets actors defined  for the spec
	 */
	def getSpecActors(spec_id: Long) = Action.async {
		ConformanceManager.getActorsWithSpecificationId(spec_id) map { actors =>
			val json = JsonUtil.jsActors(actors).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets test suites deployed for the specification
	 * @param spec_id
	 * @return
	 */
	def getSpecTestSuites(spec_id: Long) = Action.async {
		TestSuiteManager.getTestSuitesWithSpecificationId(spec_id) map { testSuites =>
			val json = JsonUtil.jsTestSuites(testSuites).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets actors defined  for the domain
	 */
	def getDomainActors(domainId: Long) = Action.async {
		ConformanceManager.getActorsWithDomainId(domainId) map { actors =>
			val json = JsonUtil.jsActors(actors).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	/**
	 * Gets test cases defined  for the actor
	 */
	def getActorTestCases(actor_id: Long) = Action.async { request =>
		val optionIds = ParameterExtractor.optionalQueryParameter(request, Parameters.OPTIONS) match {
			case Some(ids) => Some(ids.split(",").map(_.toLong).toList)
			case None => None
		}
    val spec = ParameterExtractor.requiredQueryParameter(request, Parameters.SPEC).toLong
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort

		TestCaseManager.getTestCases(actor_id, spec, optionIds, testCaseType) map { testCases =>
			val json = JsonUtil.jsTestCaseList(testCases).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def createDomain() = Action.async { request =>
		val domain = ParameterExtractor.extractDomain(request)
		ConformanceManager.createDomain(domain) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def createOption() = Action.async { request =>
		val option = ParameterExtractor.extractOption(request)
		ConformanceManager.createOption(option) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def createActor() = Action.async { request =>
		val actor = ParameterExtractor.extractActor(request)
		val specificationId = ParameterExtractor.optionalBodyParameter(request, Parameters.SPECIFICATION_ID) match {
			case Some(str) => Some(str.toLong)
			case _ => None
		}
		ConformanceManager.createActor(actor, specificationId) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def createSpecification() = Action.async { request =>
		val specification = ParameterExtractor.extractSpecification(request)

		ConformanceManager.createSpecifications(specification) map (_ => ResponseConstructor.constructEmptyResponse)
	}

	def getOptionsForActor(actorId: Long) = Action.async { request =>
		ConformanceManager.getOptionsForActor(actorId) map { options =>
			val json = JsonUtil.jsOptions(options).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def getEndpointsForActor(actorId: Long) = Action.async {
		ConformanceManager.getEndpointsForActor(actorId) map { endpoints =>
			val json = JsonUtil.jsEndpoints(endpoints).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def getEndpoints = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)

		ConformanceManager.getEndpoints(ids) map { endpoints =>
			val json = JsonUtil.jsEndpoints(endpoints).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def getEndpointParameters(endpointId: Long) = Action.async {
		ConformanceManager.getEndpointParameters(endpointId) map { parameters =>
			val json = JsonUtil.jsParameters(parameters).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def getOptions = Action.async { request =>
		val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
		ConformanceManager.getOptions(ids) map { options =>
			val json = JsonUtil.jsOptions(options).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}

	def deleteDomain(domain_id: Long) = Action.async {
		Future {
			ResponseConstructor.constructStringResponse("TODO")
		}
	}

	def addActorToSpecification(specification_id: Long, actor_id: Long) = Action.async {
		ConformanceManager.relateActorWithSpecification(actor_id, specification_id) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def deployTestSuite(specification_id: Long) = Action.async(parse.multipartFormData) { request =>
		request.body.file(Parameters.FILE) match {
			case Some(testSuite) => {
				import java.io.File
				val file = new File("/tmp/"+RandomStringUtils.random(10, false, true)+"/"+testSuite.filename)
				file.getParentFile.mkdirs()
				testSuite.ref.moveTo(file)
				val name = testSuite.filename
				val contentType = testSuite.contentType

				logger.debug("Test suite file uploaded - filename: ["+name+"] content type: ["+contentType+"]")

				TestSuiteManager.deployTestSuiteFromZipFile(specification_id, file) map { unit =>
					ResponseConstructor.constructEmptyResponse
				}
			}
			case None => Future {
				ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
			}
		}
	}

}
