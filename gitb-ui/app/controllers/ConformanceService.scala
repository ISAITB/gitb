package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import managers._
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import models.Enums.{Result => _, _}
import models._
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils._

import java.io.File
import java.nio.file.{Files, Paths}
import java.security.cert.{Certificate, CertificateExpiredException, CertificateNotYetValidException, X509Certificate}
import java.security.{KeyStore, NoSuchAlgorithmException}
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Using
import utils.signature.SigUtils

class ConformanceService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, communityManager: CommunityManager, conformanceManager: ConformanceManager, accountManager: AccountManager, actorManager: ActorManager, testSuiteManager: TestSuiteManager, systemManager: SystemManager, testResultManager: TestResultManager, organizationManager: OrganizationManager, testCaseManager: TestCaseManager, endPointManager: EndPointManager, parameterManager: ParameterManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager, repositoryUtils: RepositoryUtils) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

  /**
   * Gets the list of domains
   */
  def getDomains = authorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewDomains(request, ids)
    val result = conformanceManager.getDomains(ids)
    val json = JsonUtil.jsDomains(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainOfSpecification(specId: Long) = authorizedAction { request =>
    authorizationManager.canViewDomainBySpecificationId(request, specId)
    val json = JsonUtil.jsDomain(conformanceManager.getDomainOfSpecification(specId)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain = authorizedAction { request =>
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

  /**
   * Gets the list of specifications
   */
  def getSpecs = authorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsBodyParameter(request)
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    authorizationManager.canViewSpecifications(request, ids)
    val result = conformanceManager.getSpecifications(ids, domainIds)
    val json = JsonUtil.jsSpecifications(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the list of specifications
   */
  def getActors = authorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsBodyParameter(request)
    authorizationManager.canViewActors(request, ids)
    val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)

    val result = conformanceManager.getActorsWithSpecificationId(ids, specificationIds)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def searchActors() = authorizedAction { request =>
    authorizationManager.canViewActors(request, None)
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
    val result = conformanceManager.searchActors(domainIds, specificationIds)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def searchActorsInDomain() = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canViewActorsByDomainId(request, domainId)
    val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
    val result = conformanceManager.searchActors(Some(List(domainId)), specificationIds)
    val json = JsonUtil.jsActorsNonCase(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets the specifications that are defined/tested in the platform
   */
  def getDomainSpecs(domain_id: Long) = authorizedAction { request =>
    authorizationManager.canViewSpecificationsByDomainId(request, domain_id)
    val specs = conformanceManager.getSpecifications(domain_id)
    val json = JsonUtil.jsSpecifications(specs).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets actors defined  for the spec
   */
  def getSpecActors(spec_id: Long) = authorizedAction { request =>
    authorizationManager.canViewActorsBySpecificationId(request, spec_id)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(List(spec_id)))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Gets test suites deployed for the specification
   * @param spec_id
   * @return
   */
  def getSpecTestSuites(spec_id: Long) = authorizedAction { request =>
    authorizationManager.canViewTestSuitesBySpecificationId(request, spec_id)
    val testSuites = testSuiteManager.getTestSuitesWithSpecificationId(spec_id)
    val json = JsonUtil.jsTestSuitesList(testSuites).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomain() = authorizedAction { request =>
    authorizationManager.canCreateDomain(request)
    val domain = ParameterExtractor.extractDomain(request)
    conformanceManager.createDomain(domain)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomain(domainId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateDomain(request, domainId)
    val domainExists = conformanceManager.checkDomainExists(domainId)
    if(domainExists) {
      val shortName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
      val fullName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
      val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)

      conformanceManager.updateDomain(domainId, shortName, fullName, description)
      ResponseConstructor.constructEmptyResponse
    } else{
      throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, LabelType.Domain) + " with ID '" + domainId + "' not found.")
    }
  }

  def createActor() = authorizedAction { request =>
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canCreateActor(request, specificationId)
    val actor = ParameterExtractor.extractActor(request)
    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, None)) {
      val labels = communityLabelManager.getLabels(request)
      ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Actor)+" already exists for this ID in the " + communityLabelManager.getLabel(labels, LabelType.Specification, true, true)+".")
    } else {
      val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
      conformanceManager.createActorWrapper(actor.toCaseObject(CryptoUtil.generateApiKey(), domainId), specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createEndpoint() = authorizedAction { request =>
    val endpoint = ParameterExtractor.extractEndpoint(request)
    authorizationManager.canCreateEndpoint(request, endpoint.actor)
    if (endPointManager.checkEndPointExistsForActor(endpoint.name, endpoint.actor, None)) {
      val labels = communityLabelManager.getLabels(request)
      ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Endpoint)+" with this name already exists for the "+communityLabelManager.getLabel(labels, LabelType.Actor, true, true))
    } else{
      endPointManager.createEndpointWrapper(endpoint)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createParameter() = authorizedAction { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    authorizationManager.canCreateParameter(request, parameter.endpoint)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None)) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the "+communityLabelManager.getLabel(request, LabelType.Endpoint, true, true)+".")
    } else {
      parameterManager.createParameterWrapper(parameter)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def createSpecification() = authorizedAction { request =>
    val specification = ParameterExtractor.extractSpecification(request)
    authorizationManager.canCreateSpecification(request, specification.domain)
    conformanceManager.createSpecifications(specification)
    ResponseConstructor.constructEmptyResponse
  }

  def getEndpointsForActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canViewEndpoints(request, actorId)
    val endpoints = conformanceManager.getEndpointsForActor(actorId)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getEndpoints = authorizedAction { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewEndpointsById(request, ids)
    val endpoints = conformanceManager.getEndpoints(ids)
    val json = JsonUtil.jsEndpoints(endpoints).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteDomain(domain_id: Long) = authorizedAction { request =>
    authorizationManager.canDeleteDomain(request, domain_id)
    conformanceManager.deleteDomain(domain_id)
    ResponseConstructor.constructEmptyResponse
  }

  private def specsMatch(allowedIds: Set[Long], actions: List[PendingTestSuiteAction]): Boolean = {
    actions.foreach { action =>
      if (allowedIds.contains(action.specification)) {
        return false
      }
    }
    true
  }

  def resolvePendingTestSuites(): Action[AnyContent] = authorizedAction { request =>
    val specificationIds = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC_IDS).split(',').map(s => s.toLong).toList
    authorizationManager.canManageSpecifications(request, specificationIds)
    val pendingFolderId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
    val overallActionStr = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ACTION)
    var overallAction: TestSuiteReplacementChoice = TestSuiteReplacementChoice.CANCEL
    if ("proceed".equals(overallActionStr)) {
      overallAction = TestSuiteReplacementChoice.PROCEED
    }
    var response: Result = null
    var result: TestSuiteUploadResult = null
    if (overallAction == TestSuiteReplacementChoice.CANCEL) {
      result = testSuiteManager.cancelPendingTestSuiteActions(pendingFolderId)
    } else {
      val actionsStr = ParameterExtractor.optionalBodyParameter(request, Parameters.ACTIONS)
      var actions: List[PendingTestSuiteAction] = null
      if (actionsStr.isDefined) {
        actions = JsonUtil.parseJsPendingTestSuiteActions(actionsStr.get)
        // Ensure that the specification IDs in the individual actions match the list of specification IDs
        if (specsMatch(specificationIds.toSet, actions)) {
          response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_PARAM, "Provided actions don't match selected specifications")
        }
      } else {
        actions = specificationIds.map { specId =>
          new PendingTestSuiteAction(specId, TestSuiteReplacementChoice.PROCEED, Some(TestSuiteReplacementChoiceHistory.KEEP), Some(TestSuiteReplacementChoiceMetadata.SKIP))
        }
      }
      if (response == null) {
        result = testSuiteManager.completePendingTestSuiteActions(pendingFolderId, actions)
      }
    }
    if (response == null) {
      val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
      response = ResponseConstructor.constructJsonResponse(json)
    }
    response
  }

  def deployTestSuiteToSpecifications() = authorizedAction(parse.multipartFormData) { request =>
    try {
      val specIds: List[Long] = ParameterExtractor.requiredBodyParameterMulti(request, Parameters.SPEC_IDS).split(",").map(_.toLong).toList
      authorizationManager.canManageSpecifications(request, specIds)
      var response:Result = null
      val testSuiteFileName = "ts_"+RandomStringUtils.random(10, false, true)+".zip"
      request.body.file(Parameters.FILE) match {
        case Some(testSuite) =>
          if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
            val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            val scanResult = virusScanner.scan(testSuite.ref)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
            }
          }
          if (response == null) {
            val file = Paths.get(
              repositoryUtils.getTempFolder().getAbsolutePath,
              RandomStringUtils.random(10, false, true),
              testSuiteFileName
            ).toFile
            file.getParentFile.mkdirs()
            testSuite.ref.moveTo(file, replace = true)
            val contentType = testSuite.contentType
            logger.debug("Test suite file uploaded - filename: [" + testSuiteFileName + "] content type: [" + contentType + "]")
            val result = testSuiteManager.deployTestSuiteFromZipFile(specIds, file)
            val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
            response = ResponseConstructor.constructJsonResponse(json)
          }
        case None =>
          response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
      }
      response
    } finally {
      request.body.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def getConformanceStatus(actorId: Long, sutId: Long) = authorizedAction { request =>
    authorizationManager.canViewConformanceStatus(request, actorId, sutId)
    val results = conformanceManager.getConformanceStatus(actorId, sutId, None)
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getConformanceStatusForTestSuite(actorId: Long, sutId: Long, testSuite: Long) = authorizedAction { request =>
    authorizationManager.canViewConformanceStatus(request, actorId, sutId)
    val results = conformanceManager.getConformanceStatus(actorId, sutId, Some(testSuite))
    val json: String = JsonUtil.jsConformanceResultList(results).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestSuiteTestCase(testCaseId: Long) = authorizedAction { request =>
    authorizationManager.canViewTestSuiteByTestCaseId(request, testCaseId)
    val testCase = testCaseManager.getTestCase(testCaseId.toString()).get
    val json: String = JsonUtil.jsTestCase(testCase).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameters(domainId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    // Optionally skip loading values (if we only want to show the list of parameters)
    val loadValues = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.VALUES).getOrElse(true)
    val result = conformanceManager.getDomainParameters(domainId, loadValues, None)
    val json = JsonUtil.jsDomainParameters(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParametersOfCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewDomainParametersForCommunity(request, communityId)
    val result = conformanceManager.getDomainParametersByCommunityId(communityId)
    val json = JsonUtil.jsDomainParameters(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getDomainParameter(domainId: Long, domainParameterId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    val result = conformanceManager.getDomainParameter(domainParameterId)
    val json = JsonUtil.jsDomainParameter(result).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createDomainParameter(domainId: Long) = authorizedAction { request =>
    try {
      authorizationManager.canManageDomainParameters(request, domainId)
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      val jsDomainParameter = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.CONFIG)
      var fileToStore: Option[File] = None
      if (files.contains(Parameters.FILE)) {
        fileToStore = Some(files(Parameters.FILE).file)
      }
      var response: Result = null
      val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, None, domainId)
      if (conformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).isDefined) {
        response = ResponseConstructor.constructBadRequestResponse(500, s"A parameter with this name already exists for the ${communityLabelManager.getLabel(request, models.Enums.LabelType.Domain, single = true, lowercase = true)}.")
      } else {
        if (domainParameter.kind == "BINARY") {
          if (fileToStore.isDefined) {
            if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
            }
          } else {
            response = ResponseConstructor.constructBadRequestResponse(500, "No file provided for binary parameter.")
          }
        }
      }
      if (response == null) {
        conformanceManager.createDomainParameter(domainParameter, fileToStore)
        response = ResponseConstructor.constructEmptyResponse
      }
      response
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def deleteDomainParameter(domainId: Long, domainParameterId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    conformanceManager.deleteDomainParameterWrapper(domainId, domainParameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateDomainParameter(domainId: Long, domainParameterId: Long) = authorizedAction { request =>
    try {
      authorizationManager.canManageDomainParameters(request, domainId)
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      var result: Result = null
      val jsDomainParameter = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.CONFIG)
      var fileToStore: Option[File] = None
      if (files.contains(Parameters.FILE)) {
        fileToStore = Some(files(Parameters.FILE).file)
      }
      val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, Some(domainParameterId), domainId)
      val existingDomainParameter = conformanceManager.getDomainParameterByDomainAndName(domainId, domainParameter.name)
      if (existingDomainParameter.isDefined && (existingDomainParameter.get.id != domainParameterId)) {
        result = ResponseConstructor.constructBadRequestResponse(500, s"A parameter with this name already exists for the ${communityLabelManager.getLabel(request, models.Enums.LabelType.Domain, single = true, lowercase = true)}.")
      } else if (domainParameter.kind == "BINARY") {
        if (fileToStore.isDefined) {
          if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
            result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
          }
        }
      }
      if (result == null) {
        conformanceManager.updateDomainParameter(domainId, domainParameterId, domainParameter.name, domainParameter.desc, domainParameter.kind, domainParameter.value, domainParameter.inTests, domainParameter.contentType, fileToStore)
        result = ResponseConstructor.constructEmptyResponse
      }
      result
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def downloadDomainParameterFile(domainId: Long, domainParameterId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomainParameters(request, domainId)
    val file = repositoryUtils.getDomainParameterFile(domainId, domainParameterId)
    if (file.exists()) {
      Ok.sendFile(
        content = file,
        inline = false
      )
    } else {
      ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "Domain parameter was not found")
    }
  }

  def getConformanceOverview() = authorizedAction { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)
    authorizationManager.canViewConformanceOverview(request, communityIds)
    val fullResults = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL).toBoolean
    val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
    val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
    val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
    val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))
    val forExport: Boolean = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
    val status = ParameterExtractor.optionalListBodyParameter(request, Parameters.STATUS)
    val updateTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_BEGIN)
    val updateTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_END)
    val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
    var results: List[ConformanceStatementFull] = null
    if (fullResults) {
      results = conformanceManager.getConformanceStatementsFull(domainIds, specIds, actorIds,
        communityIds, organizationIds, systemIds, orgParameters, sysParameters,
        status, updateTimeBegin, updateTimeEnd,
        sortColumn, sortOrder)
    } else {
      results = conformanceManager.getConformanceStatements(domainIds, specIds, actorIds,
        communityIds, organizationIds, systemIds, orgParameters, sysParameters,
        status, updateTimeBegin, updateTimeEnd,
        sortColumn, sortOrder)
    }
    var orgParameterDefinitions: Option[List[OrganisationParameters]] = None
    var orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    var sysParameterDefinitions: Option[List[SystemParameters]] = None
    var sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
      orgParameterDefinitions = Some(communityManager.getOrganisationParametersForExport(communityIds.get.head))
      orgParameterValues = Some(communityManager.getOrganisationParametersValuesForExport(communityIds.get.head, organizationIds))
      sysParameterDefinitions = Some(communityManager.getSystemParametersForExport(communityIds.get.head))
      sysParameterValues = Some(communityManager.getSystemParametersValuesForExport(communityIds.get.head, organizationIds, systemIds))
    }
    val json = JsonUtil.jsConformanceResultFullList(results, orgParameterDefinitions, orgParameterValues, sysParameterDefinitions, sysParameterValues).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def deleteTestResults() = authorizedAction { request =>
    val communityId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.COMMUNITY_ID)
    authorizationManager.canDeleteTestResults(request, communityId)
    val sessionIds = JsonUtil.parseStringArray(ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_IDS))
    testResultManager.deleteTestSessions(sessionIds)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteAllObsoleteTestResults() = authorizedAction { request =>
    authorizationManager.canDeleteAllObsoleteTestResults(request)
    testResultManager.deleteAllObsoleteTestResults()
    ResponseConstructor.constructEmptyResponse
  }

  def deleteObsoleteTestResultsForSystem() = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForSystem(request, systemId)
    testResultManager.deleteObsoleteTestResultsForSystemWrapper(systemId)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteObsoleteTestResultsForCommunity() = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForCommunity(request, communityId)
    testResultManager.deleteObsoleteTestResultsForCommunityWrapper(communityId)
    ResponseConstructor.constructEmptyResponse
  }

  def downloadConformanceCertificateKeystore(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId)
    val settings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
    if (settings.isDefined && settings.get.keystoreFile.isDefined) {
      val tempFile = Files.createTempFile("itb", "store")
      try {
        Files.write(tempFile, Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.get.keystoreFile.get)))
      } catch {
        case e:Exception =>
          FileUtils.deleteQuietly(tempFile.toFile)
          throw new IllegalStateException("Unable to generate keystore file", e)
      }
      Ok.sendFile(
        content = tempFile.toFile,
        inline = false,
        onClose = () => { FileUtils.deleteQuietly(tempFile.toFile) }
      )
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getConformanceCertificateSettings(communityId: Long) = authorizedAction { request =>
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

  def updateConformanceCertificateSettings(communityId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
    try {
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      var response: Result = null
      var keystoreData: Option[String] = None
      if (files.contains(Parameters.FILE)) {
        if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
          val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
          val scanResult = virusScanner.scan(files(Parameters.FILE).file)
          if (!ClamAVClient.isCleanReply(scanResult)) {
            response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan.")
          }
        }
        if (response == null) {
          keystoreData = Some(MimeUtil.getFileAsDataURL(files(Parameters.FILE).file, "application/octet-stream"))
        }
      }
      if (response == null) {
        val jsSettings = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS)
        val removeKeystore = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.REMOVE_KEYSTORE).toBoolean
        val updatePasswords = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.UPDATE_PASSWORDS).toBoolean
        val settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId, keystoreData)
        conformanceManager.updateConformanceCertificateSettings(settings, updatePasswords, removeKeystore)
        response = ResponseConstructor.constructEmptyResponse
      }
      response
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def testKeystoreSettings(communityId: Long) = authorizedAction { request =>
    var keystoreFile: Option[File] = None
    try {
      authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      var response: Result = null
      if (files.contains(Parameters.FILE)) {
        keystoreFile = Some(files(Parameters.FILE).file)
        if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
          val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
          val scanResult = virusScanner.scan(keystoreFile.get)
          if (!ClamAVClient.isCleanReply(scanResult)) {
            response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan.")
          }
        }
      }
      if (response == null) {
        val jsSettings = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS)
        val updatePasswords = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.UPDATE_PASSWORDS).toBoolean
        val settings = JsonUtil.parseJsConformanceCertificateSettingsForKeystoreTest(jsSettings, communityId)
        var keystorePassword: Option[String] = None
        var keyPassword: Option[String] = None
        if (updatePasswords) {
          if (settings.keystorePassword.isDefined && settings.keyPassword.isDefined) {
            keystorePassword = settings.keystorePassword
            keyPassword = settings.keyPassword
          }
        }
        if (keyPassword.isEmpty || keystorePassword.isEmpty || keystoreFile.isEmpty) {
          val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
          if (storedSettings.isDefined) {
            if (keyPassword.isEmpty && storedSettings.get.keyPassword.isDefined) {
              keyPassword = Some(MimeUtil.decryptString(storedSettings.get.keyPassword.get))
            }
            if (keystorePassword.isEmpty && storedSettings.get.keystorePassword.isDefined) {
              keystorePassword = Some(MimeUtil.decryptString(storedSettings.get.keystorePassword.get))
            }
            if (keystoreFile.isEmpty && storedSettings.get.keystoreFile.isDefined) {
              val tempFile = Files.createTempFile("itb", "store")
              Files.write(tempFile, Base64.decodeBase64(MimeUtil.getBase64FromDataURL(storedSettings.get.keystoreFile.get)))
              keystoreFile = Some(tempFile.toFile)
            }
          }
        }
        if (keyPassword.isEmpty || keystorePassword.isEmpty || keystoreFile.isEmpty) {
          throw new IllegalArgumentException("Passwords and keystore could not be loaded")
        }
        var problem:String = null
        var level: String = null
        val keystore = KeyStore.getInstance(settings.keystoreType.get)
        Using(Files.newInputStream(keystoreFile.get.toPath)) { input =>
          try {
            keystore.load(input, keystorePassword.get.toCharArray)
          } catch {
            case _: NoSuchAlgorithmException =>
              problem = "The keystore defines an invalid integrity check algorithm"
              level = "error"
            case _: Exception =>
              problem = "The keystore could not be opened"
              level = "error"
          }
        }
        if (problem == null) {
          var certificate: Certificate = null
          try {
            certificate = SigUtils.checkKeystore(keystore, keyPassword.get.toCharArray)
            if (certificate == null) {
              problem = "A valid key could not be found in the keystore"
              level = "error"
            }
          } catch {
            case _: Exception =>
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
                case _: CertificateExpiredException =>
                  problem = "The contained certificate is expired"
                  level = "error"
                case _: CertificateNotYetValidException =>
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
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
      if (keystoreFile.isDefined) {
        FileUtils.deleteQuietly(keystoreFile.get)
      }
    }
  }

  def getSystemConfigurations() = authorizedAction { request =>
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, system)
    val actor = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    val configs = conformanceManager.getSystemConfigurationStatus(system, actor)
    val isAdmin = accountManager.checkUserRole(ParameterExtractor.extractUserId(request), UserRole.SystemAdmin, UserRole.CommunityAdmin)
    val json = JsonUtil.jsSystemConfigurationEndpoints(configs, addValues = true, isAdmin)
    ResponseConstructor.constructJsonResponse(json.toString)
  }

  def checkConfigurations() = authorizedAction { request =>
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, system)
    val actor = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    val status = conformanceManager.getSystemConfigurationStatus(system, actor).map { configStatus =>
      if (configStatus.endpointParameters.isDefined) {
        // Filter out values with missing prerequisites.
        configStatus.endpointParameters = Some(PrerequisiteUtil.withValidPrerequisites(configStatus.endpointParameters.get))
      }
      configStatus
    }
    val json = JsonUtil.jsSystemConfigurationEndpoints(status, addValues = false, isAdmin = false) // The isAdmin flag only affects whether or a hidden value will be added (i.e. not applicable is addValues is false)
    ResponseConstructor.constructJsonResponse(json.toString)
  }

  def getDocumentationForPreview() = authorizedAction { request =>
    authorizationManager.canPreviewDocumentation(request)
    val documentation = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
    ResponseConstructor.constructStringResponse(documentation)
  }

  def getTestCaseDocumentation(id: Long) = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, id.toString)
    val documentation = testCaseManager.getTestCaseDocumentation(id)
    if (documentation.isDefined) {
      ResponseConstructor.constructStringResponse(documentation.get)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getTestSuiteDocumentation(id: Long) = authorizedAction { request =>
    authorizationManager.canViewTestSuite(request, id)
    val documentation = testSuiteManager.getTestSuiteDocumentation(id)
    if (documentation.isDefined) {
      ResponseConstructor.constructStringResponse(documentation.get)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

}
