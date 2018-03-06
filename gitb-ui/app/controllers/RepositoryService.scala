package controllers

import java.io._
import java.nio.file.{Files, Paths}
import javax.xml.transform.stream.StreamSource

import com.gitb.tbs.TestStepStatus
import com.gitb.utils.XMLUtils
import config.Configurations
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.{ReportManager, TestCaseManager, TestSuiteManager}
import models.TestCase
import org.apache.commons.codec.net.URLCodec
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil}

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService extends Controller {
	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()

  private val TESTCASE_REPORT_NAME = "report.pdf"
  private val TESTCASE_STEP_REPORT_NAME = "step.pdf"

	def getTestSuitesPath(): File = {

		val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + TestSuiteManager.TEST_SUITES_PATH
	    val path = new File(root);

	    path
	}

	def getStatusUpdatesPath(): File = {

		val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + ReportManager.STATUS_UPDATES_PATH
	    val path = new File(root);

	    path
	}


	def getTestSuiteResource(filePath:String): Action[AnyContent] = Action {
		implicit request =>
			val file = new File(getTestSuitesPath(), codec.decode(filePath))

			logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
			if(file.exists()) {
				Ok.sendFile(file, true)
			} else {
				NotFound
			}
	}

	def getTestStepReport(reportPath: String): Action[AnyContent] = Action { implicit request=>
		val file = new File(getStatusUpdatesPath(), codec.decode(reportPath))

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

  def exportTestStepReport(reportPath: String): Action[AnyContent] = Action { implicit request=>
    val file = new File(getStatusUpdatesPath(), codec.decode(reportPath))
    val pdf = new File(getStatusUpdatesPath(), codec.decode(reportPath.replace(".xml", ".pdf")))

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

  def exportTestCaseReport(): Action[AnyContent] = Action.async { implicit request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    val testCaseId = ParameterExtractor.requiredQueryParameter(request, Parameters.TEST_ID)

    val folder = new File(getStatusUpdatesPath(), codec.decode(session))

    logger.debug("Reading test case report ["+codec.decode(session)+"] from the file ["+folder+"]")

    TestCaseManager.getTestCase(testCaseId) map { testCase =>
      if(folder.exists()) {
        val exportedReport = new File(folder, TESTCASE_REPORT_NAME)

        if(!exportedReport.exists()) {
          val list = ReportManager.getListOfTestSteps(folder)
          ReportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session, false)
        }

        Ok.sendFile(
          content = exportedReport,
          fileName = _ => TESTCASE_REPORT_NAME
        )
      } else {
        NotFound
      }
    }
  }

  def exportTestCaseReports(): Action[AnyContent] = Action { implicit request =>
    val sessionIdsParam  = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_IDS)
    val testCaseIdsParam = ParameterExtractor.requiredQueryParameter(request, Parameters.TEST_IDS)

    val sessionIds  = sessionIdsParam.split(",")
    val testCaseIds = testCaseIdsParam.split(",")

    val doc = new XWPFDocument

    ReportManager.generateTestCaseOverviewPage(doc, testCaseIds, sessionIds)

    val report = new File(getStatusUpdatesPath(), System.currentTimeMillis() + ".docx")
    val out = new FileOutputStream(report);
    doc.write(out)

    Ok.sendFile(
      content  = report,
      fileName = _ => TESTCASE_REPORT_NAME,
      onClose  = () => report.delete()
    )
  }

	def getTestCase(testId:String) = Action.async {
		TestCaseManager.getTestCase(testId) map {
			case Some(tc: TestCase) => {
				val json = JsonUtil.jsTestCase(tc).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
			case _ => NotFound
		}
	}

	def getTestCaseDefinition(testId: String) = Action.async {
		TestCaseManager.getTestCase(testId) map {
			case Some(tc: TestCase) => {
				val file = new File(getTestSuitesPath(), tc.path)
				logger.debug("Reading test case ["+testId+"] definition from the file ["+file+"]")
				if(file.exists()) {
					Ok.sendFile(file, true)
				} else {
					NotFound
				}
			}
			case _ => NotFound
		}
	}

  def getTestCases() = Action.async { request =>
    val testCaseIds = ParameterExtractor.extractLongIdsQueryParameter(request)

    TestCaseManager.getTestCases(testCaseIds) map { testCases =>
      val json = JsonUtil.jsTestCasesList(testCases).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  private def removeFile(file:File) = {
    file.delete()
  }
}
