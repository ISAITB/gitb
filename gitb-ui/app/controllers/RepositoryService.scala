package controllers

import com.gitb.tbs.TestStepStatus
import com.gitb.tpl.TestCase
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import com.gitb.xml.export.Export
import config.Configurations
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers._
import managers.export._
import models.{ConformanceCertificate, ConformanceCertificates, Constants, TestSuites}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils._

import java.io._
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import scala.concurrent.ExecutionContext

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, systemManager: SystemManager, testCaseManager: TestCaseManager, testSuiteManager: TestSuiteManager, reportManager: ReportManager, testResultManager: TestResultManager, conformanceManager: ConformanceManager, specificationManager: SpecificationManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager, exportManager: ExportManager, importPreviewManager: ImportPreviewManager, importCompleteManager: ImportCompleteManager, repositoryUtils: RepositoryUtils) extends AbstractController(cc) {

	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()
  private val EXPORT_QNAME:QName = new QName("http://www.gitb.com/export/v1/", "export")
  private val TESTCASE_STEP_REPORT_NAME = "step.pdf"

  import scala.collection.JavaConverters._

	def getTestSuiteResource(locationKey: String, filePath:String) = authorizedAction { request =>
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
    val testCase = testCaseManager.getTestCaseForIdWrapper(testIdentifier.get).get
    val specificationOfTestCase = specificationManager.getSpecificationById(testCase.targetSpec)
    var testSuite: Option[TestSuites] = None
    if (testSuiteIdentifier.isEmpty) {
      // Consider test suite of test case.
      testSuite = Some(testSuiteManager.getTestSuiteOfTestCaseWrapper(testCase.id))
    } else {
      // Find test suite in specification or domain.
      testSuite = repositoryUtils.findTestSuiteByIdentifier(testSuiteIdentifier.get, Some(specificationOfTestCase.domain), specificationOfTestCase.id)
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
      val testSuiteFolder = repositoryUtils.getTestSuitesResource(testSuite.get.specification, specificationOfTestCase.domain, testSuite.get.filename, None)
      val file = repositoryUtils.getTestSuitesResource(testSuite.get.specification, specificationOfTestCase.domain, filePathToLookup, filePathToAlsoCheck)
      logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
      if (file.exists() && file.toPath.normalize().startsWith(testSuiteFolder.toPath.normalize())) {
        Ok.sendFile(file, inline = true)
      } else {
        NotFound
      }
    }
	}

	def getTestStepReport(sessionId: String, reportPath: String) = authorizedAction { request =>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId)

		val sessionFolderInfo = reportManager.getPathForTestSessionWrapper(sessionId, isExpected = true)
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
        val bytes  = Files.readAllBytes(Paths.get(file.getAbsolutePath));
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

  def exportTestStepReport(sessionId: String, reportPath: String) = authorizedAction { request =>
    //    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId)
    var path: String = null
    if (reportPath.startsWith(sessionId)) {
      // Backwards compatibility.
      val pathParts = StringUtils.split(codec.decode(reportPath), "/")
      path = pathParts(1)
    } else {
      path = reportPath
    }
    path = codec.decode(path)
    val sessionFolderInfo = reportManager.getPathForTestSessionWrapper(sessionId, isExpected = true)
    try {
      val file = new File(sessionFolderInfo.path.toFile, path)
      val pdf = new File(sessionFolderInfo.path.toFile, path.toLowerCase().replace(".xml", ".pdf"))

      if (!pdf.exists()) {
        if (file.exists()) {
          reportManager.generateTestStepReport(file.toPath, pdf.toPath)
        }
      }
      if (!pdf.exists()) {
        NotFound
      } else {
        Ok.sendFile(
          content = pdf,
          fileName = _ => Some(TESTCASE_STEP_REPORT_NAME),
          onClose = () => {
            if (sessionFolderInfo.archived) {
              FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
            }
          }
        )
      }
    } catch {
      case e: Exception =>
        if (sessionFolderInfo.archived) {
          FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
        }
        throw e
    }
  }

  def exportTestCaseReport() = authorizedAction { request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, session)
    val testCaseId = ParameterExtractor.requiredQueryParameter(request, Parameters.TEST_ID)

    val sessionFolderInfo = reportManager.getPathForTestSessionWrapper(codec.decode(session), isExpected = true)
    try {
      logger.debug("Reading test case report [" + codec.decode(session) + "] from the file [" + sessionFolderInfo + "]")
      val testResult = testResultManager.getTestResultForSessionWrapper(session)
      if (testResult.isDefined) {
        var exportedReport: File = null
        if (testResult.get.endTime.isEmpty) {
          // This name will be unique to ensure that a report generated for a pending session never gets cached.
          exportedReport = new File(sessionFolderInfo.path.toFile, "report_" + System.currentTimeMillis() + ".pdf")
          FileUtils.forceDeleteOnExit(exportedReport)
        } else {
          exportedReport = new File(sessionFolderInfo.path.toFile, "report.pdf")
        }
        val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult.get.tpl)))
        val testCase = testCaseManager.getTestCase(testCaseId)
        if (!exportedReport.exists()) {
          val list = reportManager.getListOfTestSteps(testcasePresentation, sessionFolderInfo.path.toFile)
          val labels = communityLabelManager.getLabels(request)
          reportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session, false, labels)
        }
        Ok.sendFile(
          content = exportedReport,
          fileName = _ => Some(exportedReport.getName),
          onClose = () => {
            if (sessionFolderInfo.archived) {
              FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
            }
          }
        )
      } else {
        NotFound
      }
    } catch {
      case e: Exception =>
        if (sessionFolderInfo.archived) {
          FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
        }
        throw e
    }
  }

  def exportConformanceStatementReport() = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID)
    authorizationManager.canViewConformanceStatementReport(request, systemId)
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID)
    val includeTests = ParameterExtractor.requiredQueryParameter(request, Parameters.TESTS).toBoolean
    val reportPath = Paths.get(
      repositoryUtils.getTempReportFolder().getAbsolutePath,
      "conformance_reports",
      actorId,
      systemId,
      "report_"+System.currentTimeMillis+".pdf"
    )
    FileUtils.deleteQuietly(reportPath.toFile.getParentFile)
    try {
      val labels = communityLabelManager.getLabels(request)
      reportManager.generateConformanceStatementReport(reportPath, includeTests, actorId.toLong, systemId.toLong, labels)
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

  private def exportConformanceCertificateInternal(settings: ConformanceCertificates, communityId: Long, systemId: Long, actorId: Long) = {
    val reportPath = Paths.get(
      repositoryUtils.getTempReportFolder().getAbsolutePath,
      "conformance_reports",
      "c"+communityId+"_a"+actorId+"_s"+systemId,
      "report_"+System.currentTimeMillis+".pdf"
    )
    FileUtils.deleteQuietly(reportPath.toFile.getParentFile)
    try {
      reportManager.generateConformanceCertificate(reportPath, settings, actorId, systemId)
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

  def exportOwnConformanceCertificateReport() = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewOwnConformanceCertificateReport(request, systemId)
    val communityId = systemManager.getCommunityIdOfSystem(systemId)
    var settingsToUse: Option[ConformanceCertificates] = None
    val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
    if (storedSettings.isEmpty) {
      // Use default settings.
      settingsToUse = Some(ConformanceCertificates(0L, None, None, includeMessage = false, includeTestStatus = true, includeTestCases = true, includeDetails = true, includeSignature = false, None, None, None, None, communityId))
    } else {
      val completeSettings = new ConformanceCertificate(storedSettings.get)
      if (storedSettings.get.includeSignature) {
        completeSettings.keystoreFile = storedSettings.get.keystoreFile
        completeSettings.keystoreType = storedSettings.get.keystoreType
        completeSettings.keyPassword = Some(MimeUtil.decryptString(storedSettings.get.keyPassword.get))
        completeSettings.keystorePassword = Some(MimeUtil.decryptString(storedSettings.get.keystorePassword.get))
        settingsToUse = Some(completeSettings.toCaseObject)
      } else {
        settingsToUse = storedSettings
      }
    }
    exportConformanceCertificateInternal(settingsToUse.get, communityId, systemId, actorId)
  }

  def exportConformanceCertificateReport() = authorizedAction { request =>
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewConformanceCertificateReport(request, communityId)

    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    var settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
    if (settings.includeSignature) {
      // The signature information needs to be looked up from the stored data.
      val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
      val completeSettings = new ConformanceCertificate(settings)
      completeSettings.keystoreFile = storedSettings.get.keystoreFile
      completeSettings.keystoreType = storedSettings.get.keystoreType
      completeSettings.keyPassword = Some(MimeUtil.decryptString(storedSettings.get.keyPassword.get))
      completeSettings.keystorePassword = Some(MimeUtil.decryptString(storedSettings.get.keystorePassword.get))
      settings = completeSettings.toCaseObject
    }
    exportConformanceCertificateInternal(settings, communityId, systemId, actorId)
  }

  def exportDemoConformanceCertificateReport(communityId: Long) = authorizedAction { request =>
    authorizationManager.canPreviewConformanceCertificateReport(request, communityId)
    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    var settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)

    var response: Result = null
    if (settings.keystoreFile.isDefined && Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      val scanResult = virusScanner.scan(Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.keystoreFile.get)))
      if (!ClamAVClient.isCleanReply(scanResult)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan.")
      }
    }
    if (response == null) {
      val reportPath = Paths.get(
        repositoryUtils.getTempReportFolder().getAbsolutePath,
        "conformance_reports",
        "c"+communityId,
        "report_"+System.currentTimeMillis+".pdf"
      )
      FileUtils.deleteQuietly(reportPath.toFile.getParentFile)
      if (settings.includeSignature && (settings.keystorePassword.isEmpty || settings.keyPassword.isEmpty)) {
        // The passwords need to be looked up from the stored data.
        val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
        val completeSettings = new ConformanceCertificate(settings)
        completeSettings.keyPassword = Some(MimeUtil.decryptString(storedSettings.get.keyPassword.get))
        completeSettings.keystorePassword = Some(MimeUtil.decryptString(storedSettings.get.keystorePassword.get))
        settings = completeSettings.toCaseObject
      }
      try {
        reportManager.generateDemoConformanceCertificate(reportPath, settings, communityId)
        response = Ok.sendFile(
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
    response
  }

	def getTestCaseDefinition(testId: String) = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, testId)
    val tc = testCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val file = repositoryUtils.getTestSuitesResource(specificationManager.getSpecificationById(tc.get.targetSpec), tc.get.path, None)
      logger.debug("Reading test case ["+testId+"] definition from the file ["+file+"]")
      if(file.exists()) {
        Ok.sendFile(file, true)
      } else {
        NotFound
      }
    } else {
      NotFound
    }
	}

  def getAllTestCases() = authorizedAction { request =>
    authorizationManager.canViewAllTestCases(request)
    val testCases = testCaseManager.getAllTestCases()
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestCasesForSystem(systemId: Long) = authorizedAction { request =>
    authorizationManager.canViewTestCasesBySystemId(request, systemId)
    val testCases = testCaseManager.getTestCasesForSystem(systemId)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestCasesForCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canViewTestCasesByCommunityId(request, communityId)
    val testCases = testCaseManager.getTestCasesForCommunity(communityId)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def exportDomain(domainId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    exportInternal(request, (exportSettings: ExportSettings) => {
      exportManager.exportDomain(domainId, exportSettings)
    })
  }

  def exportCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    exportInternal(request, (exportSettings: ExportSettings) => {
      exportManager.exportCommunity(communityId, exportSettings)
    })
  }

  private def exportInternal(request: Request[AnyContent], fnExportData: ExportSettings => Export) = {
    // Get export settings to apply.
    val exportSettings = JsonUtil.parseJsExportSettings(requiredBodyParameter(request, Parameters.VALUES))
    val exportData = fnExportData.apply(exportSettings)
    exportData.setVersion(Constants.VersionNumber)
    exportData.setTimestamp(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
    // Define temp file paths.
    val randomToken = RandomStringUtils.random(10, false, true)
    val exportPathXml = Paths.get(
      repositoryUtils.getTempReportFolder().getAbsolutePath,
      "export",
      "data."+randomToken+".xml"
    )
    val exportPathZip = Paths.get(
      repositoryUtils.getTempReportFolder().getAbsolutePath,
      "export",
      "data."+randomToken+".zip"
    )
    Files.createDirectories(exportPathXml.getParent)
    // Serialise to temp XML file.
    val xmlOS = Files.newOutputStream(exportPathXml)
    try {
      XMLUtils.marshalToStream(new JAXBElement(EXPORT_QNAME, classOf[com.gitb.xml.export.Export], exportData), xmlOS)
      xmlOS.flush()
      xmlOS.close()
      // Compress
      new ZipArchiver(exportPathXml, exportPathZip, exportSettings.encryptionKey.get.toCharArray).zip()
      Ok.sendFile(
        content = exportPathZip.toFile,
        inline = false,
        fileName = _ => Some(exportPathZip.toFile.getName),
        onClose = () => FileUtils.deleteQuietly(exportPathZip.toFile)
      )
    } catch {
      case e:Exception => {
        FileUtils.deleteQuietly(exportPathZip.toFile)
        throw e
      }
    } finally {
      FileUtils.deleteQuietly(exportPathXml.toFile)
    }
  }

  private def processImport(request: Request[AnyContent], isDomain: Boolean, fnImportData: (Export, ImportSettings) => List[ImportItem]) = {
    // Get import settings.
    val importSettings = JsonUtil.parseJsImportSettings(ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS))
    val archiveData = ParameterExtractor.requiredBinaryBodyParameter(request, Parameters.DATA)
    val result = importPreviewManager.prepareImportPreview(archiveData, importSettings, requireDomain = isDomain, requireCommunity = !isDomain)
    if (result._1.isDefined) {
      // We have an error.
      ResponseConstructor.constructBadRequestResponse(result._1.get._1, result._1.get._2)
    } else {
      // All ok.
      try {
        val importItems = fnImportData.apply(result._2.get, importSettings)
        val json = JsonUtil.jsImportPreviewResult(result._3.get, importItems).toString()
        ResponseConstructor.constructJsonResponse(json)
      } catch {
        case e:Exception => {
          logger.error("An unexpected error occurred while processing the provided archive.", e)
          // Delete the temporary file.
          if (result._4.isDefined) {
            FileUtils.deleteQuietly(result._4.get.toFile)
          }
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An error occurred while processing the provided archive.")
        }
      }
    }
  }

  def uploadCommunityExport(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    processImport(request, isDomain = false, (exportData: Export, settings: ImportSettings) => {
      val result = importPreviewManager.previewCommunityImport(exportData.getCommunities.getCommunity.get(0), Some(communityId))
      if (result._2.isDefined) {
        List(result._2.get, result._1)
      } else {
        List(result._1)
      }
    })
  }

  def uploadDomainExport(domainId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    processImport(request, isDomain = true, (exportData: Export, settings: ImportSettings) => {
      val result = importPreviewManager.previewDomainImport(exportData.getDomains.getDomain.get(0), Some(domainId))
      List(result)
    })
  }

  private def cancelImportInternal(request: Request[AnyContent]) = {
    val pendingImportId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
    val pendingFolder = Paths.get(importPreviewManager.getPendingFolder().getAbsolutePath, pendingImportId)
    // Delete temporary folder (if exists)
    FileUtils.deleteQuietly(pendingFolder.toFile)
    ResponseConstructor.constructEmptyResponse
  }

  def cancelDomainImport(domainId: Long) = authorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    cancelImportInternal(request)
  }

  def cancelCommunityImport(communityId: Long) = authorizedAction { request =>
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
      importCompleteManager.completeDomainImport(collectionAsScalaIterable(export.getDomains.getDomain).head, importSettings, importItems, Some(domainId), canAddOrDeleteDomain)
    })
  }

  private def confirmCommunityImportInternal(request: Request[AnyContent], communityId: Long, canAddOrDeleteDomain: Boolean) = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeCommunityImport(collectionAsScalaIterable(export.getCommunities.getCommunity).head, importSettings, importItems, Some(communityId), canAddOrDeleteDomain, Some(ParameterExtractor.extractUserId(request)))
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
    confirmCommunityImportInternal(request, communityId, canAddOrDeleteDomain = true)
  }

  def confirmCommunityImportCommunityAdmin(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    confirmCommunityImportInternal(request, communityId, canAddOrDeleteDomain = false)
  }

  def applySandboxData() = authorizedAction(parse.multipartFormData) { request =>
    authorizationManager.canApplySandboxDataMulti(request)
    var response:Result = null
    val archivePassword = ParameterExtractor.requiredBodyParameterMulti(request, Parameters.PASSWORD)
    request.body.file(Parameters.FILE) match {
      case Some(archive) => {
        val archiveFile = archive.ref.path.toFile
        try {
          val importResult = importCompleteManager.importSandboxData(archiveFile, archivePassword)
          if (importResult._1) {
            // Successful - prevent other imports to take place and return
            repositoryUtils.createDataLockFile()
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
      }
      case None =>
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
    }
    response
  }

}
