package controllers

import java.io._
import java.nio.file.{Files, Paths}

import javax.xml.transform.stream.StreamSource
import com.gitb.tbs.TestStepStatus
import com.gitb.tpl.TestCase
import com.gitb.utils.XMLUtils
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers._
import models.ConformanceCertificate
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil, MimeUtil, RepositoryUtils}

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService @Inject() (testCaseManager: TestCaseManager, testSuiteManager: TestSuiteManager, reportManager: ReportManager, testResultManager: TestResultManager, conformanceManager: ConformanceManager, specificationManager: SpecificationManager) extends Controller {
	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()

  private val TESTCASE_STEP_REPORT_NAME = "step.pdf"

	def getTestSuiteResource(testId: String, filePath:String): Action[AnyContent] = Action {
		implicit request =>
      val testCase = testCaseManager.getTestCaseForIdWrapper(testId).get
      val testSuite = testSuiteManager.getTestSuiteOfTestCaseWrapper(testCase.id)
      var filePathToLookup = codec.decode(filePath)
      if (!filePath.startsWith(testSuite.shortname) && !filePath.startsWith("/"+testSuite.shortname)) {
        // Prefix with the test suite name.
        filePathToLookup = testSuite.shortname + "/" + filePathToLookup
      }
      val file = RepositoryUtils.getTestSuitesResource(specificationManager.getSpecificationById(testCase.targetSpec), filePathToLookup)
			logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
			if(file.exists()) {
				Ok.sendFile(file, true)
			} else {
				NotFound
			}
	}

	def getTestStepReport(sessionId: String, reportPath: String): Action[AnyContent] = Action { implicit request=>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
		val sessionFolder = reportManager.getPathForTestSessionWrapper(sessionId, true).toFile
    var path: String = null
    if (reportPath.startsWith(sessionId)) {
      // Backwards compatibility.
      val pathParts = StringUtils.split(codec.decode(reportPath), "/")
      path = pathParts(1)
    } else {
      path = reportPath
    }
    path = codec.decode(path)
    val file = new File(sessionFolder, path)

    logger.debug("Reading test step report ["+codec.decode(reportPath)+"] from the file ["+file+"]")

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

  def exportTestStepReport(sessionId: String, reportPath: String): Action[AnyContent] = Action { implicit request=>
    //    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
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
      } else {
        NotFound
      }
    }
    Ok.sendFile(
      content = pdf,
      fileName = _ => TESTCASE_STEP_REPORT_NAME
    )
  }

  def exportTestCaseReport(): Action[AnyContent] = Action.apply { implicit request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
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
        reportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session, false)
      }
      Ok.sendFile(
        content = exportedReport,
        fileName = _ => exportedReport.getName
      )
    } else {
      NotFound
    }
  }

  def exportTestCaseReports(): Action[AnyContent] = Action { implicit request =>
    NotFound
  }

  def exportConformanceStatementReport(): Action[AnyContent] = Action.apply { implicit request =>
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID)
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID)
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
    reportManager.generateConformanceStatementReport(reportPath, includeTests, actorId.toLong, systemId.toLong)
    Ok.sendFile(
      content = reportPath.toFile,
      fileName = _ => reportPath.toFile.getName
    )
  }

  def exportConformanceCertificateReport(): Action[AnyContent] = Action.apply { implicit request =>
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

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

  def exportDemoConformanceCertificateReport(communityId: Long): Action[AnyContent] = Action.apply { implicit request =>
    val jsSettings = ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS)
    var settings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
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
    reportManager.generateDemoConformanceCertificate(reportPath, settings)
    Ok.sendFile(
      content = reportPath.toFile,
      fileName = _ => reportPath.toFile.getName
    )
  }

	def getTestCase(testId:String) = Action.apply { implicit request =>
		val tc = testCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val json = JsonUtil.jsTestCase(tc.get).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      NotFound
    }
	}

	def getTestCaseDefinition(testId: String) = Action.apply { implicit request =>
		val tc = testCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val file = RepositoryUtils.getTestSuitesResource(specificationManager.getSpecificationById(tc.get.targetSpec), tc.get.path)
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

  def getTestCases() = Action.apply { request =>
    val testCaseIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    val testCases = testCaseManager.getTestCases(testCaseIds)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  private def removeFile(file:File) = {
    file.delete()
  }
}
