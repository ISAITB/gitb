package controllers

import com.gitb.tbs.TestStepStatus
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import com.gitb.xml.export.Export
import config.Configurations
import controllers.dto.ExportDemoCertificateInfo
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{Parameters, _}
import exceptions.{ErrorCodes, ServiceCallException}
import jakarta.xml.bind.JAXBElement
import managers._
import managers.export._
import models.Enums.ReportType.ReportType
import models.Enums.{OverviewLevelType, ReportType}
import models._
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import play.api.mvc.{Action, _}
import utils._

import java.io._
import java.nio.file.{Files, Path, Paths}
import javax.inject.Inject
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

class RepositoryService @Inject() (implicit ec: ExecutionContext,
                                   authorizedAction: AuthorizedAction,
                                   cc: ControllerComponents,
                                   communityManager: CommunityManager,
                                   systemManager: SystemManager,
                                   testCaseManager: TestCaseManager,
                                   testSuiteManager: TestSuiteManager,
                                   reportManager: ReportManager,
                                   testResultManager: TestResultManager,
                                   conformanceManager: ConformanceManager,
                                   authorizationManager: AuthorizationManager,
                                   communityLabelManager: CommunityLabelManager,
                                   exportManager: ExportManager,
                                   importPreviewManager: ImportPreviewManager,
                                   importCompleteManager: ImportCompleteManager,
                                   repositoryUtils: RepositoryUtils,
                                   systemConfigurationManager: SystemConfigurationManager) extends AbstractController(cc) {

	private final val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private final val codec = new URLCodec()
  private final val EXPORT_QNAME:QName = new QName("http://www.gitb.com/export/v1/", "export")

	def getTestSuiteResource(locationKey: String, filePath:String): Action[AnyContent] = authorizedAction { request =>
    // Location key is either a test case ID (e.g. '123') or a test case ID prefixed by a test suite string identifier [TEST_SUITE_IDENTIFIER]|123.
    authorizationManager.canViewTestSuiteResource(request, locationKey)
    var testSuiteIdentifier: Option[String] = None
    var testIdentifier: Option[String] = None
    val locationKeyToUse = codec.decode(locationKey)
    val locationSeparatorIndex = locationKeyToUse.lastIndexOf('|')
    if (locationSeparatorIndex != -1) {
      testSuiteIdentifier = Some(locationKeyToUse.substring(0, locationSeparatorIndex))
      testIdentifier = Some(locationKeyToUse.substring(locationSeparatorIndex+1))
    } else {
      testIdentifier = Some(locationKeyToUse)
    }
    val testCaseId = testIdentifier.get.toLong
    var testSuite: Option[TestSuites] = None
    if (testSuiteIdentifier.isEmpty) {
      // Consider test suite of test case.
      testSuite = Some(testSuiteManager.getTestSuiteOfTestCase(testCaseId))
    } else {
      // Find test suite in specification or domain.
      val specificationsOfTestCase = testCaseManager.getSpecificationsOfTestCases(List(testCaseId))
      val domainId = testCaseManager.getDomainOfTestCase(testCaseId)
      testSuite = repositoryUtils.findTestSuiteByIdentifier(testSuiteIdentifier.get, domainId, specificationsOfTestCase.headOption)
    }
    if (testSuite.isEmpty) {
      NotFound
    } else {
      var filePathToLookup = codec.decode(filePath)
      if (filePathToLookup.startsWith("/")) {
        filePathToLookup = filePathToLookup.substring(1)
      }
      var filePathToAlsoCheck: Option[String] = null
      if (!filePathToLookup.startsWith(testSuite.get.identifier)) {
        filePathToLookup = testSuite.get.filename + "/" + filePathToLookup
        filePathToAlsoCheck = None
      } else {
        filePathToAlsoCheck = Some(testSuite.get.filename + "/" + filePathToLookup)
        filePathToLookup = StringUtils.replaceOnce(filePathToLookup, testSuite.get.identifier, testSuite.get.filename)
      }
      // Ensure that the requested resource is within the test suite folder (to avoid path traversal)
      val testSuiteFolder = repositoryUtils.getTestSuitesResource(testSuite.get.domain, testSuite.get.filename, None)
      val file = repositoryUtils.getTestSuitesResource(testSuite.get.domain, filePathToLookup, filePathToAlsoCheck)
      logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
      if (file.exists() && file.toPath.normalize().startsWith(testSuiteFolder.toPath.normalize())) {
        Ok.sendFile(file, inline = true)
      } else {
        NotFound
      }
    }
	}

  def getTestResult(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTestSession(request, sessionId, requireAdmin = false)
    getTestResultInternal(sessionId, isAdmin = false)
  }

  def getTestResultAdmin(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTestSession(request, sessionId, requireAdmin = true)
    getTestResultInternal(sessionId, isAdmin = true)
  }

  private def getTestResultInternal(sessionId: String, isAdmin: Boolean): Result = {
    val result = testResultManager.getTestResult(sessionId)
    if (result.isDefined) {
      // Load also logs.
      val logContents = testResultManager.getTestSessionLog(sessionId, isExpected = true)
      // Load also pending interactions.
      val adminInteractions = if (isAdmin) None else Some(false)
      val pendingInteractions = testResultManager.getTestInteractions(sessionId, adminInteractions)
      // Serialise.
      val json = JsonUtil.jsTestResultReport(result.get, None, None, None, None, withOutputMessage = true, logContents, Some(pendingInteractions)).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getPendingTestSessionsForAdminInteraction(): Action[AnyContent] = authorizedAction { request =>
    val communityId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.COMMUNITY_ID)
    authorizationManager.canCheckPendingTestSessionInteractions(request, communityId)
    val results = testResultManager.getPendingTestSessionsForAdminInteraction(communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsStringArray(results).toString())
  }

  def getPendingTestSessionInteractionsAdmin(session: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTestSession(request, session, requireAdmin = true)
    val results = testResultManager.getTestInteractions(session, None)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsTestInteractions(results).toString())
  }

  def getPendingTestSessionInteractions(session: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTestSession(request, session, requireAdmin = false)
    val results = testResultManager.getTestInteractions(session, Some(false))
    ResponseConstructor.constructJsonResponse(JsonUtil.jsTestInteractions(results).toString())
  }

  private def getTestSessionData(sessionFolderInfo: SessionFolderInfo, dataId: String): Option[Path] = {
    var sessionDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = false)
    var dataFile = Path.of(sessionDataFolder.toString, dataId)
    if (!Files.exists(dataFile)) {
      sessionDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = true)
      dataFile = Path.of(sessionDataFolder.toString, dataId)
      if (!Files.exists(dataFile)) {
        None
      } else {
        Some(dataFile)
      }
    } else {
      Some(dataFile)
    }
  }

  def getTestSessionLog(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val logContents = testResultManager.getTestSessionLog(sessionId, isExpected = true)
    if (logContents.isDefined) {
      ResponseConstructor.constructStringResponse(JsonUtil.jsStringArray(logContents.get).toString())
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getTestStepReportDataAsDataUrl(sessionId: String, dataId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val sessionFolderInfo = repositoryUtils.getPathForTestSession(sessionId, isExpected = true)
    try {
      val dataFile = getTestSessionData(sessionFolderInfo, dataId)
      if (dataFile.isEmpty) {
        NotFound
      } else {
        val requestedMimeType = request.headers.get(ACCEPT)
        val detectedMimeType = Option(MimeUtil.getMimeType(dataFile.get))
        var mimeTypeToUse: Option[String] = None
        var dataUrl: Option[String] = None
        if (requestedMimeType.isEmpty || requestedMimeType.get.contains("*")) {
          mimeTypeToUse = detectedMimeType
          dataUrl = Some(MimeUtil.getFileAsDataURL(dataFile.get.toFile, mimeTypeToUse.getOrElse("application/octet-stream")))
        } else {
          mimeTypeToUse = requestedMimeType
          if (MimeUtil.isImageType(requestedMimeType.get) && detectedMimeType.exists(_.equals("text/plain"))) {
            // This is an image stored as a base64-encoded string.
            dataUrl = Some(MimeUtil.base64AsDataURL(Files.readString(dataFile.get), requestedMimeType.get))
          } else {
            dataUrl = Some(MimeUtil.getFileAsDataURL(dataFile.get.toFile, requestedMimeType.get))
          }
        }
        ResponseConstructor.constructJsonResponse(JsonUtil.jsFileReference(dataUrl.get, mimeTypeToUse.getOrElse("application/octet-stream")).toString())
      }
    } finally {
      if (sessionFolderInfo.archived) {
        FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
      }
    }
  }

  def getTestStepReportData(sessionId: String, dataId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val sessionFolderInfo = repositoryUtils.getPathForTestSession(sessionId, isExpected = true)
    val dataFile = getTestSessionData(sessionFolderInfo, dataId)
    if (dataFile.isEmpty) {
      NotFound
    } else {
      var mimeTypeToUse = request.headers.get(ACCEPT)
      if (mimeTypeToUse.isEmpty || mimeTypeToUse.get.contains("*")) {
        mimeTypeToUse = Option(MimeUtil.getMimeType(dataFile.get))
      }
      val extension = MimeUtil.getExtensionFromMimeType(mimeTypeToUse.orNull)
      Ok.sendFile(
        content = dataFile.get.toFile,
        fileName = _ => Some(dataId+extension),
        onClose = () => {
          if (sessionFolderInfo.archived) {
            FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
          }
        }
      )
    }
  }

  def getTestStepReport(sessionId: String, reportPath: String): Action[AnyContent] = authorizedAction { request =>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId)

		val sessionFolderInfo = repositoryUtils.getPathForTestSessionWrapper(sessionId, isExpected = true)
    try {
      var path: String = null
      path = reportPath
        .replace("__SQS__", "[")
        .replace("__SQE__", "]")

      if (path.startsWith(sessionId)) {
        // Backwards compatibility.
        val pathParts = StringUtils.split(codec.decode(path), "/")
        path = pathParts(1)
      } else {
        path = path
      }
      path = codec.decode(path)
      val file = new File(sessionFolderInfo.path.toFile, path)

      if(file.exists()) {
        //read file into a string
        val bytes  = Files.readAllBytes(Paths.get(file.getAbsolutePath))
        val string = new String(bytes)

        //convert string in xml format into its object representation
        val step = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))

        //serialize report inside the object into json
        ResponseConstructor.constructJsonResponse(JacksonUtil.serializeTestReport(step.getReport))
      } else {
        NotFound
      }
    } finally {
      if (sessionFolderInfo.archived) {
        FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
      }
    }
	}

  def exportTestStepReport(sessionId: String, stepReportPath: String): Action[AnyContent] = authorizedAction { request =>
    //    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val contentType = request.headers.get("Accept").getOrElse(Constants.MimeTypeXML)
    val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
    val reportPath = getReportTempFile(suffix)
    try {
      var path: String = if (stepReportPath.startsWith(sessionId)) {
        // Backwards compatibility.
        val pathParts = StringUtils.split(codec.decode(stepReportPath), "/")
        pathParts(1)
      } else {
        stepReportPath
      }
      path = codec.decode(path)
      val reportFile = reportManager.generateTestStepReport(reportPath, sessionId, path, contentType, ParameterExtractor.extractOptionalUserId(request))
      if (reportFile.isEmpty) {
        NotFound
      } else {
        Ok.sendFile(
          content = reportFile.get.toFile,
          fileName = _ => Some("step_report"+suffix),
          onClose = () => {
            if (reportFile.isDefined) {
              FileUtils.deleteQuietly(reportFile.get.toFile)
            }
          }
        )
      }
    } catch {
      case e: Exception =>
        if (Files.exists(reportPath)) {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
        throw e
    }
  }

  def exportTestCaseReport(): Action[AnyContent] = authorizedAction { request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, session)
    val contentType = request.headers.get(Constants.AcceptHeader).getOrElse(Constants.MimeTypeXML)
    val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
    val reportPath = getReportTempFile(suffix)
    try {
      val reportFile = reportManager.generateTestCaseReport(reportPath, session, contentType, None, ParameterExtractor.extractOptionalUserId(request))
      if (reportFile.isDefined) {
        Ok.sendFile(
          content = reportFile.get.toFile,
          fileName = _ => Some("test_report"+suffix),
          onClose = () => {
            if (reportFile.isDefined) {
              FileUtils.deleteQuietly(reportFile.get.toFile)
            }
          }
        )
      } else {
        NotFound
      }
    } catch {
      case e: Exception =>
        if (Files.exists(reportPath)) {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
        throw e
    }
  }

  private def exportConformanceOverviewCertificateInternal(settings: Option[ConformanceOverviewCertificateWithMessages], communityId: Long, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], snapshotId: Option[Long]): Result = {
    val reportPath = getReportTempFile(".pdf")
    try {
      reportManager.generateConformanceOverviewCertificate(reportPath, settings, systemId, domainId, groupId, specificationId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_certificate.pdf"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        logger.warn("Error while generating conformance certificate preview", e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
    }
  }

  def exportOwnConformanceOverviewCertificateReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewOwnConformanceCertificateReport(request, systemId, snapshotId)
    val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
    val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
    val communityId = systemManager.getCommunityIdOfSystem(systemId)

    exportConformanceOverviewCertificateInternal(None, communityId, systemId, domainId, groupId, specificationId, snapshotId)
  }

  def exportConformanceOverviewCertificateReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewConformanceCertificateReport(request, communityId, snapshotId)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
    val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)

    val jsSettings = ParameterExtractor.optionalBodyParameter(request, Parameters.SETTINGS)
    val settings = jsSettings.map(JsonUtil.parseJsConformanceOverviewCertificateWithMessages(_, communityId))

    exportConformanceOverviewCertificateInternal(settings, communityId, systemId, domainId, groupId, specificationId, snapshotId)
  }

  def exportConformanceOverviewReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId)

    val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
    val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)

    val reportPath = getReportTempFile(".pdf")
    try {
      val communityId = if (snapshotId.isDefined) {
        conformanceManager.getConformanceSnapshot(snapshotId.get).community
      } else {
        systemManager.getCommunityIdOfSystem(systemId)
      }
      reportManager.generateConformanceOverviewReport(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some(reportPath.toFile.getName),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportOwnConformanceOverviewReportInXML(): Action[AnyContent] = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    if (snapshotId.isDefined) {
      authorizationManager.canViewSystemInSnapshot(request, systemId, snapshotId.get)
    } else {
      authorizationManager.canViewSystem(request, systemId)
    }
    val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
    val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
    val communityId = systemManager.getCommunityIdOfSystem(systemId)
    val reportPath = getReportTempFile(".pdf")
    try {
      reportManager.generateConformanceOverviewReportInXML(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_report.xml"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportConformanceOverviewReportInXML(): Action[AnyContent] = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
    val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val reportPath = getReportTempFile(".pdf")
    try {
      reportManager.generateConformanceOverviewReportInXML(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_report.xml"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportOwnConformanceStatementReportInXML(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.TESTS).getOrElse(false)
    val communityId = systemManager.getCommunityIdOfSystem(systemId)
    val reportPath = getReportTempFile(".pdf")
    try {
      reportManager.generateConformanceStatementReportInXML(reportPath, withTestSteps, actorId, systemId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_report.xml"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportConformanceStatementReportInXML(): Action[AnyContent] = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId)
    val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.TESTS).getOrElse(false)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val reportPath = getReportTempFile(".xml")
    try {
      reportManager.generateConformanceStatementReportInXML(reportPath, withTestSteps, actorId, systemId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_report.xml"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportConformanceStatementReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val includeTests = ParameterExtractor.requiredBodyParameter(request, Parameters.TESTS).toBoolean

    val reportPath = getReportTempFile(".pdf")
    try {
      val labels = communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request))
      val communityId = if (snapshotId.isDefined) {
        conformanceManager.getConformanceSnapshot(snapshotId.get).community
      } else {
        systemManager.getCommunityIdOfSystem(systemId)
      }
      reportManager.generateConformanceStatementReport(reportPath, includeTests, actorId, systemId, labels, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_report.pdf"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  private def exportConformanceCertificateInternal(settings: Option[ConformanceCertificateInfo], communityId: Long, systemId: Long, actorId: Long, snapshotId: Option[Long]): Result = {
    val reportPath = getReportTempFile(".pdf")
    try {
      reportManager.generateConformanceCertificate(reportPath, settings, actorId, systemId, communityId, snapshotId)
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_certificate.pdf"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportDemoConformanceOverviewCertificateReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoCertificateReport(communityId, request, ReportType.ConformanceOverviewCertificate , exportInfo => {
      val certificateSettings = JsonUtil.parseJsConformanceOverviewCertificateWithMessages(exportInfo.jsSettings, communityId)
      val reportIdentifier = ParameterExtractor.optionalLongBodyParameter(exportInfo.paramMap, Parameters.ID)
      val reportLevel = OverviewLevelType.withName(ParameterExtractor.requiredBodyParameter(exportInfo.paramMap, Parameters.LEVEL))
      var settingsToUse: Option[ConformanceCertificateInfo] = None
      if (!exportInfo.reportSettings.customPdfs) {
        // Get the message (if needed) for the specific level
        val customMessage = certificateSettings.messageToUse(reportLevel, reportIdentifier)
        // Get the keystore (if needed) to use for the signature
        val keystore = if (certificateSettings.settings.includeSignature) {
          communityManager.getCommunityKeystore(communityId, decryptKeys = true)
        } else {
          None
        }
        settingsToUse = Some(certificateSettings.settings.toConformanceCertificateInfo(customMessage, keystore))
      }
      reportManager.generateDemoConformanceOverviewCertificate(exportInfo.reportPath, exportInfo.reportSettings, exportInfo.stylesheetPath, settingsToUse, communityId, reportLevel)
    })
  }

  def exportOwnConformanceCertificateReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewOwnConformanceCertificateReport(request, systemId, snapshotId)
    val communityId = systemManager.getCommunityIdOfSystem(systemId)
    exportConformanceCertificateInternal(None, communityId, systemId, actorId, snapshotId)
  }

  def exportConformanceCertificateReport(): Action[AnyContent] = authorizedAction { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewConformanceCertificateReport(request, communityId, snapshotId)
    val jsSettings = ParameterExtractor.optionalBodyParameter(request, Parameters.SETTINGS)
    val settings = jsSettings.map(x => {
      val parsedSettings = JsonUtil.parseJsConformanceCertificateSettings(x, communityId)
      if (parsedSettings.includeSignature) {
        val keystore = communityManager.getCommunityKeystore(communityId, decryptKeys = true)
        parsedSettings.toConformanceCertificateInfo(keystore)
      } else {
        parsedSettings.toConformanceCertificateInfo(None)
      }
    })
    exportConformanceCertificateInternal(settings, communityId, systemId, actorId, snapshotId)
  }

  private def exportDemoCertificateReport(communityId: Long, request: RequestWithAttributes[AnyContent], reportType: ReportType, certificateSettingsLoader: ExportDemoCertificateInfo => Unit): Result = {
    authorizationManager.canPreviewConformanceCertificateReport(request, communityId)
    val reportPath = getReportTempFile(".pdf")
    try {
      val paramMap = ParameterExtractor.paramMap(request)
      var result: Option[Result] = None
      var stylesheetFile: Option[Path] = None
      val reportSettings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      val jsSettings = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS)
      if (reportSettings.customPdfs) {
        if (reportSettings.customPdfsWithCustomXml) {
          val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
          if (stylesheetInfo._2.isEmpty) {
            if (stylesheetInfo._1.isDefined) {
              // Stylesheet provided as part of the request
              stylesheetFile = Some(stylesheetInfo._1.get.toPath)
            } else {
              // Already existing stylesheet
              stylesheetFile = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
            }
          } else {
            result = stylesheetInfo._2
          }
        }
      }
      if (result.isEmpty) {
        certificateSettingsLoader.apply(ExportDemoCertificateInfo(reportSettings, stylesheetFile, jsSettings, reportPath, paramMap))
        Ok.sendFile(
          content = reportPath.toFile,
          fileName = _ => Some("conformance_certificate.pdf"),
          onClose = () => {
            FileUtils.deleteQuietly(reportPath.toFile)
          }
        )
      } else {
        result.get
      }
    } catch {
      case e: ServiceCallException => handleDemoReportServiceError(e, reportPath)
      case e: Exception => handleDemoReportGeneralError(e, reportPath)
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def exportDemoConformanceCertificateReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoCertificateReport(communityId, request, ReportType.ConformanceStatementCertificate, exportInfo => {
      val certificateSettings = JsonUtil.parseJsConformanceCertificateSettings(exportInfo.jsSettings, communityId)
      var settingsToUse: Option[ConformanceCertificateInfo] = None
      if (!exportInfo.reportSettings.customPdfs) {
        val keystore = if (certificateSettings.includeSignature) {
          communityManager.getCommunityKeystore(communityId, decryptKeys = true)
        } else {
          None
        }
        settingsToUse = Some(certificateSettings.toConformanceCertificateInfo(keystore))
      }
      reportManager.generateDemoConformanceCertificate(exportInfo.reportPath, exportInfo.reportSettings, exportInfo.stylesheetPath, settingsToUse, communityId)
    })
  }

  def exportDemoTestStepReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReport(request, communityId, "step_report.pdf", ReportType.TestStepReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoTestStepReport(reportPath, settings, xsltPath)
    })
  }

  def exportDemoTestStepReportInXML(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReportInXML(request, communityId, "step_report.xml", ReportType.TestStepReport, (reportPath: Path, xsltPath: Option[Path], _: Option[Map[String, Seq[String]]]) => {
      reportManager.generateDemoTestStepReportInXML(reportPath, xsltPath)
    })
  }

  def exportDemoTestCaseReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReport(request, communityId, "test_report.pdf", ReportType.TestCaseReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoTestCaseReport(reportPath, settings, xsltPath)
    })
  }

  def exportDemoTestCaseReportInXML(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReportInXML(request, communityId, "test_report.xml", ReportType.TestCaseReport, (reportPath: Path, xsltPath: Option[Path], _: Option[Map[String, Seq[String]]]) => {
      reportManager.generateDemoTestCaseReportInXML(reportPath, xsltPath)
    })
  }

  def exportDemoConformanceOverviewReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReport(request, communityId, "conformance_report.pdf", ReportType.ConformanceOverviewReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val level = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.LEVEL).map(OverviewLevelType.withName).getOrElse(OverviewLevelType.OrganisationLevel)
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoConformanceOverviewReport(reportPath, settings, xsltPath, communityId, level)
    })
  }

  def exportDemoConformanceOverviewReportInXML(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReportInXML(request, communityId, "conformance_report.xml", ReportType.ConformanceOverviewReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val level = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.LEVEL).map(OverviewLevelType.withName).getOrElse(OverviewLevelType.OrganisationLevel)
      reportManager.generateDemoConformanceOverviewReportInXML(reportPath, xsltPath, communityId, level)
    })
  }

  def exportDemoConformanceStatementReport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReport(request, communityId, "conformance_report.pdf", ReportType.ConformanceStatementReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.TESTS).getOrElse(false)
      reportManager.generateDemoConformanceStatementReport(reportPath, settings, xsltPath, withTestSteps, communityId)
    })
  }

  def exportDemoConformanceStatementReportInXML(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    exportDemoReportInXML(request, communityId, "conformance_report.xml", ReportType.ConformanceStatementReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.TESTS).getOrElse(false)
      reportManager.generateDemoConformanceStatementReportInXML(reportPath, xsltPath, withTestSteps, communityId)
    })
  }

  private def exportDemoReport(request: RequestWithAttributes[AnyContent], communityId: Long, reportName: String, reportType: ReportType, handler: (Path, Option[Path], Option[Map[String, Seq[String]]]) => Unit): Result = {
    val reportPath = getReportTempFile(".pdf")
    try {
      authorizationManager.canManageCommunity(request, communityId)
      val paramMap = ParameterExtractor.paramMap(request)
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      var stylesheetToUse: Option[Path] = None
      var resultToReturn: Option[Result] = None
      val useStyleSheet = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.USE_STYLE_SHEET).toBoolean
      if (useStyleSheet && settings.customPdfsWithCustomXml) {
        val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
        if (stylesheetInfo._2.isEmpty) {
          if (stylesheetInfo._1.isDefined) {
            // Stylesheet provided as part of the request
            stylesheetToUse = Some(stylesheetInfo._1.get.toPath)
          } else {
            // Already existing stylesheet
            stylesheetToUse = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
          }
        } else {
          resultToReturn = stylesheetInfo._2
        }
      }
      if (resultToReturn.isEmpty) {
        handler.apply(reportPath, stylesheetToUse, paramMap)
        Ok.sendFile(
          content = reportPath.toFile,
          fileName = _ => Some(reportName),
          onClose = () => {
            FileUtils.deleteQuietly(reportPath.toFile)
          }
        )
      } else {
        resultToReturn.get
      }
    } catch {
      case e: ServiceCallException => handleDemoReportServiceError(e, reportPath)
      case e: Exception => handleDemoReportGeneralError(e, reportPath)
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  private def handleDemoReportServiceError(e: ServiceCallException, reportPath: Path): Result = {
    FileUtils.deleteQuietly(reportPath.toFile)
    logger.warn("Error while generating PDF report", e)
    val json = if (e.cause.isDefined) {
      JsonUtil.jsErrorMessages(reportManager.extractFailureDetails(e), "text/plain")
    } else if (e.responseBody.exists(StringUtils.isNotBlank)) {
      JsonUtil.jsErrorMessages(List(e.responseBody.get), e.responseContentType.getOrElse("text/plain"))
    } else {
      JsonUtil.jsErrorMessages(List(e.message), "text/plain")
    }
    ResponseConstructor.constructJsonResponse(json.toString())
  }

  private def handleDemoReportGeneralError(e: Exception, reportPath: Path): Result = {
    FileUtils.deleteQuietly(reportPath.toFile)
    logger.warn("Error while generating PDF report", e)
    ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
  }

  private def exportDemoReportInXML(request: RequestWithAttributes[AnyContent], communityId: Long, reportName: String, reportType: ReportType, handler: (Path, Option[Path], Option[Map[String, Seq[String]]]) => Unit): Result = {
    val reportPath = getReportTempFile(".xml")
    try {
      authorizationManager.canManageCommunity(request, communityId)
      val paramMap = ParameterExtractor.paramMap(request)
      val enabled = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.ENABLE).toBoolean
      var stylesheetToUse: Option[Path] = None
      var resultToReturn: Option[Result] = None
      if (enabled) {
        val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
        if (stylesheetInfo._2.isEmpty) {
          if (stylesheetInfo._1.isDefined) {
            // Stylesheet provided as part of the request
            stylesheetToUse = Some(stylesheetInfo._1.get.toPath)
          } else {
            // Already existing stylesheet
            stylesheetToUse = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
          }
        } else {
          resultToReturn = stylesheetInfo._2
        }
      }
      if (resultToReturn.isEmpty) {
        handler.apply(reportPath, stylesheetToUse, paramMap)
        Ok.sendFile(
          content = reportPath.toFile,
          fileName = _ => Some(reportName),
          onClose = () => {
            FileUtils.deleteQuietly(reportPath.toFile)
          }
        )
      } else {
        resultToReturn.get
      }
    } catch {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        logger.warn("Error while generating XML report", e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def loadReportSettings(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val reportType = ReportType.apply(ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort)
    val settings = reportManager.getReportSettings(communityId, reportType)
    val styleSheetExists = repositoryUtils.getCommunityReportStylesheet(communityId, reportType).isDefined
    ResponseConstructor.constructJsonResponse(JsonUtil.jsReportSettings(settings, styleSheetExists).toString)
  }

  def getReportStylesheet(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    val reportType = ReportType.apply(ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort)
    val stylesheet = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
    if (stylesheet.isDefined) {
      Ok.sendFile(
        content = stylesheet.get.toFile,
        fileName = _ => Some("stylesheet.xslt")
      )
    } else {
      NotFound
    }
  }

  def updateConformanceCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    handleUpdateCertificateSettings(communityId, request, (reportSettings, stylesheet, jsSettings) => {
      val certificateSettings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
      reportManager.updateConformanceCertificateSettings(certificateSettings, reportSettings, stylesheet)
    })
  }

  def updateConformanceOverviewCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    handleUpdateCertificateSettings(communityId, request, (reportSettings, stylesheet, jsSettings) => {
      val certificateSettings = JsonUtil.parseJsConformanceOverviewCertificateWithMessages(jsSettings, communityId)
      reportManager.updateConformanceOverviewCertificateSettings(certificateSettings, reportSettings, stylesheet)
    })
  }

  private def handleUpdateCertificateSettings(communityId: Long, request: RequestWithAttributes[AnyContent], certificateSettingsHandler: (CommunityReportSettings, Option[Option[Path]], String) => Unit) = {
    try {
      authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
      val paramMap = ParameterExtractor.paramMap(request)
      val reportSettings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      var result: Option[Result] = None
      var stylesheetFile: Option[Option[Path]] = None
      if (reportSettings.customPdfsWithCustomXml) {
        val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
        result = stylesheetInfo._2
        if (result.isEmpty) {
          if (stylesheetInfo._1.isDefined) {
            stylesheetFile = Some(stylesheetInfo._1.map(_.toPath))
          }
        }
      } else {
        // Delete stylesheet.
        stylesheetFile = Some(None)
      }
      if (result.isEmpty) {
        val jsSettings = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS)
        certificateSettingsHandler.apply(reportSettings, stylesheetFile, jsSettings)
        ResponseConstructor.constructEmptyResponse
      } else {
        result.get
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def updateReportSettings(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageCommunity(request, communityId)
      val paramMap = ParameterExtractor.paramMap(request)
      val reportSettings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      val useStyleSheet = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.USE_STYLE_SHEET).toBoolean
      var stylesheetFile: Option[Option[Path]] = None
      var result: Option[Result] = None
      if (useStyleSheet) {
        val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
        result = stylesheetInfo._2
        if (result.isEmpty) {
          if (stylesheetInfo._1.isDefined) {
            stylesheetFile = Some(stylesheetInfo._1.map(_.toPath))
          }
        }
      } else {
        // Delete stylesheet
        stylesheetFile = Some(None)
      }
      if (result.isEmpty) {
        reportManager.updateReportSettings(reportSettings, stylesheetFile)
        ResponseConstructor.constructEmptyResponse
      } else {
        result.get
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

	def getTestCaseDefinition(testId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, testId)
    val tc = testCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val testCaseId = testId.toLong
      val domainId = testCaseManager.getDomainOfTestCase(testCaseId)
      val file = repositoryUtils.getTestSuitesResource(domainId, tc.get.path, None)
      logger.debug("Reading test case ["+testId+"] definition from the file ["+file+"]")
      if(file.exists()) {
        Ok.sendFile(file, inline = true)
      } else {
        NotFound
      }
    } else {
      NotFound
    }
	}

  def searchTestCases(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewAllTestCases(request)
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
    val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
    val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
    val testSuiteIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.TEST_SUITE_IDS)
    val testCases = testCaseManager.searchTestCases(domainIds, specificationIds, groupIds, actorIds, testSuiteIds)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def searchTestCasesInDomain(): Action[AnyContent] = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canViewDomains(request, Some(List(domainId)))
    val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
    val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
    val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
    val testSuiteIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.TEST_SUITE_IDS)
    val testCases = testCaseManager.searchTestCases(Some(List(domainId)), specificationIds, groupIds, actorIds, testSuiteIds)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def exportDomain(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
      exportManager.exportDomain(domainId, exportSettings)
    })
  }

  def exportDomainAndSettings(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSystemSettings(request)
    exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
      exportManager.exportDomain(domainId, exportSettings)
    })
  }

  def exportCommunity(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
      exportManager.exportCommunity(communityId, exportSettings)
    })
  }

  def exportCommunityAndSettings(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSystemSettings(request)
    exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
      exportManager.exportCommunity(communityId, exportSettings)
    })
  }

  def exportSystemSettings(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewSystemConfigurationValues(request)
    exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
      exportManager.exportSystemSettings(exportSettings)
    })
  }

  def exportDeletions(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canDeleteAnyDomain(request)
    exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
      exportManager.exportDeletions(exportSettings.communitiesToDelete.getOrElse(List.empty[String]), exportSettings.domainsToDelete.getOrElse(List.empty[String]))
    })
  }

  private def exportInternal(request: Request[AnyContent], includeSettings: Boolean, fnExportData: ExportSettings => Export) = {
    // Get export settings to apply.
    val exportSettings = JsonUtil.parseJsExportSettings(requiredBodyParameter(request, Parameters.VALUES), includeSettings)
    val exportData = fnExportData.apply(exportSettings)
    exportData.setVersion(Configurations.mainVersionNumber())
    exportData.setTimestamp(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
    // Define temp file paths.
    val exportPathXml = getReportTempFile(".xml")
    val exportPathZip = getReportTempFile(".zip")
    try {
      // Serialise to temp XML file.
      Using.resource(Files.newOutputStream(exportPathXml)) { xmlOS =>
        XMLUtils.marshalToStream(new JAXBElement(EXPORT_QNAME, classOf[com.gitb.xml.export.Export], exportData), xmlOS)
        xmlOS.flush()
      }
      // Compress
      new ZipArchiver(exportPathXml, exportPathZip, exportSettings.encryptionKey.get.toCharArray).zip()
      // Send
      Ok.sendFile(
        content = exportPathZip.toFile,
        inline = false,
        fileName = _ => Some("data.zip"),
        onClose = () => FileUtils.deleteQuietly(exportPathZip.toFile)
      )
    } catch {
      case e:Exception =>
        FileUtils.deleteQuietly(exportPathZip.toFile)
        throw e
    } finally {
      FileUtils.deleteQuietly(exportPathXml.toFile)
    }
  }

  private def processImport(request: Request[AnyContent], requireDomain: Boolean, requireCommunity: Boolean, requireSettings: Boolean, requireDeletions: Boolean, fnImportData: (Export, ImportSettings) => List[ImportItem]) = {
    // Get import settings.
    val paramMap = ParameterExtractor.paramMap(request)
    val files = ParameterExtractor.extractFiles(request)
    val importSettings = JsonUtil.parseJsImportSettings(ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS))
    if (files.contains(Parameters.FILE)) {
      val result = importPreviewManager.prepareImportPreview(files(Parameters.FILE).file, importSettings, requireDomain, requireCommunity, requireSettings, requireDeletions)
      if (result._1.isDefined) {
        // We have an error.
        ResponseConstructor.constructErrorResponse(result._1.get._1, result._1.get._2, Some("archive"))
      } else {
        // All ok.
        try {
          val importItems = fnImportData.apply(result._2.get, importSettings)
          val json = JsonUtil.jsImportPreviewResult(result._3.get, importItems).toString()
          ResponseConstructor.constructJsonResponse(json)
        } catch {
          case e:Exception =>
            logger.error("An unexpected error occurred while processing the provided archive.", e)
            // Delete the temporary file.
            if (result._4.isDefined) {
              FileUtils.deleteQuietly(result._4.get.toFile)
            }
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An error occurred while processing the provided archive.")
        }
      }
    } else {
      ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "No archive was provided")
    }
  }

  def uploadSystemSettingsExport(): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageSystemSettings(request)
      processImport(request, requireDomain = false, requireCommunity = false, requireSettings = true, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
        val result = importPreviewManager.previewSystemSettingsImport(exportData.getSettings)
        List(result)
      })
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadDeletionsExport(): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canDeleteAnyDomain(request)
      processImport(request, requireDomain = false, requireCommunity = false, requireSettings = false, requireDeletions = true, (exportData: Export, settings: ImportSettings) => {
        importPreviewManager.previewDeletionsImport(exportData.getDeletions)
      })
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadCommunityExportTestBedAdmin(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    uploadCommunityExportInternal(request, communityId, canDoAdminOperations = true)
  }

  def uploadCommunityExportCommunityAdmin(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    uploadCommunityExportInternal(request, communityId, canDoAdminOperations = false)
  }

  private def emptyForNegativeId(id: Long): Option[Long] = {
    if (id == -1) {
      None
    } else {
      Some(id)
    }
  }

  private def uploadCommunityExportInternal(request: Request[AnyContent], communityId: Long, canDoAdminOperations: Boolean) = {
    try {
      processImport(request, requireDomain = false, requireCommunity = true, requireSettings = false, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
        val result = importPreviewManager.previewCommunityImport(exportData, emptyForNegativeId(communityId), canDoAdminOperations, settings)
        val items = new ListBuffer[ImportItem]()
        // First add domain.
        if (result._2.isDefined) {
          items += result._2.get
        }
        // Next add community.
        items += result._1
        // Finally add system settings.
        if (result._3.isDefined) {
          items += result._3.get
        }
        items.toList
      })
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadDomainExportTestBedAdmin(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canCreateDomain(request)
    uploadDomainExportInternal(request, domainId, canDoAdminOperations = true)
  }

  def uploadDomainExportCommunityAdmin(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    uploadDomainExportInternal(request, domainId, canDoAdminOperations = false)
  }

  private def uploadDomainExportInternal(request: Request[AnyContent], domainId: Long, canDoAdminOperations: Boolean): Result = {
    try {
      processImport(request, requireDomain = true, requireCommunity = false, requireSettings = false, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
        val result = importPreviewManager.previewDomainImport(exportData.getDomains.getDomain.get(0), emptyForNegativeId(domainId), canDoAdminOperations, settings)
        List(result)
      })
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def cancelImportInternal(request: Request[AnyContent]) = {
    val pendingImportId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
    val pendingFolder = Paths.get(importPreviewManager.getPendingFolder().getAbsolutePath, pendingImportId)
    // Delete temporary folder (if exists)
    FileUtils.deleteQuietly(pendingFolder.toFile)
    ResponseConstructor.constructEmptyResponse
  }

  def cancelDomainImport(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    cancelImportInternal(request)
  }

  def cancelSystemSettingsImport(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSystemSettings(request)
    cancelImportInternal(request)
  }

  def cancelCommunityImport(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    cancelImportInternal(request)
  }

  private def confirmImportInternal(request: Request[AnyContent], fnImportData: (Export, ImportSettings, List[ImportItem]) => _) = {
    val pendingImportId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
    val pendingFolder = Paths.get(importPreviewManager.getPendingFolder().getAbsolutePath, pendingImportId)
    try {
      // Get XML file and deserialize.
      val xmlFile = importPreviewManager.getPendingImportFile(pendingFolder, pendingImportId)
      if (xmlFile.isDefined) {
        // We can skip XSD validation this time as the time was checked previously (at initial upload).
        val exportData: Export = XMLUtils.unmarshal(classOf[com.gitb.xml.export.Export], new StreamSource(Files.newInputStream(xmlFile.get.toPath)))
        val importSettings = JsonUtil.parseJsImportSettings(ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS))
        importSettings.dataFilePath = Some(xmlFile.get.toPath)
        val importItems = JsonUtil.parseJsImportItems(ParameterExtractor.requiredBodyParameter(request, Parameters.ITEMS))
        fnImportData.apply(exportData, importSettings, importItems)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected failure occurred during the import process.")
      }
    } finally {
      FileUtils.deleteQuietly(pendingFolder.toFile)
    }
  }

  private def confirmDomainImportInternal(request: Request[AnyContent], domainId: Long, canAddOrDeleteDomain: Boolean) = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeDomainImport(export.getDomains.getDomain.asScala.head, importSettings, importItems, emptyForNegativeId(domainId), canAddOrDeleteDomain)
    })
  }

  private def confirmCommunityImportInternal(request: Request[AnyContent], communityId: Long, canDoAdminOperations: Boolean) = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeCommunityImport(export, importSettings, importItems, emptyForNegativeId(communityId), canDoAdminOperations, Some(ParameterExtractor.extractUserId(request)))
    })
  }

  def confirmDomainImportTestBedAdmin(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canCreateDomain(request)
    confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = true)
  }

  def confirmDomainImportCommunityAdmin(domainId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = false)
  }

  def confirmCommunityImportTestBedAdmin(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    confirmCommunityImportInternal(request, communityId, canDoAdminOperations = true)
  }

  def confirmCommunityImportCommunityAdmin(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    confirmCommunityImportInternal(request, communityId, canDoAdminOperations = false)
  }

  def confirmSystemSettingsImport: Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSystemSettings(request)
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeSystemSettingsImport(export.getSettings, importSettings, importItems, canManageSettings = true, Some(ParameterExtractor.extractUserId(request)))
    })
  }

  def confirmDeletionsImport: Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canDeleteAnyDomain(request)
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeDeletionsImport(export.getDeletions, importSettings, importItems)
    })
  }

  def applySandboxData() = authorizedAction(parse.multipartFormData) { request =>
    authorizationManager.canApplySandboxDataMulti(request)
    var response:Result = null
    val archivePassword = ParameterExtractor.requiredBodyParameterMulti(request, Parameters.PASSWORD)
    request.body.file(Parameters.FILE) match {
      case Some(archive) =>
        val archiveFile = archive.ref.path.toFile
        try {
          val importResult = importCompleteManager.importSandboxData(archiveFile, archivePassword)
          if (importResult._1) {
            // Successful - prevent other imports to take place and return
            repositoryUtils.createDataLockFile()
            // The default theme may have changed.
            systemConfigurationManager.reloadThemeCss()
            response = ResponseConstructor.constructEmptyResponse
          } else {
            // Unsuccessful.
            var message: Option[String] = None
            if (importResult._2.isDefined) {
              message = importResult._2
            } else {
              message = Some("An error occurred while processing the archive")
            }
            response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, message.get)
          }
        } finally {
          FileUtils.deleteQuietly(archiveFile)
        }
      case None =>
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
    }
    response
  }

  private def getReportTempFile(suffix: String): Path = {
    repositoryUtils.getReportTempFile(suffix)
  }

}
