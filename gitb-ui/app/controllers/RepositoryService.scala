package controllers

import java.io._
import java.nio.file.{Files, Path, Paths}

import com.gitb.tbs.TestStepStatus
import com.gitb.tpl.TestCase
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import com.gitb.xml.export.Export
import config.Configurations
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import managers._
import managers.export.{ExportManager, ExportSettings, ImportCompleteManager, ImportItem, ImportPreviewManager, ImportSettings}
import models.{ConformanceCertificate, Constants}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils._

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService @Inject() (testCaseManager: TestCaseManager, testSuiteManager: TestSuiteManager, reportManager: ReportManager, testResultManager: TestResultManager, conformanceManager: ConformanceManager, specificationManager: SpecificationManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager, exportManager: ExportManager, importPreviewManager: ImportPreviewManager, importCompleteManager: ImportCompleteManager) extends Controller {

	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()
  private val EXPORT_QNAME:QName = new QName("http://www.gitb.com/export/v1/", "export")
  private val TESTCASE_STEP_REPORT_NAME = "step.pdf"

  import scala.collection.JavaConversions._

	def getTestSuiteResource(testId: String, filePath:String) = AuthorizedAction { request =>
    authorizationManager.canViewTestSuiteResource(request, testId)
    val testCase = testCaseManager.getTestCaseForIdWrapper(testId).get
    val testSuite = testSuiteManager.getTestSuiteOfTestCaseWrapper(testCase.id)
    var filePathToLookup = codec.decode(filePath)
    var filePathToAlsoCheck: Option[String] = null
    if (!filePath.startsWith(testSuite.shortname) && !filePath.startsWith("/"+testSuite.shortname)) {
      filePathToLookup = testSuite.filename + "/" + filePathToLookup
      filePathToAlsoCheck = None
    } else {
      filePathToAlsoCheck = Some(testSuite.filename + "/" + filePathToLookup)
      filePathToLookup = StringUtils.replaceOnce(filePathToLookup, testSuite.shortname, testSuite.filename)
    }
    // Ensure that the requested resource is within the test suite folder (to avoid path traversal)
    val spec = specificationManager.getSpecificationById(testSuite.specification)
    val testSuiteFolder = RepositoryUtils.getTestSuitesResource(spec, testSuite.filename, None)
    val file = RepositoryUtils.getTestSuitesResource(spec, filePathToLookup, filePathToAlsoCheck)
    logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
    if (file.exists() && file.toPath.normalize().startsWith(testSuiteFolder.toPath.normalize())) {
      Ok.sendFile(file, true)
    } else {
      NotFound
    }
	}

	def getTestStepReport(sessionId: String, reportPath: String) = AuthorizedAction { request =>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId)

		val sessionFolder = reportManager.getPathForTestSessionWrapper(sessionId, true).toFile
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
    val file = new File(sessionFolder, path)

		if(file.exists()) {
      //read file incto a string
      val bytes  = Files.readAllBytes(Paths.get(file.getAbsolutePath));
      val string = new String(bytes)

      //convert string in xml format into its object representation
      val step = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))

      //serialize report inside the object into json
			ResponseConstructor.constructJsonResponse(JacksonUtil.serializeTestReport(step.getReport))
		} else {
			NotFound
		}
	}

  def exportTestStepReport(sessionId: String, reportPath: String) = AuthorizedAction { request =>
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
    val sessionFolder = reportManager.getPathForTestSessionWrapper(sessionId, true).toFile
    val file = new File(sessionFolder, path)
    val pdf = new File(sessionFolder, path.toLowerCase().replace(".xml", ".pdf"))

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
        fileName = _ => TESTCASE_STEP_REPORT_NAME
      )
    }
  }

  def exportTestCaseReport() = AuthorizedAction { request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, session)
    val testCaseId = ParameterExtractor.requiredQueryParameter(request, Parameters.TEST_ID)

    val folder = reportManager.getPathForTestSessionWrapper(codec.decode(session), true).toFile

    logger.debug("Reading test case report ["+codec.decode(session)+"] from the file ["+folder+"]")

    val testResult = testResultManager.getTestResultForSessionWrapper(session)
    if (testResult.isDefined) {

      var exportedReport: File = null
      if (testResult.get.endTime.isEmpty) {
        // This name will be unique to ensure that a report generated for a pending session never gets cached.
        exportedReport = new File(folder, "report_" + System.currentTimeMillis() + ".pdf")
        FileUtils.forceDeleteOnExit(exportedReport)
      } else {
        exportedReport = new File(folder, "report.pdf")
      }
      val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult.get.tpl)))
      val testCase = testCaseManager.getTestCase(testCaseId)
      if (!exportedReport.exists()) {
        val list = reportManager.getListOfTestSteps(testcasePresentation, folder)
        val labels = communityLabelManager.getLabels(request)
        reportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session, false, labels)
      }
      Ok.sendFile(
        content = exportedReport,
        fileName = _ => exportedReport.getName
      )
    } else {
      NotFound
    }
  }

  def exportConformanceStatementReport() = AuthorizedAction { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID)
    authorizationManager.canViewConformanceStatementReport(request, systemId)
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID)
    val includeTests = ParameterExtractor.requiredQueryParameter(request, Parameters.TESTS).toBoolean
    val reportPath = Paths.get(
      ReportManager.getTempFolderPath().toFile.getAbsolutePath,
      "conformance_reports",
      actorId,
      systemId,
      "report_"+System.currentTimeMillis+".pdf"
    )
    try {
      FileUtils.deleteDirectory(reportPath.toFile.getParentFile)
    } catch  {
      case e:Exception => {
        // Ignore these are anyway deleted every hour
      }
    }
    val labels = communityLabelManager.getLabels(request)
    reportManager.generateConformanceStatementReport(reportPath, includeTests, actorId.toLong, systemId.toLong, labels)
    Ok.sendFile(
      content = reportPath.toFile,
      fileName = _ => reportPath.toFile.getName
    )
  }

  def exportConformanceCertificateReport() = AuthorizedAction { request =>
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewConformanceCertificateReport(request, communityId)

    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    var settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
    val reportPath = Paths.get(
      ReportManager.getTempFolderPath().toFile.getAbsolutePath,
      "conformance_reports",
      "c"+communityId+"_a"+actorId+"_s"+systemId,
      "report_"+System.currentTimeMillis+".pdf"
    )
    try {
      FileUtils.deleteDirectory(reportPath.toFile.getParentFile)
    } catch  {
      case e:Exception => {
        // Ignore these are anyway deleted every hour
      }
    }
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
    reportManager.generateConformanceCertificate(reportPath, settings, actorId, systemId)
    Ok.sendFile(
      content = reportPath.toFile,
      fileName = _ => reportPath.toFile.getName
    )
  }

  def exportDemoConformanceCertificateReport(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewConformanceCertificateReport(request, communityId)
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
        ReportManager.getTempFolderPath().toFile.getAbsolutePath,
        "conformance_reports",
        "c"+communityId,
        "report_"+System.currentTimeMillis+".pdf"
      )
      try {
        FileUtils.deleteDirectory(reportPath.toFile.getParentFile)
      } catch  {
        case e:Exception => {
          // Ignore these are anyway deleted every hour
        }
      }
      if (settings.includeSignature && (settings.keystorePassword.isEmpty || settings.keyPassword.isEmpty)) {
        // The passwords need to be looked up from the stored data.
        val storedSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
        val completeSettings = new ConformanceCertificate(settings)
        completeSettings.keyPassword = Some(MimeUtil.decryptString(storedSettings.get.keyPassword.get))
        completeSettings.keystorePassword = Some(MimeUtil.decryptString(storedSettings.get.keystorePassword.get))
        settings = completeSettings.toCaseObject
      }
      reportManager.generateDemoConformanceCertificate(reportPath, settings, communityId)
      response = Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => reportPath.toFile.getName
      )
    }
    response
  }

	def getTestCaseDefinition(testId: String) = AuthorizedAction { request =>
    authorizationManager.canViewTestCase(request, testId)
    val tc = testCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val file = RepositoryUtils.getTestSuitesResource(specificationManager.getSpecificationById(tc.get.targetSpec), tc.get.path, None)
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

  def getAllTestCases() = AuthorizedAction { request =>
    authorizationManager.canViewAllTestCases(request)
    val testCases = testCaseManager.getAllTestCases()
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestCasesForSystem(systemId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestCasesBySystemId(request, systemId)
    val testCases = testCaseManager.getTestCasesForSystem(systemId)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestCasesForCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canViewTestCasesByCommunityId(request, communityId)
    val testCases = testCaseManager.getTestCasesForCommunity(communityId)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def exportDomain(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    exportInternal(request, (exportSettings: ExportSettings) => {
      exportManager.exportDomain(domainId, exportSettings)
    })
  }

  def exportCommunity(communityId: Long) = AuthorizedAction { request =>
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
      ReportManager.getTempFolderPath().toFile.getAbsolutePath,
      "export",
      "data."+randomToken+".xml"
    )
    val exportPathZip = Paths.get(
      ReportManager.getTempFolderPath().toFile.getAbsolutePath,
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
        fileName = _ => exportPathZip.toFile.getName,
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

  def uploadCommunityExport(communityId: Long) = AuthorizedAction { request =>
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

  def uploadDomainExport(domainId: Long) = AuthorizedAction { request =>
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

  def cancelDomainImport(domainId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    cancelImportInternal(request)
  }

  def cancelCommunityImport(communityId: Long) = AuthorizedAction { request =>
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
      importCompleteManager.completeDomainImport(export.getDomains.getDomain.head, importSettings, importItems, Some(domainId), canAddOrDeleteDomain)
    })
  }

  private def confirmCommunityImportInternal(request: Request[AnyContent], communityId: Long, canAddOrDeleteDomain: Boolean) = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeCommunityImport(export.getCommunities.getCommunity.head, importSettings, importItems, Some(communityId), canAddOrDeleteDomain, Some(ParameterExtractor.extractUserId(request)))
    })
  }

  def confirmDomainImportTestBedAdmin(domainId: Long): Action[AnyContent] = AuthorizedAction { request =>
    authorizationManager.canCreateDomain(request)
    confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = true)
  }

  def confirmDomainImportCommunityAdmin(domainId: Long): Action[AnyContent] = AuthorizedAction { request =>
    authorizationManager.canManageDomain(request, domainId)
    confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = false)
  }

  def confirmCommunityImportTestBedAdmin(communityId: Long): Action[AnyContent] = AuthorizedAction { request =>
    authorizationManager.canCreateCommunity(request)
    confirmCommunityImportInternal(request, communityId, canAddOrDeleteDomain = true)
  }

  def confirmCommunityImportCommunityAdmin(communityId: Long): Action[AnyContent] = AuthorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    confirmCommunityImportInternal(request, communityId, canAddOrDeleteDomain = false)
  }

}
