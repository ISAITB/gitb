package controllers

import java.io.{StringReader, File}
import java.nio.file.{Paths, Files}
import javax.xml.transform.stream.StreamSource


import com.gitb.tbs.TestStepStatus
import com.gitb.utils.XMLUtils
import config.Configurations
import models.TestCase
import org.apache.commons.codec.net.URLCodec
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import controllers.util.ResponseConstructor
import managers.{TestSuiteManager, ReportManager, TestCaseManager}
import play.api.Play
import play.api.mvc._
import utils.{JacksonUtil, JsonUtil}

/**
 * Created by serbay on 10/16/14.
 */
class RepositoryService extends Controller {
	private val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private val codec = new URLCodec()

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
}
