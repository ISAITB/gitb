package controllers

import java.nio.file.Paths

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import managers._
import models.ConformanceStatementFull
import models.Enums.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import org.apache.commons.lang.RandomStringUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.JsonUtil

class ConformanceService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

  /**
   * Gets the list of domains
   */
  def getDomains = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val result = ConformanceManager.getDomains(ids)
    val json = JsonUtil.jsDomains(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainOfSpecification(specId: Long) = Action.apply { request =>
    val json = JsonUtil.jsDomain(ConformanceManager.getDomainOfSpecification(specId)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain = Action.apply { request =>
   val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val domain = ConformanceManager.getCommunityDomain(communityId)
    if (domain != null) {
      val json = JsonUtil.jsDomain(domain).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Gets the list of specifications
   */
  def getSpecs = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val result = ConformanceManager.getSpecifications(ids)
    val json = JsonUtil.jsSpecifications(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the list of specifications
   */
  def getActors = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val result = ConformanceManager.getActors(ids)
    val json = JsonUtil.jsActors(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the specifications that are defined/tested in the platform
   */
  def getDomainSpecs(domain_id: Long) = Action.apply {
    val specs = ConformanceManager.getSpecifications(domain_id)
    val json = JsonUtil.jsSpecifications(specs).toString()
    ResponseConstructor.constructJsonResponse(json)
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
  def getSpecTestSuites(spec_id: Long) = Action.apply {
    val testSuites = TestSuiteManager.getTestSuitesWithSpecificationId(spec_id)
    val json = JsonUtil.jsTestSuitesList(testSuites).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets actors defined  for the domain
   */
  def getDomainActors(domainId: Long) = Action.apply {
    val actors = ConformanceManager.getActorsWithDomainId(domainId)
    val json = JsonUtil.jsActors(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test cases defined  for the actor
   */
  def getActorTestCases(actor_id: Long) = Action.apply { request =>
    val optionIds = ParameterExtractor.optionalQueryParameter(request, Parameters.OPTIONS) match {
      case Some(ids) => Some(ids.split(",").map(_.toLong).toList)
      case None => None
    }
    val spec = ParameterExtractor.requiredQueryParameter(request, Parameters.SPEC).toLong
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort

    val testCases = TestCaseManager.getTestCases(actor_id, spec, optionIds, testCaseType)
    val json = JsonUtil.jsTestCaseList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomain() = Action.apply { request =>
    val domain = ParameterExtractor.extractDomain(request)
    ConformanceManager.createDomain(domain)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomain(domainId: Long) = Action.apply { request =>
    val domainExists = ConformanceManager.checkDomainExists(domainId)
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

  def createOption() = Action.apply { request =>
    val option = ParameterExtractor.extractOption(request)
    ConformanceManager.createOption(option)
    ResponseConstructor.constructEmptyResponse
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
      EndPointManager.createEndpointWrapper(endpoint)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createParameter() = Action.apply { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    if (ParameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      ParameterManager.createParameterWrapper(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createSpecification() = Action.apply { request =>
    val specification = ParameterExtractor.extractSpecification(request)
    ConformanceManager.createSpecifications(specification)
    ResponseConstructor.constructEmptyResponse
  }

  def getOptionsForActor(actorId: Long) = Action.apply { request =>
    val options = ConformanceManager.getOptionsForActor(actorId)
    val json = JsonUtil.jsOptions(options).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpointsForActor(actorId: Long) = Action.apply {
    val endpoints = ConformanceManager.getEndpointsForActor(actorId)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpoints = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)

    val endpoints = ConformanceManager.getEndpoints(ids)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getParameters = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)

    val parameters = ConformanceManager.getParameters(ids)
    val json = JsonUtil.jsParameters(parameters).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpointParameters(endpointId: Long) = Action.apply {
    val parameters = ConformanceManager.getEndpointParameters(endpointId)
    val json = JsonUtil.jsParameters(parameters).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getOptions = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val options = ConformanceManager.getOptions(ids)
    val json = JsonUtil.jsOptions(options).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteDomain(domain_id: Long) = Action.apply {
    ConformanceManager.deleteDomain(domain_id)
    ResponseConstructor.constructEmptyResponse
  }

  def addActorToSpecification(specification_id: Long, actor_id: Long) = Action.apply {
    ConformanceManager.relateActorWithSpecification(actor_id, specification_id)
    ResponseConstructor.constructEmptyResponse
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

  def getConformanceStatus(actorId: Long, sutId: Long) = Action.apply { request =>
    val results = ConformanceManager.getConformanceStatus(actorId, sutId, None)
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getConformanceStatusForTestSuite(actorId: Long, sutId: Long, testSuite: Long) = Action.apply { request =>
    val results = ConformanceManager.getConformanceStatus(actorId, sutId, Some(testSuite))
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCases(testSuiteId: Long) = Action.apply { request =>
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort
    val list = TestCaseManager.getTestCasesOfTestSuiteWrapper(testSuiteId, Some(testCaseType))
    import scala.collection.JavaConversions._
    val json: String = JsonUtil.jsTestCasesList(list.toList).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCase(testCaseId: Long) = Action.apply { request =>
    val testCase = TestCaseManager.getTestCase(testCaseId.toString()).get
    val json: String = JsonUtil.jsTestCase(testCase).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameters(domainId: Long) = Action.apply { request =>
    val result = ConformanceManager.getDomainParameters(domainId)
    val json = JsonUtil.jsDomainParameters(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply { request =>
    val result = ConformanceManager.getDomainParameter(domainParameterId)
    val json = JsonUtil.jsDomainParameter(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomainParameter(domainId: Long) = Action.apply { request =>
    val jsDomainParameter = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, None, domainId)
    if (ConformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).isDefined) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the domain")
    } else {
      ConformanceManager.createDomainParameter(domainParameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def deleteDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply {
    ConformanceManager.deleteDomainParameterWrapper(domainParameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply { request =>
    val jsDomainParameter = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, Some(domainParameterId), domainId)

    val existingDomainParameter = ConformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name)
    if (existingDomainParameter.isDefined && (existingDomainParameter.get.id != domainParameterId)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the domain")
    } else {
      ConformanceManager.updateDomainParameter(domainParameterId, domainParameter.name, domainParameter.desc, domainParameter.kind, domainParameter.value)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getConformanceOverview() = Action.apply { request =>
    val fullResults = ParameterExtractor.requiredQueryParameter(request, Parameters.FULL).toBoolean
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val communityIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.COMMUNITY_IDS)
    val organizationIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SYSTEM_IDS)
    val actorIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.ACTOR_IDS)
    var results: List[ConformanceStatementFull] = null
    if (fullResults) {
      results = ConformanceManager.getConformanceStatementsFull(domainIds, specIds, actorIds, communityIds, organizationIds, systemIds)
    } else {
      results = ConformanceManager.getConformanceStatements(domainIds, specIds, actorIds, communityIds, organizationIds, systemIds)
    }
    val json = JsonUtil.jsConformanceResultFullList(results).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

}
