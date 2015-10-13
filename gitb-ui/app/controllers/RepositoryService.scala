package controllers

import java.io._
import java.nio.file.{Paths, Files}
import java.util
import javax.xml.transform.stream.StreamSource



import com.gitb.tbs.TestStepStatus
import com.gitb.tr.TestStepReportType
import com.gitb.utils.XMLUtils
import config.Configurations
import models.TestCase
import org.apache.commons.codec.net.URLCodec
import org.apache.poi.xwpf.usermodel.{ParagraphAlignment, XWPFDocument}
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.{TestSuiteManager, ReportManager, TestCaseManager}
import play.api.Play
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil}

import scala.collection.mutable.ListBuffer

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService extends Controller {
	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()

  private val TESTCASE_REPORT_NAME = "report.docx"

	def getTestSuiteResource(filePath:String): Action[AnyContent] = Action {
		implicit request =>
			val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + TestSuiteManager.TEST_SUITES_PATH
			val application = Play.current
			val file = new File(application.getFile(root), codec.decode(filePath))

			logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
			if(file.exists()) {
				Ok.sendFile(file, true)
			} else {
				NotFound
			}
	}

	def getTestStepReport(reportPath: String): Action[AnyContent] = Action { implicit request=>
		val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + ReportManager.STATUS_UPDATES_PATH

		val application = Play.current
		val file = new File(application.getFile(root), codec.decode(reportPath))

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
    val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + ReportManager.STATUS_UPDATES_PATH

    val application = Play.current
    val file = new File(application.getFile(root), codec.decode(reportPath))
    var docx = new File(application.getFile(root), codec.decode(reportPath.replace(".xml", ".docx")))

    if(!docx.exists()) {
      if(file.exists()) {
        //read file into a string
        val bytes  = Files.readAllBytes(Paths.get(file.getAbsolutePath));
        val string = new String(bytes)

        //convert string in xml format into its object representation
        val step = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))

        //generate pdf
        docx = ReportManager.generateDetailedTestStepReport(step.getReport, docx.getAbsolutePath)
      } else {
        NotFound
      }
    }

    println(docx.getAbsolutePath)
    Ok.sendFile(docx, true)
  }

  def exportTestCaseReport(): Action[AnyContent] = Action.async { implicit request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    val testCaseId = ParameterExtractor.requiredQueryParameter(request, Parameters.TEST_ID)

    val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + ReportManager.STATUS_UPDATES_PATH

    val application = Play.current
    val folder = new File(application.getFile(root), codec.decode(session))

    logger.debug("Reading test case report ["+codec.decode(session)+"] from the file ["+folder+"]")

    TestCaseManager.getTestCase(testCaseId) map { testCase =>
      if(folder.exists()) {
        val exportedReport = new File(folder, TESTCASE_REPORT_NAME)

        if(!exportedReport.exists()) {
          val list = ReportManager.getListOfTestSteps(folder)
          ReportManager.generateDetailedTestCaseReport(list, exportedReport.getAbsolutePath, testCase, session)
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
    val root: String = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + ReportManager.STATUS_UPDATES_PATH
    val application = Play.current

    val sessionIds  = sessionIdsParam.split(",")
    val testCaseIds = testCaseIdsParam.split(",")

    val doc = new XWPFDocument

    ReportManager.generateTestCaseOverviewPage(doc, testCaseIds, sessionIds)

    val report = new File(application.getFile(root), System.currentTimeMillis() + ".docx")
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
				val root = Configurations.TEST_CASE_REPOSITORY_PATH + "/" + TestSuiteManager.TEST_SUITES_PATH
				val application = Play.current
				val file = new File(application.getFile(root), tc.path)
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

  private def removeFile(file:File) = {
    file.delete()
  }
}
