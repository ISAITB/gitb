package controllers

import java.io.ByteArrayInputStream
import java.nio.file.Paths
import java.security.cert.{Certificate, CertificateExpiredException, CertificateNotYetValidException, X509Certificate}
import java.security.{KeyStore, NoSuchAlgorithmException}

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import javax.inject.Inject
import managers._
import models.ConformanceStatementFull
import models.Enums.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import play.api.mvc._
import utils.signature.SigUtils
import utils.{JsonUtil, MimeUtil}

class ConformanceService @Inject() (conformanceManager: ConformanceManager, accountManager: AccountManager, actorManager: ActorManager, testSuiteManager: TestSuiteManager, systemManager: SystemManager, testResultManager: TestResultManager, organizationManager: OrganizationManager, testCaseManager: TestCaseManager, endPointManager: EndPointManager, parameterManager: ParameterManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

  /**
   * Gets the list of domains
   */
  def getDomains = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val result = conformanceManager.getDomains(ids)
    val json = JsonUtil.jsDomains(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainOfSpecification(specId: Long) = Action.apply { request =>
    val json = JsonUtil.jsDomain(conformanceManager.getDomainOfSpecification(specId)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain = Action.apply { request =>
   val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val domain = conformanceManager.getCommunityDomain(communityId)
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
    val result = conformanceManager.getSpecifications(ids)
    val json = JsonUtil.jsSpecifications(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the list of specifications
   */
  def getActors = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val result = conformanceManager.getActorsWithSpecificationId(ids, None)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the specifications that are defined/tested in the platform
   */
  def getDomainSpecs(domain_id: Long) = Action.apply {
    val specs = conformanceManager.getSpecifications(domain_id)
    val json = JsonUtil.jsSpecifications(specs).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets actors defined  for the spec
   */
  def getSpecActors(spec_id: Long) = Action.apply {
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(spec_id))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test suites deployed for the specification
   * @param spec_id
   * @return
   */
  def getSpecTestSuites(spec_id: Long) = Action.apply {
    val testSuites = testSuiteManager.getTestSuitesWithSpecificationId(spec_id)
    val json = JsonUtil.jsTestSuitesList(testSuites).toString()
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

    val testCases = testCaseManager.getTestCases(actor_id, spec, optionIds, testCaseType)
    val json = JsonUtil.jsTestCaseList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomain() = Action.apply { request =>
    val domain = ParameterExtractor.extractDomain(request)
    conformanceManager.createDomain(domain)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomain(domainId: Long) = Action.apply { request =>
    val domainExists = conformanceManager.checkDomainExists(domainId)
    if(domainExists) {
      val shortName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
      val fullName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
      val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)

      conformanceManager.updateDomain(domainId, shortName, fullName, description)
      ResponseConstructor.constructEmptyResponse
    } else{
      throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Domain with ID '" + domainId + "' not found")
    }
  }

  def createActor() = Action.apply { request =>
    val actor = ParameterExtractor.extractActor(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An actor with this ID already exists in the specification")
    } else {
      conformanceManager.createActorWrapper(actor, specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createEndpoint() = Action.apply { request =>
    val endpoint = ParameterExtractor.extractEndpoint(request)
    if (endPointManager.checkEndPointExistsForActor(endpoint.name, endpoint.actor, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An endpoint with this name already exists for the actor")
    } else{
      endPointManager.createEndpointWrapper(endpoint)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createParameter() = Action.apply { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      parameterManager.createParameterWrapper(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createSpecification() = Action.apply { request =>
    val specification = ParameterExtractor.extractSpecification(request)
    conformanceManager.createSpecifications(specification)
    ResponseConstructor.constructEmptyResponse
  }

  def getEndpointsForActor(actorId: Long) = Action.apply {
    val endpoints = conformanceManager.getEndpointsForActor(actorId)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpoints = Action.apply { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)

    val endpoints = conformanceManager.getEndpoints(ids)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteDomain(domain_id: Long) = Action.apply {
    conformanceManager.deleteDomain(domain_id)
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
    val result = testSuiteManager.applyPendingTestSuiteAction(specification_id, pendingFolderId, pendingAction)
    val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deployTestSuite(specification_id: Long) = Action.apply(parse.multipartFormData) { request =>
    request.body.file(Parameters.FILE) match {
    case Some(testSuite) => {
      val file = Paths.get(
        testSuiteManager.getTempFolder().getAbsolutePath,
        RandomStringUtils.random(10, false, true),
        testSuite.filename
      ).toFile()
      file.getParentFile.mkdirs()
      testSuite.ref.moveTo(file)
      val name = testSuite.filename
      val contentType = testSuite.contentType
      logger.debug("Test suite file uploaded - filename: [" + name + "] content type: [" + contentType + "]")
      val result = testSuiteManager.deployTestSuiteFromZipFile(specification_id, file)
      val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
    case None =>
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
    }
  }

  def getConformanceStatus(actorId: Long, sutId: Long) = Action.apply { request =>
    val results = conformanceManager.getConformanceStatus(actorId, sutId, None)
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getConformanceStatusForTestSuite(actorId: Long, sutId: Long, testSuite: Long) = Action.apply { request =>
    val results = conformanceManager.getConformanceStatus(actorId, sutId, Some(testSuite))
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCases(testSuiteId: Long) = Action.apply { request =>
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort
    val list = testCaseManager.getTestCasesOfTestSuiteWrapper(testSuiteId, Some(testCaseType))
    import scala.collection.JavaConversions._
    val json: String = JsonUtil.jsTestCasesList(list.toList).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCase(testCaseId: Long) = Action.apply { request =>
    val testCase = testCaseManager.getTestCase(testCaseId.toString()).get
    val json: String = JsonUtil.jsTestCase(testCase).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameters(domainId: Long) = Action.apply { request =>
    val result = conformanceManager.getDomainParameters(domainId)
    val json = JsonUtil.jsDomainParameters(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply { request =>
    val result = conformanceManager.getDomainParameter(domainParameterId)
    val json = JsonUtil.jsDomainParameter(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomainParameter(domainId: Long) = Action.apply { request =>
    val jsDomainParameter = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, None, domainId)
    if (conformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).isDefined) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the domain")
    } else {
      conformanceManager.createDomainParameter(domainParameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def deleteDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply {
    conformanceManager.deleteDomainParameterWrapper(domainParameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomainParameter(domainId: Long, domainParameterId: Long) = Action.apply { request =>
    val jsDomainParameter = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, Some(domainParameterId), domainId)

    val existingDomainParameter = conformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name)
    if (existingDomainParameter.isDefined && (existingDomainParameter.get.id != domainParameterId)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the domain")
    } else {
      conformanceManager.updateDomainParameter(domainParameterId, domainParameter.name, domainParameter.desc, domainParameter.kind, domainParameter.value)
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
      results = conformanceManager.getConformanceStatementsFull(domainIds, specIds, actorIds, communityIds, organizationIds, systemIds)
    } else {
      results = conformanceManager.getConformanceStatements(domainIds, specIds, actorIds, communityIds, organizationIds, systemIds)
    }
    val json = JsonUtil.jsConformanceResultFullList(results).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteAllObsoleteTestResults() = Action.apply { request =>
    val authUserId = ParameterExtractor.extractUserId(request)
    if (accountManager.isSystemAdmin(authUserId)) {
      testResultManager.deleteAllObsoleteTestResults()
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructUnauthorizedResponse(403, "You must be a test bed administrator")
    }
  }

  def deleteObsoleteTestResultsForSystem() = Action.apply { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val authUserId = ParameterExtractor.extractUserId(request)
    val system = systemManager.getSystemByIdWrapper(systemId)
    val organisation = organizationManager.getOrganizationById(system.get.owner)
    if (accountManager.isVendorAdmin(authUserId, system.get.owner)
      || accountManager.isCommunityAdmin(authUserId, organisation.community.get.id)
      || accountManager.isSystemAdmin(authUserId)) {
      testResultManager.deleteObsoleteTestResultsForSystemWrapper(systemId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructUnauthorizedResponse(403, "You must be an organisation, community or test bed administrator")
    }
  }

  def deleteObsoleteTestResultsForCommunity() = Action.apply { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val authUserId = ParameterExtractor.extractUserId(request)
    if (accountManager.isCommunityAdmin(authUserId, communityId) || accountManager.isSystemAdmin(authUserId)) {
      testResultManager.deleteObsoleteTestResultsForCommunityWrapper(communityId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructUnauthorizedResponse(403, "You must be a community or test bed administrator")
    }
  }

  def getConformanceCertificateSettings(communityId: Long) = Action.apply { request =>
    val authUserId = ParameterExtractor.extractUserId(request)
    if (accountManager.isCommunityAdmin(authUserId, communityId) || accountManager.isSystemAdmin(authUserId)) {
      val settings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
      val includeKeystoreData = ParameterExtractor.optionalQueryParameter(request, Parameters.INCLUDE_KEYSTORE_DATA).getOrElse("false").toBoolean
      val json = JsonUtil.jsConformanceSettings(settings, includeKeystoreData)
      if (json.isDefined) {
        ResponseConstructor.constructJsonResponse(json.get.toString)
      } else {
        ResponseConstructor.constructEmptyResponse
      }
    } else {
      ResponseConstructor.constructUnauthorizedResponse(403, "You must be a community or test bed administrator")
    }
  }

  def updateConformanceCertificateSettings(communityId: Long) = Action.apply { request =>
    val authUserId = ParameterExtractor.extractUserId(request)
    if (accountManager.isCommunityAdmin(authUserId, communityId) || accountManager.isSystemAdmin(authUserId)) {
      val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
      val removeKeystore = ParameterExtractor.requiredBodyParameter(request, Parameters.REMOVE_KEYSTORE).toBoolean
      val updatePasswords = ParameterExtractor.requiredBodyParameter(request, Parameters.UPDATE_PASSWORDS).toBoolean
      val settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
      conformanceManager.updateConformanceCertificateSettings(settings, updatePasswords, removeKeystore)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructUnauthorizedResponse(403, "You must be a community or test bed administrator")
    }
  }

  def testKeystoreSettings(communityId: Long) = Action.apply { request =>
    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    val updatePasswords = ParameterExtractor.requiredBodyParameter(request, Parameters.UPDATE_PASSWORDS).toBoolean
    val settings = JsonUtil.parseJsConformanceCertificateSettingsForKeystoreTest(jsSettings, communityId)

    var keystorePassword: String = null
    var keyPassword: String = null
    if (updatePasswords) {
      if (settings.keystorePassword.isDefined && settings.keyPassword.isDefined) {
        keystorePassword = settings.keystorePassword.get
        keyPassword = settings.keyPassword.get
      }
    } else {
      val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
      if (storedSettings.isDefined && storedSettings.get.keystorePassword.isDefined && storedSettings.get.keyPassword.isDefined) {
        keystorePassword = MimeUtil.decryptString(storedSettings.get.keystorePassword.get)
        keyPassword = MimeUtil.decryptString(storedSettings.get.keyPassword.get)
      }
    }

    if (keystorePassword == null || keyPassword == null) {
      throw new IllegalArgumentException("Passwords could not be loaded")
    }

    var problem:String = null
    var level: String = null

    val keystoreBytes = Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.keystoreFile.get))
    val keystore = KeyStore.getInstance(settings.keystoreType.get)
    val bin = new ByteArrayInputStream(keystoreBytes)
    try {
      keystore.load(bin, keystorePassword.toCharArray)
    } catch {
      case e: NoSuchAlgorithmException =>
        problem = "The keystore defines an invalid integrity check algorithm"
        level = "error"
      case e: Exception =>
        problem = "The keystore could not be opened"
        level = "error"
    } finally if (bin != null) bin.close()
    if (problem == null) {
      var certificate: Certificate = null
      try {
        certificate = SigUtils.checkKeystore(keystore, keyPassword.toCharArray)
        if (certificate == null) {
          problem = "A valid key could not be found in the keystore"
          level = "error"
        }
      } catch {
        case e: Exception =>
          problem = "The key could not be read from the keystore"
          level = "error"
      }
      if (problem == null) {
        if (certificate.isInstanceOf[X509Certificate]) {
          val x509Cert = certificate.asInstanceOf[X509Certificate]
          try {
            SigUtils.checkCertificateValidity(x509Cert)
            if (!SigUtils.checkCertificateUsage(x509Cert) || !SigUtils.checkCertificateExtendedUsage(x509Cert)) {
              problem = "The provided certificate is not valid for signature use"
              level = "warning"
            }
          } catch {
            case e: CertificateExpiredException =>
              problem = "The contained certificate is expired"
              level = "error"
            case e: CertificateNotYetValidException =>
              problem = "The certificate is not yet valid"
              level = "error"
          }
        }
      }
    }
    if (problem == null) {
      ResponseConstructor.constructEmptyResponse
    } else {
      val json = JsonUtil.jsConformanceSettingsValidation(problem, level)
      ResponseConstructor.constructJsonResponse(json.toString)
    }
  }
}
