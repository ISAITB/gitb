package managers

import java.io.{File, FileOutputStream, StringReader}
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.Date
import javax.xml.transform.stream.StreamSource

import com.gitb.core.StepStatus
import com.gitb.reports.ReportGenerator
import com.gitb.reports.dto.TestCaseOverview
import com.gitb.tbs.{ObjectFactory, TestStepStatus}
import com.gitb.tpl.TestCase
import com.gitb.tr._
import com.gitb.utils.XMLUtils
import config.Configurations
import models._
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._
import utils.{JacksonUtil, TimeUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

/**
  * Created by senan on 03.12.2014.
  */
object ReportManager extends BaseManager {

  val logger: Logger = LoggerFactory.getLogger("ReportManager")

  val STATUS_UPDATES_PATH: String = "status-updates"
  private val generator = new ReportGenerator()

  def getTestResults(page: Long,
                     limit: Long,
                     systemId: Long,
                     domainIds: Option[List[Long]],
                     specIds: Option[List[Long]],
                     testSuiteIds: Option[List[Long]],
                     testCaseIds: Option[List[Long]],
                     results: Option[List[String]],
                     startTimeBegin: Option[String],
                     startTimeEnd: Option[String],
                     endTimeBegin: Option[String],
                     endTimeEnd: Option[String],
                     sortColumn: Option[String],
                     sortOrder: Option[String]): Future[List[TestResult]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(None, domainIds, specIds, testSuiteIds, testCaseIds, None, None, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
        val testResults = query.filter(_.sutId === systemId).drop((page - 1) * limit).take(limit).list
        testResults
      }
    }
  }

  def getTestResultsCount(systemId: Long,
                          domainIds: Option[List[Long]],
                          specIds: Option[List[Long]],
                          testSuiteIds: Option[List[Long]],
                          testCaseIds: Option[List[Long]],
                          results: Option[List[String]],
                          startTimeBegin: Option[String],
                          startTimeEnd: Option[String],
                          endTimeBegin: Option[String],
                          endTimeEnd: Option[String]): Future[Long] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(None, domainIds, specIds, testSuiteIds, testCaseIds, None, None, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, None, None)
        query.filter(_.sutId === systemId).size.run
      }
    }
  }

  def getActiveTestResults(communityIds: Option[List[Long]],
                           domainIds: Option[List[Long]],
                           specIds: Option[List[Long]],
                           testSuiteIds: Option[List[Long]],
                           testCaseIds: Option[List[Long]],
                           organizationIds: Option[List[Long]],
                           systemIds: Option[List[Long]],
                           startTimeBegin: Option[String],
                           startTimeEnd: Option[String],
                           sortColumn: Option[String],
                           sortOrder: Option[String]): Future[List[TestResult]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, None, startTimeBegin, startTimeEnd, None, None, sortColumn, sortOrder)
        val testResults = query.filter(_.endTime.isEmpty).list
        testResults
      }
    }
  }

  def getFinishedTestResultsCount(communityIds: Option[List[Long]],
                                  domainIds: Option[List[Long]],
                                  specIds: Option[List[Long]],
                                  testSuiteIds: Option[List[Long]],
                                  testCaseIds: Option[List[Long]],
                                  organizationIds: Option[List[Long]],
                                  systemIds: Option[List[Long]],
                                  results: Option[List[String]],
                                  startTimeBegin: Option[String],
                                  startTimeEnd: Option[String],
                                  endTimeBegin: Option[String],
                                  endTimeEnd: Option[String]): Future[Long] = {
    Future {
      DB.withSession { implicit session =>
        val testResults = getTestResultQuery(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, None, None)
        testResults.filter(_.endTime.isDefined).size.run
      }
    }
  }

  def getFinishedTestResults(page: Long,
                             limit: Long,
                             communityIds: Option[List[Long]],
                             domainIds: Option[List[Long]],
                             specIds: Option[List[Long]],
                             testSuiteIds: Option[List[Long]],
                             testCaseIds: Option[List[Long]],
                             organizationIds: Option[List[Long]],
                             systemIds: Option[List[Long]],
                             results: Option[List[String]],
                             startTimeBegin: Option[String],
                             startTimeEnd: Option[String],
                             endTimeBegin: Option[String],
                             endTimeEnd: Option[String],
                             sortColumn: Option[String],
                             sortOrder: Option[String]): Future[List[TestResult]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
        val testResults = query.filter(_.endTime.isDefined).drop((page - 1) * limit).take(limit).list
        testResults
      }
    }
  }

  private def getTestResultQuery(communityIds: Option[List[Long]],
                                 domainIds: Option[List[Long]],
                                 specIds: Option[List[Long]],
                                 testSuiteIds: Option[List[Long]],
                                 testCaseIds: Option[List[Long]],
                                 organizationIds: Option[List[Long]],
                                 systemIds: Option[List[Long]],
                                 results: Option[List[String]],
                                 startTimeBegin: Option[String],
                                 startTimeEnd: Option[String],
                                 endTimeBegin: Option[String],
                                 endTimeEnd: Option[String],
                                 sortColumn: Option[String],
                                 sortOrder: Option[String])(implicit session: Session) = {

    var query = for {
      testResult <- PersistenceSchema.testResults
    } yield testResult

    query = communityIds match {
      case Some(ids) => query.filter(_.communityId inSet ids)
      case None => query
    }

    query = domainIds match {
      case Some(ids) => query.filter(_.domainId inSet ids)
      case None => query
    }

    query = specIds match {
      case Some(ids) => query.filter(_.specificationId inSet ids)
      case None => query
    }

    query = testCaseIds match {
      case Some(ids) => query.filter(_.testCaseId inSet ids)
      case None => query
    }

    query = organizationIds match {
      case Some(ids) => query.filter(_.organizationId inSet ids)
      case None => query
    }

    query = systemIds match {
      case Some(ids) => query.filter(_.sutId inSet ids)
      case None => query
    }

    query = results match {
      case Some(s) => query.filter(_.result inSet s)
      case None => query
    }

    query = testSuiteIds match {
      case Some(ids) => query.filter(_.testSuiteId inSet ids)
      case None => query
    }

    if (startTimeBegin.isDefined && startTimeEnd.isDefined) {
      val start = TimeUtil.parseTimestamp(startTimeBegin.get)
      val end = TimeUtil.parseTimestamp(startTimeEnd.get)

      query = query.filter(_.startTime >= start).filter(_.startTime <= end)
    }

    if (endTimeBegin.isDefined && endTimeEnd.isDefined) {
      val start = TimeUtil.parseTimestamp(endTimeBegin.get)
      val end = TimeUtil.parseTimestamp(endTimeEnd.get)

      query = query.filter(_.endTime >= start).filter(_.endTime <= end)
    }

    if (sortColumn.isDefined && sortOrder.isDefined) {
      if (sortOrder.get == "asc") {
        query = sortColumn.get match {
          case "specification" => query.sortBy(_.specification)
          case "session" => query.sortBy(_.testSessionId)
          case "startTime" => query.sortBy(_.startTime)
          case "endTime" => query.sortBy(_.endTime)
          case "organization" => query.sortBy(_.organization)
          case "system" => query.sortBy(_.sut)
          case "result" => query.sortBy(_.result)
          case "testCase" => query.sortBy(_.testCase)
          case "actorName" => query.sortBy(_.actor)
          case _ => query
        }
      }
      if (sortOrder.get == "desc") {
        query = sortColumn.get match {
          case "specification" => query.sortBy(_.specification)
          case "session" => query.sortBy(_.testSessionId.desc)
          case "startTime" => query.sortBy(_.startTime.desc)
          case "endTime" => query.sortBy(_.endTime.desc)
          case "organization" => query.sortBy(_.organization.desc)
          case "system" => query.sortBy(_.sut.desc)
          case "result" => query.sortBy(_.result.desc)
          case "testCase" => query.sortBy(_.testCase.desc)
          case "actorName" => query.sortBy(_.actor.desc)
          case _ => query
        }
      }
    }

    query
  }

  def getTestResultOfSession(sessionId: String): Future[TestResult] = {
    Future {
      DB.withSession {
        implicit session =>
          val testResult = PersistenceSchema.testResults.filter(_.testSessionId === sessionId).first
          val xml = testResult.tpl
          val testcase = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(xml)))
          val json = JacksonUtil.serializeTestCasePresentation(testcase)
          testResult.withPresentation(json)
      }
    }
  }

  def createTestReport(sessionId: String, systemId: Long, testId: String, actorId: Long, presentation: String): Future[Unit] = {
    Future {
      DB.withSession {
        implicit session =>
          val initialStatus = TestResultType.UNDEFINED.value()
          val startTime = TimeUtil.getCurrentTimestamp()
          val system = SystemManager.getSystemById(systemId).get
          val organisation = OrganizationManager.getById(system.owner).get
          val community = CommunityManager.getById(organisation.community).get
          val testCase = TestCaseManager.getTestCaseForId(testId).get
          val testSuite = TestSuiteManager.getTestSuiteOfTestCase(testCase.id)
          val actor = ActorManager.getById(actorId).get
          val specification = SpecificationManager.getSpecificationOfActor(actor.id)
          val domain = ConformanceManager.getById(specification.domain).get

          PersistenceSchema.testResults.insert(TestResult(
            sessionId, Some(systemId), Some(system.shortname), Some(organisation.id), Some(organisation.shortname),
            Some(community.id), Some(community.shortname), Some(testCase.id), Some(testCase.shortname), Some(testSuite.id), Some(testSuite.shortname),
            Some(actor.id), Some(actor.name), Some(specification.id), Some(specification.shortname), Some(domain.id), Some(domain.shortname),
            initialStatus, startTime, None, presentation))
      }
    }
  }

  def finishTestReport(sessionId: String, status: TestResultType): Future[Unit] = {
    Future {
      DB.withSession {
        implicit session =>
          val q = for {
            t <- PersistenceSchema.testResults if t.testSessionId === sessionId
          } yield (t.result, t.endTime)
          q.update(status.value(), Some(TimeUtil.getCurrentTimestamp()))
      }
    }
  }

  def setEndTimeNow(sessionId: String): Future[Unit] = {
    Future {
      DB.withSession {
        implicit session =>
          val q = for {t <- PersistenceSchema.testResults if t.testSessionId === sessionId} yield (t.endTime)
          q.update(Some(TimeUtil.getCurrentTimestamp()))
      }
    }
  }

  def getPathForTestSession(sessionId: String, isExpected: Boolean): Path = {
    DB.withSession {
      implicit session =>
        val testResult = PersistenceSchema.testResults.filter(_.testSessionId === sessionId).first
        val startTime = testResult.startTime.toLocalDateTime()
        val path = Paths.get(
          Configurations.TEST_CASE_REPOSITORY_PATH,
          STATUS_UPDATES_PATH,
          String.valueOf(startTime.getYear),
          String.valueOf(startTime.getMonthValue),
          String.valueOf(startTime.getDayOfMonth),
          sessionId
        );
        if (isExpected && !Files.exists(path)) {
          // For backwards compatibility. Lookup session folder directly under status-updates folder
          Paths.get(
            Configurations.TEST_CASE_REPOSITORY_PATH,
            STATUS_UPDATES_PATH,
            sessionId
          )
        } else {
          path
        }
    }
  }

  def createTestStepReport(sessionId: String, step: TestStepStatus): Future[Unit] = {
    Future {
      DB.withSession {
        implicit session =>
          //save status reports only when step is concluded with either COMPLETED or ERROR state
          if (step.getReport != null && (step.getStatus == StepStatus.COMPLETED || step.getStatus == StepStatus.ERROR)) {
            step.getReport.setId(step.getStepId)


            val path = step.getStepId + ".xml"

            //write the report into a file
            if (step.getReport != null) {
              val file = new File(getPathForTestSession(sessionId, false).toFile, path)
              file.getParentFile.mkdirs()
              file.createNewFile()

              val stream = new FileOutputStream(file)
              stream.write(XMLUtils.marshalToString(new ObjectFactory().createUpdateStatusRequest(step)).getBytes)
              stream.close()
            }
            //save the path of the report file to the DB
            val result = TestStepResult(sessionId, step.getStepId, step.getStatus.ordinal().toShort, path)
            PersistenceSchema.testStepReports.insert(result)
          }
      }
    }
  }

  def getTestStepResults(sessionId: String): Future[List[TestStepResult]] = {
    Future {
      DB.withSession {
        implicit session =>
          PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).list
      }
    }
  }

  def getListOfTestSteps(folder: File): ListBuffer[TestStepReportType] = {
    var list = ListBuffer[TestStepReportType]()

    val stepReports = folder.list()
      .filter(t => t.endsWith(".xml"))
      .map(t => pad10(t.substring(0, t.indexOf(".xml")))).sortWith(_ < _)

    for (stepReport <- stepReports) {
      var step = stepReport
      if (stepReport.startsWith("0")) {
        step = stepReport.replaceFirst("^0+(?!$)", "")
      }

      val file = new File(folder, step + ".xml")
      val bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath));
      val string = new String(bytes)

      //convert string in xml format into its object representation
      val report = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))
      list += report.getReport
    }

    list
  }

  private def pad10(string: String): String

  = {
    "0000000000".substring(string.length) + string;
  }

  def generateTestStepReport(xmlFile: Path, pdfReport: Path): Path = {
    val fis = Files.newInputStream(xmlFile)
    val fos = Files.newOutputStream(pdfReport)
    try {
      generator.writeTestStepStatusReport(fis, "Test step report", fos, true)
      fos.flush()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Unable to generate PDF report", e)
    } finally {
      if (fis != null) fis.close()
      if (fos != null) fos.close()
    }
    pdfReport
  }

  def generateDetailedTestCaseReport(list: ListBuffer[TestStepReportType], path: String, testCase: Option[models.TestCase], sessionId: String, addContext: Boolean): Path = {
    val reportPath = Paths.get(path)
    val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    DB.withSession {
      implicit session =>
        val overview = new TestCaseOverview()
        overview.setTitle("Test Case Report")
        // Result
        val testResult = PersistenceSchema.testResults.filter(_.testSessionId === sessionId).first
        overview.setReportResult(testResult.result)
        // Start time
        val start = testResult.startTime
        overview.setStartTime(sdf.format(new Date(start.getTime)));
        // End time
        if (testResult.endTime.isDefined) {
          val end = testResult.endTime.get
          overview.setEndTime(sdf.format(new Date(end.getTime)))
        }
        if (testResult.testCase.isDefined) {
          overview.setTestName(testResult.testCase.get)
        } else {
          overview.setTestName("-")
        }
        if (testResult.system.isDefined) {
          overview.setSystem(testResult.system.get)
        } else {
          overview.setSystem("-")
        }
        if (testResult.organization.isDefined) {
          overview.setOrganisation(testResult.organization.get)
        } else {
          overview.setOrganisation("-")
        }
        if (testResult.actor.isDefined) {
          overview.setTestActor(testResult.actor.get)
        } else {
          overview.setTestActor("-")
        }
        if (testResult.specification.isDefined) {
          overview.setTestSpecification(testResult.specification.get)
        } else {
          overview.setTestSpecification("-")
        }
        if (testResult.domain.isDefined) {
          overview.setTestDomain(testResult.domain.get)
        } else {
          overview.setTestDomain("-")
        }
        if (testCase.isDefined) {
          if (testCase.get.description.isDefined) {
            overview.setTestDescription(testCase.get.description.get);
          }
        } else {
          // This is a deleted test case - get data as possible from TestResult
          overview.setTestDescription("-")
        }
        for (stepReport <- list) {
          overview.getSteps().add(generator.fromTestStepReportType(stepReport, "Step " + stepReport.getId, addContext))
        }
        val fos = Files.newOutputStream(reportPath)
        try {
          generator.writeTestCaseOverviewReport(overview, fos)
          fos.flush()
        } catch {
          case e: Exception =>
            throw new IllegalStateException("Unable to generate PDF report", e)
        } finally {
          if (fos != null) fos.close()
        }
    }
    reportPath
  }

}
