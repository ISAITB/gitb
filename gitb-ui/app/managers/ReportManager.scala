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
                     sortOrder: Option[String]): Future[List[TestResultReport]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(None, domainIds, specIds, testSuiteIds, testCaseIds, None, None, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
        val testResults = query.filter(_._4.id === systemId).drop((page - 1) * limit).take(limit).list

        testResults map { case (tr, tc, org, sys, spec, domain, tshtc, ts, actor, community) =>
          TestResultReport(tr, Some(tc), Some(actor))
        }
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
        query.filter(_._4.id === systemId).size.run
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
                           sortOrder: Option[String]): Future[List[TestResultSessionReport]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, None, startTimeBegin, startTimeEnd, None, None, sortColumn, sortOrder)
        val testResults = query.filter(_._1.endTime.isEmpty).list

        testResults map { case (tr, tc, org, sys, spec, domain, tshtc, ts, actor, community) =>
          TestResultSessionReport(tr, Some(tc), Some(org), Some(sys), Some(spec), Some(domain), Some(ts))
        }
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
        testResults.filter(_._1.endTime.isDefined).size.run
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
                             sortOrder: Option[String]): Future[List[TestResultSessionReport]] = {
    Future {
      DB.withSession { implicit session =>
        val query = getTestResultQuery(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
        val testResults = query.filter(_._1.endTime.isDefined).drop((page - 1) * limit).take(limit).list

        testResults map { case (tr, tc, org, sys, spec, domain, tshtc, ts, actor, community) =>
          TestResultSessionReport(tr, Some(tc), Some(org), Some(sys), Some(spec), Some(domain), Some(ts))
        }
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
      system <- PersistenceSchema.systems if system.id === testResult.sutId
      organization <- PersistenceSchema.organizations if organization.id === system.owner
      testCase <- PersistenceSchema.testCases if testCase.id === testResult.testcaseId
      specification <- PersistenceSchema.specifications if specification.id === testCase.targetSpec
      domain <- PersistenceSchema.domains if domain.id === specification.domain
      testSuiteHasTestCase <- PersistenceSchema.testSuiteHasTestCases if testCase.id === testSuiteHasTestCase.testcase
      testSuite <- PersistenceSchema.testSuites if testSuiteHasTestCase.testsuite === testSuite.id
      actor <- PersistenceSchema.actors if actor.id === testResult.actorId
      community <- PersistenceSchema.communities if organization.community === community.id
    } yield (testResult, testCase, organization, system, specification, domain, testSuiteHasTestCase, testSuite, actor, community)

    query = communityIds match {
      case Some(ids) => query.filter(_._10.id inSet ids)
      case None => query
    }

    query = domainIds match {
      case Some(ids) => query.filter(_._6.id inSet ids)
      case None => query
    }

    query = specIds match {
      case Some(ids) => query.filter(_._5.id inSet ids)
      case None => query
    }

    query = testCaseIds match {
      case Some(ids) => query.filter(_._2.id inSet ids)
      case None => query
    }

    query = organizationIds match {
      case Some(ids) => query.filter(_._3.id inSet ids)
      case None => query
    }

    query = systemIds match {
      case Some(ids) => query.filter(_._4.id inSet ids)
      case None => query
    }

    query = results match {
      case Some(s) => query.filter(_._1.result inSet s)
      case None => query
    }

    query = testSuiteIds match {
      case Some(ids) => query.filter(_._8.id inSet ids)
      case None => query
    }

    if (startTimeBegin.isDefined && startTimeEnd.isDefined) {
      val start = TimeUtil.parseTimestamp(startTimeBegin.get)
      val end = TimeUtil.parseTimestamp(startTimeEnd.get)

      query = query.filter(_._1.startTime >= start).filter(_._1.startTime <= end)
    }

    if (endTimeBegin.isDefined && endTimeEnd.isDefined) {
      val start = TimeUtil.parseTimestamp(endTimeBegin.get)
      val end = TimeUtil.parseTimestamp(endTimeEnd.get)

      query = query.filter(_._1.endTime >= start).filter(_._1.endTime <= end)
    }

    if (sortColumn.isDefined && sortOrder.isDefined) {
      if (sortOrder.get == "asc") {
        query = sortColumn.get match {
          case "session" => query.sortBy(_._1.testSessionId)
          case "startTime" => query.sortBy(_._1.startTime)
          case "endTime" => query.sortBy(_._1.endTime)
          case "organization" => query.sortBy(_._3.fullname)
          case "system" => query.sortBy(_._4.shortname)
          case "result" => query.sortBy(_._1.result)
          case "testCase" => query.sortBy(_._2.shortname)
          case "actorName" => query.sortBy(_._9.name)
          case _ => query
        }
      }
      if (sortOrder.get == "desc") {
        query = sortColumn.get match {
          case "session" => query.sortBy(_._1.testSessionId.desc)
          case "startTime" => query.sortBy(_._1.startTime.desc)
          case "endTime" => query.sortBy(_._1.endTime.desc)
          case "organization" => query.sortBy(_._3.fullname.desc)
          case "system" => query.sortBy(_._4.shortname.desc)
          case "result" => query.sortBy(_._1.result.desc)
          case "testCase" => query.sortBy(_._2.shortname.desc)
          case "actorName" => query.sortBy(_._9.name.desc)
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

  def createTestReport(sessionId: String, systemId: Long, testCaseName: String, actorId: Long, presentation: String): Future[Unit] = {
    Future {
      DB.withSession {
        implicit session =>
          val initialStatus = TestResultType.UNDEFINED.value()
          val startTime = TimeUtil.getCurrentTimestamp()

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
        if (testCase.isDefined) {
          val overview = new TestCaseOverview()
          overview.setTitle("Test Case Report")
          // Test name
          overview.setTestName(testCase.get.fullname)
          if (testCase.get.description.isDefined) {
            // Test description
            overview.setTestDescription(testCase.get.description.get);
          }
          // Result
          val testResult = PersistenceSchema.testResults.filter(_.testSessionId === sessionId).first
          overview.setReportResult(testResult.result)
          // Start time
          val start = testResult.startTime
          overview.setStartTime(sdf.format(new Date(testResult.startTime.getTime)));
          // End time
          if (testResult.endTime.isDefined) {
            val end = testResult.endTime.get
            overview.setEndTime(sdf.format(new Date(end.getTime)))
          }
          // System
          val system = PersistenceSchema.systems.filter(_.id === testResult.systemId).first
          overview.setSystem(system.fullname)
          // Organisation
          val organisation = PersistenceSchema.organizations.filter(_.id === system.owner).first
          overview.setOrganisation(organisation.fullname)
          // Actor
          val actor = PersistenceSchema.actors.filter(_.id === testResult.actorId).first
          overview.setTestActor(actor.name)
          // Specification
          val specification = PersistenceSchema.specifications.filter(_.id === testCase.get.targetSpec).first
          overview.setTestSpecification(specification.fullname)
          // Domain
          val domain = PersistenceSchema.domains.filter(_.id === specification.domain).first
          overview.setTestDomain(domain.fullname)
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
    }
    reportPath
  }

}
