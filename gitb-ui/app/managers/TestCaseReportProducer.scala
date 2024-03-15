package managers

import com.gitb.core.Metadata
import com.gitb.reports.dto.TestCaseOverview
import com.gitb.reports.{ReportGenerator, ReportSpecs}
import com.gitb.tbs.TestStepStatus
import com.gitb.tpl._
import com.gitb.tr.{TAR, TestCaseOverviewReportType, TestCaseStepReportType, TestCaseStepsType, TestResultType}
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import models.{CommunityLabels, Constants, SessionFolderInfo}
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import java.io.{File, StringReader}
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Using

@Singleton
class TestCaseReportProducer @Inject() (reportHelper: ReportHelper, testResultManager: TestResultManager, testCaseManager: TestCaseManager, communityLabelManager: CommunityLabelManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  val logger: Logger = LoggerFactory.getLogger("TestCaseReportProducer")
  private val codec = new URLCodec()
  import dbConfig.profile.api._

  def generateDetailedTestCaseReport(sessionId: String, contentType: Option[String], labelSupplier: Option[() => Map[Short, CommunityLabels]], reportSpecSupplier: Option[() => ReportSpecs] = None): (Option[Path], SessionFolderInfo) = {
    var report: Option[Path] = None
    val sessionFolderInfo = repositoryUtils.getPathForTestSession(codec.decode(sessionId), isExpected = true)
    if (logger.isDebugEnabled) {
      logger.debug("Reading test case report [{}] from the file [{}]", codec.decode(sessionId), sessionFolderInfo)
    }
    val testResult = testResultManager.getTestResultForSessionWrapper(sessionId)
    if (testResult.isDefined) {
      val reportData = contentType match {
        // The "vX" postfix is used to make sure we generate (but also subsequently cache) new versions of the step report
        case Some(Constants.MimeTypePDF) => (".v2.pdf", (list: ListBuffer[TitledTestStepReportType], exportedReportPath: File, testCase: Option[models.TestCase], session: String) => {
          generateDetailedTestCaseReportPdf(list, exportedReportPath.getAbsolutePath, testCase, session, labelSupplier.getOrElse(() => Map.empty[Short, CommunityLabels]).apply(), reportSpecSupplier.getOrElse(() => reportHelper.createReportSpecs()).apply())
        })
        case _ => (".report.xml", (list: ListBuffer[TitledTestStepReportType], exportedReportPath: File, testCase: Option[models.TestCase], session: String) => {
          generateDetailedTestCaseReportXml(list, exportedReportPath.getAbsolutePath, testCase, session)
        })
      }
      var exportedReport: File = null
      if (testResult.get._1.endTime.isEmpty) {
        // This name will be unique to ensure that a report generated for a pending session never gets cached.
        exportedReport = new File(sessionFolderInfo.path.toFile, "report_" + System.currentTimeMillis() + reportData._1)
        FileUtils.forceDeleteOnExit(exportedReport)
      } else {
        exportedReport = new File(sessionFolderInfo.path.toFile, "report" + reportData._1)
      }
      val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult.get._2)))
      if (!exportedReport.exists() && testResult.get._1.testCaseId.isDefined) {
        val testCase = testCaseManager.getTestCase(testResult.get._1.testCaseId.get.toString)
        val list = getListOfTestSteps(testcasePresentation, sessionFolderInfo.path.toFile)
        reportData._2.apply(list, exportedReport, testCase, sessionId)
      }
      if (exportedReport.exists()) {
        report = Some(exportedReport.toPath)
      }
    }
    (report, sessionFolderInfo)
  }

  private def generateDetailedTestCaseReportXml(list: ListBuffer[TitledTestStepReportType], path: String, testCase: Option[models.TestCase], sessionId: String): Path = {
    val reportPath = Paths.get(path)
    val overview = new TestCaseOverviewReportType()
    val testResult = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.head)
    overview.setResult(TestResultType.fromValue(testResult.result))
    overview.setMessage(testResult.outputMessage.orNull)
    overview.setStartTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testResult.startTime))
    if (testResult.endTime.isDefined) {
      overview.setEndTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testResult.endTime.get))
    }
    if (testCase.isDefined) {
      overview.setId(testCase.get.identifier)
      overview.setMetadata(new Metadata())
      overview.getMetadata.setName(testCase.get.fullname)
      overview.getMetadata.setDescription(testCase.get.description.orNull)
    }
    if (list.nonEmpty) {
      overview.setSteps(new TestCaseStepsType())
      list.foreach { stepReport =>
        val report = new TestCaseStepReportType()
        report.setId(stepReport.getWrapped.getId)
        report.setDescription(stepReport.getTitle)
        report.setReport(stepReport.getWrapped)
        overview.getSteps.getStep.add(report)
      }
    }
    Files.createDirectories(reportPath.getParent)
    Using(Files.newOutputStream(reportPath)) { fos =>
      ReportGenerator.getInstance().writeTestCaseOverviewXmlReport(overview, fos)
      fos.flush()
    }
    reportPath
  }

  private def generateDetailedTestCaseReportPdf(list: ListBuffer[TitledTestStepReportType], path: String, testCase: Option[models.TestCase], sessionId: String, labels: Map[Short, CommunityLabels], specs: ReportSpecs): Path = {
    val reportPath = Paths.get(path)
    val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    val overview = new TestCaseOverview()
    overview.setTitle("Test Case Report")
    // Labels
    overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
    overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
    overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
    overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
    overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))
    // Result
    val testResult = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.head)
    overview.setReportResult(testResult.result)
    overview.setOutputMessage(testResult.outputMessage.orNull)
    // Start time
    val start = testResult.startTime
    overview.setStartTime(sdf.format(new Date(start.getTime)))
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
        overview.setTestDescription(testCase.get.description.get)
      }
      if (specs.isIncludeDocumentation) {
        val documentation = testCaseManager.getTestCaseDocumentation(testCase.get.id)
        if (documentation.isDefined) {
          overview.setDocumentation(documentation.get)
        }
      }
      if (specs.isIncludeLogs) {
        val logContents = testResultManager.getTestSessionLog(sessionId, isExpected = true)
        if (logContents.isDefined && logContents.get.nonEmpty) {
          overview.setLogMessages(logContents.get.asJava)
        }
      }
      overview.setSpecReference(testCase.get.specReference.orNull)
      overview.setSpecDescription(testCase.get.specDescription.orNull)
      overview.setSpecLink(testCase.get.specLink.orNull)
    } else {
      // This is a deleted test case - get data as possible from TestResult
      overview.setTestDescription("-")
    }
    for (stepReport <- list) {
      overview.getSteps.add(ReportGenerator.getInstance().fromTestStepReportType(stepReport.getWrapped, stepReport.getTitle, specs))
    }
    if (overview.getSteps.isEmpty) {
      overview.setSteps(null)
    }
    // Needed if no reports have been received.
    Files.createDirectories(reportPath.getParent)
    val fos = Files.newOutputStream(reportPath)
    try {
      ReportGenerator.getInstance().writeTestCaseOverviewReport(overview, fos, specs)
      fos.flush()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Unable to generate PDF report", e)
    } finally {
      if (fos != null) fos.close()
    }
    reportPath
  }

  private def collectStepReports(testStep: TestStep, collectedSteps: ListBuffer[TitledTestStepReportType], folder: File): Unit = {
    val reportFile = new File(folder, testStep.getId + ".xml")
    if (reportFile.exists()) {
      val stepReport = new TitledTestStepReportType()
      val sequenceNumber = collectedSteps.size + 1
      if (StringUtils.isBlank(testStep.getDesc)) {
        stepReport.setTitle("Step " + sequenceNumber)
      } else {
        stepReport.setTitle("Step " + sequenceNumber + ": " + testStep.getDesc)
      }
      val bytes = Files.readAllBytes(Paths.get(reportFile.getAbsolutePath))
      val string = new String(bytes)
      //convert string in xml format into its object representation
      val report = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))
      stepReport.setWrapped(report.getReport)
      if (report.getReport != null && report.getReport.isInstanceOf[TAR] && report.getReport.asInstanceOf[TAR].getReports != null && report.getReport.asInstanceOf[TAR].getReports.getReports.isEmpty) {
        // If not set to null this would result in an empty element that is not schema-valid.
        report.getReport.asInstanceOf[TAR].setReports(null)
      }
      collectedSteps += stepReport
      // Process child steps as well if applicable
      testStep match {
        case step: GroupStep =>
          collectStepReportsForSequence(step, collectedSteps, folder)
        case step: DecisionStep =>
          collectStepReportsForSequence(step.getThen, collectedSteps, folder)
          if (step.getElse != null) {
            collectStepReportsForSequence(step.getElse, collectedSteps, folder)
          }
        case step: FlowStep =>
          import scala.jdk.CollectionConverters._
          for (thread <- step.getThread.asScala) {
            collectStepReportsForSequence(thread, collectedSteps, folder)
          }
        case step: LoopStep =>
          /*
           The sequence contained in the loop are the set of steps from the test definition.
           We need to check for specific iterations (1-indexed) against the recorded step reports to see what we include.
           */
          import scala.jdk.CollectionConverters._
          var index = 1
          while (Files.exists(Path.of(folder.toString, step.getId+"["+index+"].xml"))) {
            // We have an iteration.
            for (childStep <- step.getSteps.asScala) {
              /*
               Child steps in the loop definition are set with a fixed index of "1". We replace these with the iteration's
               index and then check to see if we have reports to add.
               */
              val prefixToLookFor = step.getId+"[1]"
              val prefixToUse = step.getId+"["+index+"]"
              replaceIdPrefix(childStep, prefixToLookFor, prefixToUse)
              collectStepReports(childStep, collectedSteps, folder)
              // Reset the IDs for the next iteration.
              replaceIdPrefix(childStep, prefixToUse, prefixToLookFor)
            }
            index += 1
          }
        case _ =>
      }
    }
  }

  private def replaceIdPrefix(step: TestStep, prefixToLookFor: String, prefixToUse: String): Unit = {
    step.setId(prefixToUse+StringUtils.removeStart(step.getId, prefixToLookFor))
    step match {
      case step: GroupStep =>
        replaceIdPrefixForSequence(step, prefixToLookFor, prefixToUse)
      case step: DecisionStep =>
        replaceIdPrefixForSequence(step.getThen, prefixToLookFor, prefixToUse)
        if (step.getElse != null) {
          replaceIdPrefixForSequence(step.getElse, prefixToLookFor, prefixToUse)
        }
      case step: FlowStep =>
        import scala.jdk.CollectionConverters._
        for (thread <- step.getThread.asScala) {
          replaceIdPrefixForSequence(thread, prefixToLookFor, prefixToUse)
        }
      case step: LoopStep =>
        import scala.jdk.CollectionConverters._
        for (childStep <- step.getSteps.asScala) {
          replaceIdPrefix(childStep, prefixToLookFor, prefixToUse)
        }
      case _ =>
    }
  }

  private def replaceIdPrefixForSequence(stepSequence: com.gitb.tpl.Sequence, prefixToLookFor: String, prefixToUse: String): Unit = {
    import scala.jdk.CollectionConverters._
    for (step <- stepSequence.getSteps.asScala) {
      replaceIdPrefix(step, prefixToLookFor, prefixToUse)
    }
  }

  private def collectStepReportsForSequence(testStepSequence: com.gitb.tpl.Sequence, collectedSteps: ListBuffer[TitledTestStepReportType], folder: File): Unit = {
    import scala.jdk.CollectionConverters._
    for (testStep <- testStepSequence.getSteps.asScala) {
      collectStepReports(testStep, collectedSteps, folder)
    }
  }

  def getListOfTestSteps(testPresentation: TestCase, folder: File): ListBuffer[TitledTestStepReportType] = {
    val list = ListBuffer[TitledTestStepReportType]()
    collectStepReportsForSequence(testPresentation.getSteps, list, folder)
    list
  }

}
