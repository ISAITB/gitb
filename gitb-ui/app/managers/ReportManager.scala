package managers

import java.io.{StringReader, FileOutputStream, File}
import javax.xml.transform.stream.StreamSource

import com.gitb.core.StepStatus
import com.gitb.tbs.{ObjectFactory, TestStepStatus}
import com.gitb.tpl.TestCase
import com.gitb.tr.TestResultType
import com.gitb.utils.XMLUtils
import config.Configurations
import models.{TestResultReport, TestStepResult, TestResult}
import org.slf4j.{LoggerFactory, Logger}
import persistence.db.PersistenceSchema
import play.api.Play

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._
import utils.{JacksonUtil, TimeUtil}

/**
 * Created by senan on 03.12.2014.
 */
object ReportManager extends BaseManager {

	val STATUS_UPDATES_PATH: String = "status-updates"
	val logger: Logger = LoggerFactory.getLogger("ReportManager")

  def getTestResults(systemId:Long, _page: Option[Long] = None, _limit:Option[Long] = None): Future[List[TestResultReport]] = {
    Future{
      DB.withSession { implicit session =>
	      val page = _page match {
		      case Some(p) => p
		      case None => 0l
	      }

	      val limit = _limit match {
		      case Some(l) => l
		      case None => 100
	      }

	      logger.debug("Returning last executed test results for page: ["+page+"] and limit: ["+limit+"]")

        val testResults = PersistenceSchema.testResults
	        .filter(_.sutId === systemId)
	        .drop(page * limit)
	        .take(limit)
	        .sortBy(_.startTime.desc)
	        .list

	      testResults map { testResult =>
		      val testCase = PersistenceSchema.testCases
			      .filter(_.id === testResult.testCaseId)
		        .firstOption

		      val actor = PersistenceSchema.actors
		        .filter(_.id === testResult.actorId)
		        .firstOption

		      TestResultReport(testResult, testCase, actor)
	      }
      }
    }
  }

  def getTestResultOfSession(sessionId:String): Future[TestResult] = {
    Future {
      DB.withSession{ implicit session =>
        val testResult = PersistenceSchema.testResults.filter(_.testSessionId === sessionId).first
        val xml = testResult.tpl
        val testcase = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(xml)))
        val json = JacksonUtil.serializeTestCasePresentation(testcase)
        testResult.withPresentation(json)
      }
    }
  }

  def createTestReport(sessionId:String, systemId:Long, testCaseName:String, actorId:Long, presentation:String):Future[Unit] = {
    Future{
      DB.withSession { implicit session =>
        val initialStatus = TestResultType.UNDEFINED.value()
        val startTime     = TimeUtil.getCurrentTime()

	      val testCaseId = {
		      val testCaseOptionId = PersistenceSchema.testCases
			      .filter(_.shortname === testCaseName)
		        .map(_.id)
		        .firstOption

		      testCaseOptionId match {
			      case Some(id) => id
			      case None => -1
		      }
	      }

        PersistenceSchema.testResults.insert(TestResult(sessionId, systemId, actorId, testCaseId, initialStatus, startTime, None, None, presentation))
      }
    }
  }

  def finishTestReport(sessionId:String, status:TestResultType):Future[Unit] = {
    Future{
      DB.withSession { implicit  session =>
        val q = for {t <- PersistenceSchema.testResults if t.testSessionId === sessionId} yield ( t.result, t.endTime )
        q.update(status.value(), Some(TimeUtil.getCurrentTime()))
      }
    }
  }

	def createTestStepReport(sessionId:String, step:TestStepStatus):Future[Unit] = {
    Future{
      DB.withSession { implicit session =>
        //save status reports only when step is concluded with either COMPLETED or ERROR state
        if (step.getReport != null && (step.getStatus == StepStatus.COMPLETED || step.getStatus == StepStatus.ERROR)) {
          val path = sessionId + "/" + step.getStepId + ".xml"

          //write the report into a file
          if(step.getReport != null) {
            val file = new File(Play.current.getFile(Configurations.TEST_CASE_REPOSITORY_PATH + "/" + STATUS_UPDATES_PATH), path)
            file.getParentFile.mkdirs()
            file.createNewFile()

            val stream = new FileOutputStream(file)
            stream.write(XMLUtils.marshalToString(new ObjectFactory().createUpdateStatusRequest(step)).getBytes)
            stream.close()
          }
          //save the path of the report file to the DB
          val result = TestStepResult(sessionId, step.getStepId, step.getStatus.ordinal().toShort , path)
          PersistenceSchema.testStepReports.insert(result)
        }
      }
    }
  }

  def getTestStepResults(sessionId:String): Future[List[TestStepResult]] = {
    Future{
      DB.withSession { implicit session =>
        PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).list
      }
    }
  }
}
