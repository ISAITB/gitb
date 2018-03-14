package controllers

import java.nio.file.Paths
import javax.xml.ws.Endpoint

import exceptions.{ErrorCodes, NotFoundException}
import org.apache.commons.lang.RandomStringUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import managers._
import utils.JsonUtil
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import models.Enums.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice

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

  def getDomainOfSpecification(specId: Long) = Action.apply { request =>
    val json = JsonUtil.jsDomain(ConformanceManager.getDomainOfSpecification(specId)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain = Action.async { request =>
   val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    ConformanceManager.getCommunityDomain(communityId) map { domain =>
      if (domain != null) {
        val json = JsonUtil.jsDomain(domain).toString()
        ResponseConstructor.constructJsonResponse(json)
      } else {
        ResponseConstructor.constructEmptyResponse
      }
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
  def getSpecActors(spec_id: Long) = Action.apply {
    val actors = ConformanceManager.getActorsWithSpecificationId(spec_id)
    val json = JsonUtil.jsActors(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test suites deployed for the specification
   * @param spec_id
   * @return
   */
  def getSpecTestSuites(spec_id: Long) = Action.async {
    TestSuiteManager.getTestSuitesWithSpecificationId(spec_id) map { testSuites =>
      val json = JsonUtil.jsTestSuitesList(testSuites).toString()
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

  def updateDomain(domainId: Long) = Action.async { request =>
    ConformanceManager.checkDomainExists(domainId) map { domainExists =>
      if(domainExists) {
        val shortName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
        val fullName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
        val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)

        ConformanceManager.updateDomain(domainId, shortName, fullName, description)
        ResponseConstructor.constructEmptyResponse
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Domain with ID '" + domainId + "' not found")
      }
    }
  }

  def createOption() = Action.async { request =>
    val option = ParameterExtractor.extractOption(request)
    ConformanceManager.createOption(option) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createActor() = Action.apply { request =>
    val actor = ParameterExtractor.extractActor(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    if (ActorManager.checkActorExistsInSpecification(actor.actorId, specificationId, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An actor with this ID already exists in the specification")
    } else {
      ConformanceManager.createActor(actor, specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createEndpoint() = Action.apply { request =>
    val endpoint = ParameterExtractor.extractEndpoint(request)
    if (EndPointManager.checkEndPointExistsForActor(endpoint.name, endpoint.actor, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An endpoint with this name already exists for the actor")
    } else{
      EndPointManager.createEndpoint(endpoint)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createParameter() = Action.apply { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    if (ParameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      ParameterManager.createParameter(parameter)
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

  def getParameters = Action.async { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)

    ConformanceManager.getParameters(ids) map { parameters =>
      val json = JsonUtil.jsParameters(parameters).toString()
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
    ConformanceManager.deleteDomain(domain_id) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def addActorToSpecification(specification_id: Long, actor_id: Long) = Action.async {
    ConformanceManager.relateActorWithSpecification(actor_id, specification_id) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def resolvePendingTestSuite(specification_id: Long) = Action.apply { request =>
    val pendingFolderId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_TEST_SUITE_ID)
    val pendingActionStr = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_TEST_SUITE_ACTION)
    var pendingAction: TestSuiteReplacementChoice = null
    if ("keep".equals(pendingActionStr)) {
      pendingAction = TestSuiteReplacementChoice.KEEP_TEST_HISTORY
    } else if ("drop".equals(pendingActionStr)) {
      pendingAction = TestSuiteReplacementChoice.DROP_TEST_HISTORY
    } else {
      // Cancel
      pendingAction = TestSuiteReplacementChoice.CANCEL
    }
    val result = TestSuiteManager.applyPendingTestSuiteAction(specification_id, pendingFolderId, pendingAction)
    val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deployTestSuite(specification_id: Long) = Action.apply(parse.multipartFormData) { request =>
    request.body.file(Parameters.FILE) match {
    case Some(testSuite) => {
      val file = Paths.get(
        TestSuiteManager.getTempFolder().getAbsolutePath,
        RandomStringUtils.random(10, false, true),
        testSuite.filename
      ).toFile()
      file.getParentFile.mkdirs()
      testSuite.ref.moveTo(file)
      val name = testSuite.filename
      val contentType = testSuite.contentType
      logger.debug("Test suite file uploaded - filename: [" + name + "] content type: [" + contentType + "]")
      val result = TestSuiteManager.deployTestSuiteFromZipFile(specification_id, file)
      val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
    case None =>
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
    }
  }

  def getActorTestSuites(actorId: Long) = Action.async { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPEC).toLong
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort

    TestSuiteManager.getTestSuitesBySpecificationAndActorAndTestCaseType(specId, actorId, testCaseType) map { list =>
      val json: String = JsonUtil.jsTestSuiteList(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

}
