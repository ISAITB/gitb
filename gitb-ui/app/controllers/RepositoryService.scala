package controllers

import java.io._
import java.nio.file.{Files, Paths}
import javax.xml.transform.stream.StreamSource

import com.gitb.tbs.TestStepStatus
import com.gitb.tpl.TestCase
import com.gitb.utils.XMLUtils
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.{ReportManager, TestCaseManager, TestResultManager}
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil, RepositoryUtils}

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService extends Controller {
	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()

  private val TESTCASE_STEP_REPORT_NAME = "step.pdf"

	def getTestSuiteResource(testId: String, filePath:String): Action[AnyContent] = Action {
		implicit request =>
      val testCase = TestCaseManager.getTestCaseForId(testId).get
      val file = RepositoryUtils.getTestSuitesResource(testCase.targetSpec, codec.decode(filePath))
			logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
			if(file.exists()) {
				Ok.sendFile(file, true)
			} else {
				NotFound
			}
	}

	def getTestStepReport(sessionId: String, reportPath: String): Action[AnyContent] = Action { implicit request=>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
		val sessionFolder = ReportManager.getPathForTestSession(sessionId, true).toFile
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
    val sessionFolder = ReportManager.getPathForTestSession(sessionId, true).toFile
    val file = new File(sessionFolder, path)
    val pdf = new File(sessionFolder, path.toLowerCase().replace(".xml", ".pdf"))

    if (!pdf.exists()) {
      if (file.exists()) {
        ReportManager.generateTestStepReport(file.toPath, pdf.toPath)
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

    val folder = ReportManager.getPathForTestSession(codec.decode(session), true).toFile

    logger.debug("Reading test case report ["+codec.decode(session)+"] from the file ["+folder+"]")

    val testResult = TestResultManager.getTestResultForSession(session)
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
      val testCase = TestCaseManager.getTestCase(testCaseId)
      if (!exportedReport.exists()) {
        val list = ReportManager.getListOfTestSteps(testcasePresentation, folder)
        ReportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session, false)
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

	def getTestCase(testId:String) = Action.apply { implicit request =>
		val tc = TestCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val json = JsonUtil.jsTestCase(tc.get).toString()
      ResponseConstructor.constructJsonResponse(json)
    } else {
      NotFound
    }
	}

	def getTestCaseDefinition(testId: String) = Action.apply { implicit request =>
		val tc = TestCaseManager.getTestCase(testId)
    if (tc.isDefined) {
      val file = RepositoryUtils.getTestSuitesResource(tc.get.targetSpec, tc.get.path)
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

    val testCases = TestCaseManager.getTestCases(testCaseIds)
    val json = JsonUtil.jsTestCasesList(testCases).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  private def removeFile(file:File) = {
    file.delete()
  }
}
