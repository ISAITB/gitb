package controllers

import java.io.ByteArrayInputStream
import java.nio.file.Paths
import java.security.cert.{Certificate, CertificateExpiredException, CertificateNotYetValidException, X509Certificate}
import java.security.{KeyStore, NoSuchAlgorithmException}

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import javax.inject.Inject
import managers._
import models.ConformanceStatementFull
import models.Enums.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import play.api.mvc._
import utils.signature.SigUtils
import utils.{ClamAVClient, JsonUtil, MimeUtil}

class ConformanceService @Inject() (conformanceManager: ConformanceManager, accountManager: AccountManager, actorManager: ActorManager, testSuiteManager: TestSuiteManager, systemManager: SystemManager, testResultManager: TestResultManager, organizationManager: OrganizationManager, testCaseManager: TestCaseManager, endPointManager: EndPointManager, parameterManager: ParameterManager, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

  /**
   * Gets the list of domains
   */
  def getDomains = AuthorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewDomains(request, ids)
    val result = conformanceManager.getDomains(ids)
    val json = JsonUtil.jsDomains(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainOfSpecification(specId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewDomainBySpecificationId(request, specId)
    val json = JsonUtil.jsDomain(conformanceManager.getDomainOfSpecification(specId)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDomainByCommunityId(request, communityId)
    val domain = conformanceManager.getCommunityDomain(communityId)
    if (domain != null) {
      val json = JsonUtil.jsDomain(domain).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getSpecsForSystem(systemId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewSpecificationsBySystemId(request, systemId)
    val result = conformanceManager.getSpecificationsBySystem(systemId)
    val json = JsonUtil.jsSpecifications(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainsForSystem(systemId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewDomainsBySystemId(request, systemId)
    val result = conformanceManager.getDomainsBySystem(systemId)
    val json = JsonUtil.jsDomains(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the list of specifications
   */
  def getSpecs = AuthorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewSpecifications(request, ids)
    val result = conformanceManager.getSpecifications(ids)
    val json = JsonUtil.jsSpecifications(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the list of specifications
   */
  def getActors = AuthorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewActorsBySpecificationIds(request, ids)
    val result = conformanceManager.getActorsWithSpecificationId(ids, None)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getActorsForDomain(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewActorsByDomainId(request, domainId)
    val result = conformanceManager.getActorsByDomainIdWithSpecificationId(domainId)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the specifications that are defined/tested in the platform
   */
  def getDomainSpecs(domain_id: Long) = AuthorizedAction { request =>
    authorizationManager.canViewSpecificationsByDomainId(request, domain_id)
    val specs = conformanceManager.getSpecifications(domain_id)
    val json = JsonUtil.jsSpecifications(specs).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets actors defined  for the spec
   */
  def getSpecActors(spec_id: Long) = AuthorizedAction { request =>
    authorizationManager.canViewActorsBySpecificationId(request, spec_id)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(spec_id))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test suites deployed for the specification
   * @param spec_id
   * @return
   */
  def getSpecTestSuites(spec_id: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestSuitesBySpecificationId(request, spec_id)
    val testSuites = testSuiteManager.getTestSuitesWithSpecificationId(spec_id)
    val json = JsonUtil.jsTestSuitesList(testSuites).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test cases defined  for the actor
   */
  def getActorTestCases(actor_id: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestCasesByActorId(request, actor_id)
    val spec = ParameterExtractor.requiredQueryParameter(request, Parameters.SPEC).toLong
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort

    val testCases = testCaseManager.getTestCases(actor_id, spec, None, testCaseType)
    val json = JsonUtil.jsTestCaseList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomain() = AuthorizedAction { request =>
    authorizationManager.canCreateDomain(request)
    val domain = ParameterExtractor.extractDomain(request)
    conformanceManager.createDomain(domain)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomain(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateDomain(request, domainId)
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

  def createActor() = AuthorizedAction { request =>
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canCreateActor(request, specificationId)
    val actor = ParameterExtractor.extractActor(request)
    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An actor with this ID already exists in the specification")
    } else {
      conformanceManager.createActorWrapper(actor, specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createEndpoint() = AuthorizedAction { request =>
    val endpoint = ParameterExtractor.extractEndpoint(request)
    authorizationManager.canCreateEndpoint(request, endpoint.actor)
    if (endPointManager.checkEndPointExistsForActor(endpoint.name, endpoint.actor, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "An endpoint with this name already exists for the actor")
    } else{
      endPointManager.createEndpointWrapper(endpoint)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createParameter() = AuthorizedAction { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    authorizationManager.canCreateParameter(request, parameter.endpoint)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      parameterManager.createParameterWrapper(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createSpecification() = AuthorizedAction { request =>
    val specification = ParameterExtractor.extractSpecification(request)
    authorizationManager.canCreateSpecification(request, specification.domain)
    conformanceManager.createSpecifications(specification)
    ResponseConstructor.constructEmptyResponse
  }

  def getEndpointsForActor(actorId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewEndpoints(request, actorId)
    val endpoints = conformanceManager.getEndpointsForActor(actorId)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpoints = AuthorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewEndpointsById(request, ids)
    val endpoints = conformanceManager.getEndpoints(ids)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteDomain(domain_id: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteDomain(request, domain_id)
    conformanceManager.deleteDomain(domain_id)
    ResponseConstructor.constructEmptyResponse
  }

  def resolvePendingTestSuite(specification_id: Long) = AuthorizedAction { request =>
    authorizationManager.canEditTestSuites(request, specification_id)
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

  def deployTestSuite(specification_id: Long) = AuthorizedAction(parse.multipartFormData) { request =>
    authorizationManager.canEditTestSuitesMulti(request, specification_id)
    var response:Result = null
    val testSuiteFileName = "ts_"+RandomStringUtils.random(10, false, true)+".zip"
    request.body.file(Parameters.FILE) match {
    case Some(testSuite) => {
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        val fileBytes = FileUtils.readFileToByteArray(testSuite.ref.file)
        val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        val scanResult = virusScanner.scan(fileBytes)
        if (!ClamAVClient.isCleanReply(scanResult)) {
          response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
        }
      }
      if (response == null) {
        val file = Paths.get(
          testSuiteManager.getTempFolder().getAbsolutePath,
          RandomStringUtils.random(10, false, true),
          testSuiteFileName
        ).toFile
        file.getParentFile.mkdirs()
        testSuite.ref.moveTo(file)
        val contentType = testSuite.contentType
        logger.debug("Test suite file uploaded - filename: [" + testSuiteFileName + "] content type: [" + contentType + "]")
        val result = testSuiteManager.deployTestSuiteFromZipFile(specification_id, file)
        val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
        response = ResponseConstructor.constructJsonResponse(json)
      }
    }
    case None =>
      response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
    }
    response
  }

  def getConformanceStatus(actorId: Long, sutId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewConformanceStatus(request, actorId, sutId)
    val results = conformanceManager.getConformanceStatus(actorId, sutId, None)
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getConformanceStatusForTestSuite(actorId: Long, sutId: Long, testSuite: Long) = AuthorizedAction { request =>
    authorizationManager.canViewConformanceStatus(request, actorId, sutId)
    val results = conformanceManager.getConformanceStatus(actorId, sutId, Some(testSuite))
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCases(testSuiteId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestSuite(request, testSuiteId)
    val testCaseType = ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort
    val list = testCaseManager.getTestCasesOfTestSuiteWrapper(testSuiteId, Some(testCaseType))
    import scala.collection.JavaConversions._
    val json: String = JsonUtil.jsTestCasesList(list.toList).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCase(testCaseId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestSuiteByTestCaseId(request, testCaseId)
    val testCase = testCaseManager.getTestCase(testCaseId.toString()).get
    val json: String = JsonUtil.jsTestCase(testCase).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameters(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    val result = conformanceManager.getDomainParameters(domainId)
    val json = JsonUtil.jsDomainParameters(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameter(domainId: Long, domainParameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    val result = conformanceManager.getDomainParameter(domainParameterId)
    val json = JsonUtil.jsDomainParameter(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomainParameter(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    val jsDomainParameter = ParameterExtractor.requiredBodyParameter(request, Parameters.CONFIG)
    val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, None, domainId)
    if (conformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).isDefined) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the domain")
    } else {
      conformanceManager.createDomainParameter(domainParameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def deleteDomainParameter(domainId: Long, domainParameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    conformanceManager.deleteDomainParameterWrapper(domainParameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomainParameter(domainId: Long, domainParameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
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

  def getConformanceOverview() = AuthorizedAction { request =>
    val communityIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.COMMUNITY_IDS)
    authorizationManager.canViewConformanceOverview(request, communityIds)
    val fullResults = ParameterExtractor.requiredQueryParameter(request, Parameters.FULL).toBoolean
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
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

  def deleteAllObsoleteTestResults() = AuthorizedAction { request =>
    authorizationManager.canDeleteAllObsoleteTestResults(request)
    testResultManager.deleteAllObsoleteTestResults()
    ResponseConstructor.constructEmptyResponse
  }

  def deleteObsoleteTestResultsForSystem() = AuthorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForSystem(request, systemId)
    testResultManager.deleteObsoleteTestResultsForSystemWrapper(systemId)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteObsoleteTestResultsForCommunity() = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForCommunity(request, communityId)
    testResultManager.deleteObsoleteTestResultsForCommunityWrapper(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  def getConformanceCertificateSettings(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId)
    val settings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
    val includeKeystoreData = ParameterExtractor.optionalQueryParameter(request, Parameters.INCLUDE_KEYSTORE_DATA).getOrElse("false").toBoolean
    val json = JsonUtil.jsConformanceSettings(settings, includeKeystoreData)
    if (json.isDefined) {
      ResponseConstructor.constructJsonResponse(json.get.toString)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateConformanceCertificateSettings(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    val removeKeystore = ParameterExtractor.requiredBodyParameter(request, Parameters.REMOVE_KEYSTORE).toBoolean
    val updatePasswords = ParameterExtractor.requiredBodyParameter(request, Parameters.UPDATE_PASSWORDS).toBoolean
    val settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
    var response: Result = null
    if (settings.keystoreFile.isDefined && Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      val scanResult = virusScanner.scan(Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.keystoreFile.get)))
      if (!ClamAVClient.isCleanReply(scanResult)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan.")
      }
    }
    if (response == null) {
      conformanceManager.updateConformanceCertificateSettings(settings, updatePasswords, removeKeystore)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  def testKeystoreSettings(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    val updatePasswords = ParameterExtractor.requiredBodyParameter(request, Parameters.UPDATE_PASSWORDS).toBoolean
    val settings = JsonUtil.parseJsConformanceCertificateSettingsForKeystoreTest(jsSettings, communityId)
    val keystoreBytes = Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.keystoreFile.get))
    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      val scanResult = virusScanner.scan(keystoreBytes)
      if (!ClamAVClient.isCleanReply(scanResult)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan.")
      }
    }
    if (response == null) {
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
        response = ResponseConstructor.constructEmptyResponse
      } else {
        val json = JsonUtil.jsConformanceSettingsValidation(problem, level)
        response = ResponseConstructor.constructJsonResponse(json.toString)
      }
    }
    response
  }
}
