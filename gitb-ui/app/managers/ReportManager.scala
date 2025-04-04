package managers

import com.gitb.core.{Metadata, SpecificationInfo, StepStatus, Tags}
import com.gitb.reports.dto._
import com.gitb.reports.{ReportGenerator, ReportSpecs, dto}
import com.gitb.tbs.TestStepStatus
import com.gitb.tpl.TestCase
import com.gitb.tr
import com.gitb.tr._
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import config.Configurations
import exceptions.{AutomationApiException, ErrorCodes, ServiceCallException}
import managers.ReportManager.{ActorInfo, XmlReportInputs}
import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType
import models.Enums.OverviewLevelType.OverviewLevelType
import models.Enums.ReportType.ReportType
import models.Enums.{ConformanceStatementItemType, OverviewLevelType, ReportType, TestResultStatus}
import models.automation.TestSessionStatus
import models.statement._
import models.{TestCaseGroup, _}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils._
import utils.signature.{CreateSignature, SigUtils}

import java.io.{File, FileOutputStream, StringReader}
import java.math.BigInteger
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.{Calendar, Date, UUID}
import javax.inject.{Inject, Singleton}
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.transform.stax.StAXSource
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.transform.{OutputKeys, TransformerConfigurationException, TransformerException}
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.jdk.FutureConverters._
import scala.util.Using

object ReportManager {

  private case class ActorInfo(testSuiteMap: Map[Long, Map[Long, (ConformanceTestSuite, Map[Long, (TestCaseGroup, Counters)])]], lastUpdateMap: Map[Long, Timestamp])

  private case class XmlReportInputs(deleteInputWhenDone: Boolean, input: Option[Path]) {

    def cleanup(): Unit = {
      if (deleteInputWhenDone && input.isDefined) {
        FileUtils.deleteQuietly(input.get.toFile)
      }
    }

  }

}

/**
  * Created by senan on 03.12.2014.
  */
@Singleton
class ReportManager @Inject() (communityManager: CommunityManager,
                               apiHelper: AutomationApiHelper,
                               organizationManager: OrganizationManager,
                               systemManager: SystemManager,
                               domainParameterManager: DomainParameterManager,
                               reportHelper: ReportHelper,
                               testCaseReportProducer: TestCaseReportProducer,
                               testSuiteManager: TestSuiteManager,
                               specificationManager: SpecificationManager,
                               conformanceManager: ConformanceManager,
                               dbConfigProvider: DatabaseConfigProvider,
                               communityLabelManager: CommunityLabelManager,
                               repositoryUtils: RepositoryUtils,
                               testResultManager: TestResultManager)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val LOGGER = LoggerFactory.getLogger(classOf[ReportManager])

  private val PLACEHOLDER_DOMAIN_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderDomain+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_GROUP_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecificationGroup+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_GROUP_OPTION_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecificationGroupOption+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecification+"\\{(\\d+)\\})")
  private val PLACEHOLDER_ACTOR_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderActor+"\\{(\\d+)\\})")
  private val PLACEHOLDER_BADGE_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadge+"(?:\\{(\\d+)(?:\\|(\\d+))?\\})?)")
  private val PLACEHOLDER_BADGE_WITHOUT_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadge+"(?:\\{(\\d+)\\})?)")
  private val PLACEHOLDER_BADGE_LIST_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadges+"\\{((?:horizontal)|(?:vertical))(?:\\|(\\d+))?\\})")
  private val PLACEHOLDER_LAST_UPDATE_DATE_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderLastUpdateDate+"\\{(.+)\\})")
  private val PLACEHOLDER_REPORT_DATE_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderReportDate+"\\{(.+)\\})")
  private val BADGE_PREVIEW_URL_REGEXP = Pattern.compile("['\"](\\S*/badgereportpreview/([A-Z]+)/(-?\\d+)/(-?\\d+)/(-?\\d+)(?:/(\\d+))?)['\"]")
  private val gitbTrObjectFactory = new com.gitb.tr.ObjectFactory
  private val gitbTplObjectFactory = new com.gitb.tpl.ObjectFactory
  private val gitbTbsObjectFactory = new com.gitb.tbs.ObjectFactory

  private def removeStepDocumentation(testCase: com.gitb.tpl.TestCase): Unit = {
    if (testCase.getSteps != null && testCase.getSteps.getSteps != null) {
      import scala.jdk.CollectionConverters._
      testCase.getSteps.getSteps.asScala.foreach { step =>
        step.setDocumentation(null)
      }
    }
  }

  def createTestReport(sessionId: String, systemId: Long, testId: String, actorId: Long, testCasePresentation: com.gitb.tpl.TestCase): Future[Unit] = {
    val initialStatus = TestResultType.UNDEFINED.value()
    val startTime = TimeUtil.getCurrentTimestamp()
    val testCaseId = testId.toLong
    // Remove the step documentation because it can greatly increase the size without any use (documentation links are not displayed for non-active test sessions)
    removeStepDocumentation(testCasePresentation)
    val presentation = XMLUtils.marshalToString(gitbTplObjectFactory.createTestcase(testCasePresentation))
    DB.run(
      (for {
        // Load required data.
        system <- PersistenceSchema.systems.filter(_.id === systemId).map(x => (x.shortname, x.owner)).result.head
        organisation <- PersistenceSchema.organizations.filter(_.id === system._2).map(x => (x.shortname, x.community)).result.head
        communityName <- PersistenceSchema.communities.filter(_.id === organisation._2).map(_.shortname).result.head
        testCaseName <- PersistenceSchema.testCases.filter(_.id === testCaseId).map(_.shortname).result.head
        testSuite <- testSuiteManager.getTestSuiteOfTestCaseInternal(testCaseId)
        actorName <- PersistenceSchema.actors.filter(_.id === actorId).map(_.name).result.head
        specification <- specificationManager.getSpecificationOfActorInternal(actorId)
        specificationGroupName <- {
          if (specification.group.isDefined) {
            PersistenceSchema.specificationGroups.filter(_.id === specification.group.get).map(_.shortname).result.headOption
          } else {
            DBIO.successful(None)
          }
        }
        domainName <- PersistenceSchema.domains.filter(_.id === specification.domain).map(_.shortname).result.head
        // Insert test result.
        _ <- {
          var specificationName = specification.shortname
          if (specificationGroupName.nonEmpty) {
            specificationName = specificationGroupName.get + " - " + specificationName
          }
          PersistenceSchema.testResults += TestResult(
            sessionId, Some(systemId), Some(system._1), Some(system._2), Some(organisation._1),
            Some(organisation._2), Some(communityName), Some(testCaseId), Some(testCaseName), Some(testSuite.id), Some(testSuite.shortname),
            Some(actorId), Some(actorName), Some(specification.id), Some(specificationName), Some(specification.domain), Some(domainName),
            initialStatus, startTime, None, None)
        }
        // Insert TPL definition.
        _ <- PersistenceSchema.testResultDefinitions += TestResultDefinition(sessionId, presentation)
        // Update also the conformance results for the system
        _ <- PersistenceSchema.conformanceResults
          .filter(_.sut === systemId)
          .filter(_.testcase === testCaseId)
          .map(c => (c.testsession, c.result, c.outputMessage, c.updateTime))
          .update(Some(sessionId), initialStatus, None, Some(startTime))
      } yield ()).transactionally
    )
  }

  def createTestStepReport(sessionId: String, step: TestStepStatus): Future[Option[String]] = {
    //save status reports only when step is concluded with either COMPLETED or ERROR state
    if (step.getReport != null && (step.getStatus == StepStatus.COMPLETED || step.getStatus == StepStatus.ERROR || step.getStatus == StepStatus.WARNING)) {
      // Check to see if we have already recorded this to avoid potential concurrency errors popping up that
      // would just lead to unique constraint errors.
      DB.run(PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).filter(_.testStepId === step.getStepId).result.headOption).flatMap { existingTestStepReport =>
        if (existingTestStepReport.isEmpty) {
          step.getReport.setId(step.getStepId)
          repositoryUtils.getPathForTestSession(sessionId, isExpected = false).flatMap { sessionFolder =>
            val sessionFolderPath = sessionFolder.path
            Files.createDirectories(sessionFolderPath)
            val testStepReportPath = step.getStepId + ".xml"
            // Write the report into a file
            if (step.getReport != null) {
              step.getReport match {
                case tar: TAR =>
                  repositoryUtils.decoupleLargeData(tar.getContext, sessionFolderPath, isTempData = false)
                case _ =>
              }
              val file = new File(sessionFolderPath.toFile, testStepReportPath)
              file.createNewFile()
              Using.resource(new FileOutputStream(file)) { stream =>
                stream.write(XMLUtils.marshalToString(gitbTbsObjectFactory.createUpdateStatusRequest(step)).getBytes)
              }
            }
            // Save the path of the report file to the DB
            val result = TestStepResult(sessionId, step.getStepId, step.getStatus.ordinal().toShort, testStepReportPath)
            DB.run((PersistenceSchema.testStepReports += result).transactionally).flatMap { _ =>
              // Flush the current log messages
              testResultManager.flushSessionLogs(sessionId, Some(sessionFolderPath.toFile)).map { _ =>
                // Return the path.
                Some(testStepReportPath)
              }
            }
          }
        } else {
          Future.successful(None)
        }
      }
    } else {
      Future.successful(None)
    }
  }

  def getTestStepResults(sessionId: String): Future[List[TestStepResult]] = {
    DB.run(PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).result.map(_.toList))
  }

  private def generateTestStepXmlReport(xmlFile: Path, xmlReport: Path): Path = {
    Using.resource(Files.newInputStream(xmlFile)) { fis =>
      Using.resource(Files.newOutputStream(xmlReport)) { fos =>
        ReportGenerator.getInstance().writeTestStepStatusXmlReport(fis, fos, false)
        fos.flush()
      }
    }
    xmlReport
  }

  private def generateTestStepPdfReport(xmlFile: Path, pdfReport: Path): Path = {
    Using.resource(Files.newInputStream(xmlFile)) { fis =>
      Using.resource(Files.newOutputStream(pdfReport)) { fos =>
        ReportGenerator.getInstance().writeTestStepStatusReport(fis, "Test step report", fos, reportHelper.createReportSpecs())
        fos.flush()
      }
    }
    pdfReport
  }

  private def resolveCommunityId(sessionId: String, userId: Option[Long]): Future[Option[Long]] = {
    DB.run(
      for {
        ids <- PersistenceSchema.testResults
          .filter(_.testSessionId === sessionId)
          .map(x => (x.communityId, x.domainId))
          .result
          .headOption
        communityIdFromSession <- {
          val communityId = ids.flatMap(_._1)
          val domainId = ids.flatMap(_._2)
          if (communityId.exists(_ != Constants.DefaultCommunityId)) {
            // Community defined that is not the default community ID
            DBIO.successful(communityId)
          } else if ((communityId.contains(Constants.DefaultCommunityId) || communityId.isEmpty) && domainId.nonEmpty) {
            // The community ID is the default community - try to lookup based on the domain.
            for {
              domainCommunityIds <- PersistenceSchema.communities
                .filter(_.domain === domainId.get)
                .map(_.id)
                .result
              domainCommunityId <- {
                if (domainCommunityIds.size == 1) {
                  DBIO.successful(Some(domainCommunityIds.head))
                } else {
                  // We can't determine a single domain from the test session.
                  DBIO.successful(None)
                }
              }
            } yield domainCommunityId
          } else {
            DBIO.successful(None)
          }
        }
        communityIdToUse <- {
          if (communityIdFromSession.nonEmpty) {
            DBIO.successful(communityIdFromSession)
          } else if (userId.nonEmpty) {
            // Last resort is to look up based on the user's community.
            communityManager.getUserCommunityIdInternal(userId.get)
          } else {
            DBIO.successful(None)
          }
        }
      } yield communityIdToUse
    )
  }

  private def getReportLabels(communityId: Long): Future[Map[Short, CommunityLabels]] = {
    communityLabelManager.getLabels(communityId)
  }

  private def createDemoTestCaseOverview(communityId: Long, source: TestCaseOverviewReportType, reportSpecs: ReportSpecs): Future[com.gitb.reports.dto.TestCaseOverview] = {
    getReportLabels(communityId).map { labels =>
      val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
      val sdfLog = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val overview = new com.gitb.reports.dto.TestCaseOverview
      overview.setTitle("Test Case Report")
      // Labels
      overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
      overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
      overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
      overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
      overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))
      // Result
      overview.setReportResult(source.getResult.value())
      overview.setOutputMessages(source.getMessage)
      // Start time
      overview.setStartTime(sdf.format(source.getStartTime.toGregorianCalendar.getTime))
      // End time
      overview.setEndTime(overview.getStartTime)
      overview.setId("1")
      overview.setTestName("Sample test case")
      overview.setSystem("Sample system")
      overview.setOrganisation("Sample organisation")
      overview.setTestActor("Sample actor")
      overview.setTestSpecification("Sample specification")
      overview.setTestDomain("Sample domain")
      overview.setTestDescription("Sample test case description")
      overview.setDocumentation("<p>Sample test case documentation</p>")
      overview.setLogMessages(util.List.of(
        "[%s] INFO Sample log info message".formatted(sdfLog.format(source.getStartTime.toGregorianCalendar.getTime)),
        "[%s] WARN Sample log warning message".formatted(sdfLog.format(source.getStartTime.toGregorianCalendar.getTime)),
        "[%s] ERROR Sample log error message".formatted(sdfLog.format(source.getStartTime.toGregorianCalendar.getTime))
      ))
      overview.setSpecReference("SPEC1")
      overview.setSpecDescription("Description for SPEC1")
      overview.setSpecLink("https://link.to.spec1")
      source.getSteps.getStep.forEach { step =>
        overview.getSteps.add(ReportGenerator.getInstance().fromTestStepReportType(step.getReport, "Sample step " + step.getId, reportSpecs))
      }
      overview
    }
  }

  private def createDemoTestCaseOverviewReport(): TestCaseOverviewReportType = {
    val report = new TestCaseOverviewReportType
    report.setResult(TestResultType.FAILURE)
    report.getMessage.add("Test session resulted in a failure.")
    report.setStartTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
    report.setEndTime(report.getStartTime)
    // Test case metadata
    report.setMetadata(new Metadata)
    report.getMetadata.setName("Sample test case")
    report.getMetadata.setDescription("Sample description")
    report.getMetadata.setSpecification(new SpecificationInfo)
    report.getMetadata.getSpecification.setReference("REF1")
    report.getMetadata.getSpecification.setLink("https://link.to.spec")
    report.getMetadata.getSpecification.setDescription("Sample specification description")
    report.getMetadata.setTags(new Tags)
    val sampleTag = new com.gitb.core.Tag
    sampleTag.setName("security")
    sampleTag.setValue("Test case linked to security requirements.")
    sampleTag.setForeground("#FFFFFF")
    sampleTag.setBackground("#000000")
    report.getMetadata.getTags.getTag.add(sampleTag)
    // Test case steps
    report.setSteps(new TestCaseStepsType)
    report.getSteps.getStep.add(createSimpleDemoSuccessStep(Some("1"), Some(report.getStartTime)))
    val step2 = new TestCaseStepReportType
    step2.setId("2")
    step2.setDescription("Second sample step")
    step2.setReport(createDemoTAR(Some(step2.getId), Some(report.getStartTime)))
    report.getSteps.getStep.add(step2)
    report
  }

  private def createDemoTAR(id: Option[String], time: Option[XMLGregorianCalendar]): TAR = {
    val report = new TAR
    report.setId(id.orNull)
    report.setDate(time.getOrElse(XMLDateTimeUtils.getXMLGregorianCalendarDateTime))
    report.setName("Validation report")
    report.setResult(TestResultType.FAILURE)
    report.setCounters(new ValidationCounters)
    report.getCounters.setNrOfErrors(BigInteger.ONE)
    report.getCounters.setNrOfWarnings(BigInteger.ONE)
    report.getCounters.setNrOfAssertions(BigInteger.ONE)
    report.setReports(new TestAssertionGroupReportsType)
    val errorContent = new BAR()
    errorContent.setDescription("Error message")
    report.getReports.getInfoOrWarningOrError.add(gitbTrObjectFactory.createTestAssertionGroupReportsTypeError(errorContent))
    val warningContent = new BAR()
    warningContent.setDescription("Warning message")
    report.getReports.getInfoOrWarningOrError.add(gitbTrObjectFactory.createTestAssertionGroupReportsTypeWarning(warningContent))
    val infoContent = new BAR()
    infoContent.setDescription("Information message")
    report.getReports.getInfoOrWarningOrError.add(gitbTrObjectFactory.createTestAssertionGroupReportsTypeInfo(infoContent))
    report
  }

  def generateDemoTestStepReport(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path]): Future[Path] = {
    for {
      _ <- {
        if (reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)) {
          // Delegate to external service. First create XML report.
          val tempXmlReport = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          // Generate the XML report and apply stylesheet if defined and needed.
          generateDemoTestStepReportInXML(tempXmlReport, transformer.filter(_ => reportSettings.customPdfsWithCustomXml)).flatMap { _ =>
            // Call service.
            callCustomPdfGenerationService(reportSettings.customPdfService.get, tempXmlReport, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(tempXmlReport.toFile)
          }
        } else {
          // Create demo data.
          val reportSpecs = ReportSpecs.build()
          val reportData = createDemoTAR(None, None)
          // Write PDF report.
          Using.resource(Files.newOutputStream(reportPath)) { output =>
            ReportGenerator.getInstance().writeTARReport(reportData, "Test step report", output, reportSpecs)
            output.flush()
          }
          Future.successful(reportPath)
        }
      }
      // Sign if needed.
      report <- signReportIfNeeded(reportSettings, reportPath)
    } yield report
  }

  def generateDemoTestStepReportInXML(reportPath: Path, transformer: Option[Path]): Future[Path] = {
    for {
      // Construct demo data
      report <- {
        val report = createDemoTAR(None, None)
        // Generate report
        Using.resource(Files.newOutputStream(reportPath)) { output =>
          ReportGenerator.getInstance().writeTestStepStatusXmlReport(report, output, false)
          output.flush()
        }
        applyXsltToReportAndPrettyPrint(reportPath, transformer)
        Future.successful(reportPath)
      }
    } yield report
  }

  def generateTestStepReport(reportPath: Path, sessionId: String, stepXmlFilePath: String, contentType: String, userId: Option[Long]): Future[Option[Path]] = {
    for {
      communityId <- resolveCommunityId(sessionId, userId)
      reportSettings <- {
        if (communityId.isDefined) {
          getReportSettings(communityId.get, ReportType.TestStepReport).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      sessionFolderInfo <- repositoryUtils.getPathForTestSessionWrapper(sessionId, isExpected = true)
      // Generate the input for the report
      reportInfo <- {
        if (contentType == Constants.MimeTypePDF && reportSettings.exists(x => x.customPdfs && x.customPdfService.isDefined)) {
          // We have a PDF report and need to delegate its generation to an external service. First generate (or retrieve) the XML report.
          val xmlInput = new File(sessionFolderInfo.path.toFile, stepXmlFilePath.toLowerCase().replace(".xml", ".report.xml")).toPath
          if (!Files.exists(xmlInput)) {
            val stepDataFile = new File(sessionFolderInfo.path.toFile, stepXmlFilePath)
            if (stepDataFile.exists()) {
              generateTestStepXmlReport(stepDataFile.toPath, xmlInput)
            } else {
              throw new IllegalStateException("Unable to retrieve report data")
            }
          }
          Future.successful(SessionReportPath(Some(xmlInput), sessionFolderInfo))
        } else {
          // Create the report.
          val reportData = contentType match {
            // The PDF report will always be unique and generated on the fly.
            case Constants.MimeTypePDF => (".report.pdf", (stepDataFile: File, report: File) => generateTestStepPdfReport(stepDataFile.toPath, report.toPath))
            // The XML report once generated will be cached as it will never change.
            case _ => (".report.xml", (stepDataFile: File, report: File) => generateTestStepXmlReport(stepDataFile.toPath, report.toPath))
          }
          val report = if (contentType == Constants.MimeTypePDF) {
            // This name will be unique to ensure that PDF reports are never cached.
            new File(sessionFolderInfo.path.toFile, UUID.randomUUID().toString + reportData._1).toPath
          } else {
            // XML reports are cached as they will never change.
            new File(sessionFolderInfo.path.toFile, stepXmlFilePath.toLowerCase().replace(".xml", reportData._1)).toPath
          }
          if (!Files.exists(report)) {
            // Generate report if not already defined.
            val stepDataFile = new File(sessionFolderInfo.path.toFile, stepXmlFilePath)
            if (stepDataFile.exists()) {
              reportData._2.apply(stepDataFile, report.toFile)
            }
          }
          Future.successful(SessionReportPath(Some(report), sessionFolderInfo))
        }
      }
      // Call custom PDF generation service (for PDFs), apply XSLT (for XML reports), sign (for PDF reports) and clean up.
      report <- finaliseTestSessionReport(reportPath, reportInfo, contentType, reportSettings, communityId, ReportType.TestStepReport)
    } yield report
  }

  private def createSimpleDemoSuccessStep(id: Option[String], date: Option[XMLGregorianCalendar]): TestCaseStepReportType = {
    val step = new TestCaseStepReportType
    step.setId(id.getOrElse("1"))
    step.setDescription("Sample step")
    val simpleReport = new SR
    simpleReport.setId(step.getId)
    simpleReport.setResult(TestResultType.SUCCESS)
    simpleReport.setDate(date.getOrElse(XMLDateTimeUtils.getXMLGregorianCalendarDateTime))
    step.setReport(simpleReport)
    step
  }

  def generateDemoTestCaseReport(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path]): Future[Path] = {
    for {
      _ <- {
        if (reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)) {
          // Delegate to external service. First create XML report.
          val tempXmlReport = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          // Generate the XML report and apply stylesheet if defined and needed.
          generateDemoTestCaseReportInXML(tempXmlReport, transformer.filter(_ => reportSettings.customPdfsWithCustomXml)).flatMap { _ =>
            // Call service.
            callCustomPdfGenerationService(reportSettings.customPdfService.get, tempXmlReport, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(tempXmlReport.toFile)
          }
        } else {
          // Create demo data.
          val reportSpecs = ReportSpecs.build()
          createDemoTestCaseOverview(reportSettings.community, createDemoTestCaseOverviewReport(), reportSpecs).map { reportData =>
            // Write PDF report.
            Using.resource(Files.newOutputStream(reportPath)) { output =>
              ReportGenerator.getInstance().writeTestCaseOverviewReport(reportData, output, reportSpecs)
              output.flush()
            }
            reportPath
          }
        }
      }
      // Sign if needed.
      report <- signReportIfNeeded(reportSettings, reportPath)
    } yield report
  }

  def generateDemoTestCaseReportInXML(reportPath: Path, transformer: Option[Path]): Future[Path] = {
    Future.successful {
      // Create demo data
      val reportData = createDemoTestCaseOverviewReport()
      // Generate report
      Using.resource(Files.newOutputStream(reportPath)) { output =>
        ReportGenerator.getInstance().writeTestCaseOverviewXmlReport(reportData, output)
        output.flush()
      }
      applyXsltToReportAndPrettyPrint(reportPath, transformer)
    }
  }

  private def generateCustomPdf(reportSettings: CommunityReportSettings, sessionReportPath: SessionReportPath, reportType: ReportType): Future[Path] = {
    if (sessionReportPath.report.isDefined) {
      for {
        xmlInputs <- {
          var xmlInputs = XmlReportInputs(deleteInputWhenDone = false, None)
          try {
            if (reportSettings.customPdfsWithCustomXml) {
              // Apply the custom stylesheet to the default XML report. This adapted XML report needs to be deleted when the PDF generation is finished.
              val reportStylesheet = repositoryUtils.getCommunityReportStylesheet(reportSettings.community, reportType)
              if (reportStylesheet.isDefined) {
                xmlInputs = XmlReportInputs(deleteInputWhenDone = true, Some(sessionReportPath.report.get.resolveSibling(UUID.randomUUID().toString + ".xml")))
                Files.copy(sessionReportPath.report.get, xmlInputs.input.get)
                applyXsltToReportAndPrettyPrint(xmlInputs.input.get, reportStylesheet)
              }
            }
            if (xmlInputs.input.isEmpty) {
              xmlInputs = XmlReportInputs(deleteInputWhenDone = false, sessionReportPath.report)
            }
            Future.successful(xmlInputs)
          } catch {
            case e: Exception =>
              xmlInputs.cleanup()
              throw e
          }
        }
        result <- {
          // Call remote service.
          val pdfPath = sessionReportPath.report.get.resolveSibling(UUID.randomUUID().toString + ".pdf")
          callCustomPdfGenerationService(reportSettings.customPdfService.get, xmlInputs.input.get, pdfPath).map { _ =>
            pdfPath
          }.andThen { _ =>
            xmlInputs.cleanup()
          }
        }
      } yield result
    } else {
      throw new IllegalStateException("Unable to retrieve report data")
    }
  }

  def generateTestCaseReport(reportPath: Path, sessionId: String, contentType: String, requestedCommunityId: Option[Long], requestedUserId: Option[Long]): Future[Option[Path]] = {
    for {
      communityId <- {
        if (requestedCommunityId.isDefined) {
          Future.successful(requestedCommunityId)
        } else {
          resolveCommunityId(sessionId, requestedUserId)
        }
      }
      reportSettings <- {
        if (communityId.isDefined) {
          getReportSettings(communityId.get, ReportType.TestCaseReport).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      // Generate the input for the report
      reportInfo <- {
        if (contentType == Constants.MimeTypePDF && reportSettings.exists(x => x.customPdfs && x.customPdfService.isDefined)) {
          testCaseReportProducer.generateDetailedTestCaseReport(sessionId, Some(Constants.MimeTypeXML), None, None)
        } else {
          // Create the report.
          val labelProvider = if (communityId.isDefined) {
            Some(() => getReportLabels(communityId.get))
          } else if (requestedUserId.isDefined) {
            Some(() => communityLabelManager.getLabelsByUserId(requestedUserId.get))
          } else {
            None
          }
          // ReportSpec provider
          val reportSpecProvider = if (communityId.isDefined) {
            Some(() => reportHelper.createReportSpecs(communityId))
          } else {
            None
          }
          testCaseReportProducer.generateDetailedTestCaseReport(sessionId, Some(contentType), labelProvider, reportSpecProvider)
        }
      }
      // Call custom PDF generation service (for PDFs), apply XSLT (for XML reports), sign (for PDF reports) and clean up.
      report <- finaliseTestSessionReport(reportPath, reportInfo, contentType, reportSettings, communityId, ReportType.TestCaseReport)
    } yield report
  }

  private def finaliseTestSessionReport(reportPath: Path, reportInfo: SessionReportPath, contentType: String, reportSettings: Option[CommunityReportSettings], communityId: Option[Long], reportType: ReportType): Future[Option[Path]] = {
    val task = if (reportInfo.report.isDefined) {
      if (contentType == Constants.MimeTypePDF) {
        for {
          pdfReport <- {
            // Call the custom PDF generation service
            if (reportSettings.exists(x => x.customPdfs && x.customPdfService.isDefined)) {
              generateCustomPdf(reportSettings.get, reportInfo, reportType)
            } else {
              Future.successful(reportInfo.report.get)
            }
          }
          pdfReport <- {
            if (communityId.isDefined && reportSettings.exists(_.signPdfs)) {
              // Sign the produced PDF report.
              communityManager.getCommunityKeystore(communityId.get, decryptKeys = true).map { keystore =>
                if (keystore.isDefined) {
                  signReport(keystore.get, pdfReport, reportPath)
                } else {
                  // PDF reports are not cached.
                  Files.move(pdfReport, reportPath)
                }
                Some(reportPath)
              }
            } else {
              // PDF reports are not cached.
              Files.move(pdfReport, reportPath)
              Future.successful(Some(reportPath))
            }
          }
        } yield pdfReport
      } else {
        // XML reports are cached (i.e. keep the original).
        Files.copy(reportInfo.report.get, reportPath)
        if (communityId.isDefined) {
          // Apply custom report stylesheet if one is defined for the relevant community.
          applyXsltToReportAndPrettyPrint(
            reportPath,
            repositoryUtils.getCommunityReportStylesheet(communityId.get, ReportType.TestCaseReport)
          )
        }
        Future.successful(Some(reportPath))
      }
    } else {
      Future.successful(None)
    }
    task.andThen { _ =>
      reportInfo.cleanup()
    }
  }

  private def getSampleConformanceStatement(addPrefixes: Boolean, testSuiteIndex: Int, testCaseIndex: Int, labels: Map[Short, CommunityLabels], domainId: Long, groupId: Long, specificationId: Long, actorId: Long): ConformanceStatementFull = {
    val domainName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain, single = true, lowercase = true) + (if (addPrefixes) " "+domainId else "")
    val groupName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup, single = true, lowercase = true) + (if (addPrefixes) " "+groupId else "")
    val specificationName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification, single = true, lowercase = true) + (if (addPrefixes) " "+specificationId else "")
    val actorName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor, single = true, lowercase = true) + (if (addPrefixes) " "+actorId else "")
    val systemName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.System, single = true, lowercase = true)
    new ConformanceStatementFull(
      0L, "Sample community",
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation, single = true, lowercase = true),
      0L, systemName, "", Some(systemName+" description"), Some("v1.0.0"),
      domainId, domainName, domainName, Some(domainName+" description"), Some(domainName+" metadata"),
      actorId, actorName, actorName, Some(actorName+" description"), Some(actorName+" metadata") ,"",
      specificationId, specificationName, specificationName, Some(specificationName+" description"), Some(specificationName+" metadata"), 0,
      Some(groupId), Some(groupName), Some(groupName+" description"), Some(groupName+" metadata"),
      Some(groupName), Option(0), specificationName, specificationName,
      Some(testSuiteIndex), Some("Sample test suite "+testSuiteIndex), Some("Description for Sample test suite "+testSuiteIndex), None, None, None, "1.0",
      Some(testCaseIndex), Some("Sample test case "+testCaseIndex), Some("Description for Sample test case "+testCaseIndex), Some(false), Some(false), None,  None, None, None, None, "1.0",
      None, None, None, None,
      "SUCCESS", Some("An output message for the test session"),
      None, Some(new Timestamp(Calendar.getInstance().getTimeInMillis)), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
  }

  def generateDemoConformanceCertificate(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path], certificateSettings: Option[ConformanceCertificateInfo], communityId: Long): Future[Path] = {
    for {
      labels <- getReportLabels(communityId)
      report <- {
        val conformanceInfo = createDemoDataForConformanceStatementReport(labels)
        generateConformanceCertificate(reportPath, reportSettings, transformer, certificateSettings, conformanceInfo, communityId, Some(labels), None, isDemo = true)
      }
    } yield report
  }

  def generateDemoConformanceOverviewCertificate(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path], certificateSettings: Option[ConformanceCertificateInfo], communityId: Long, level: OverviewLevelType): Future[Path] = {
    for {
      labels <- getReportLabels(communityId)
      conformanceData <- createDemoDataForConformanceOverviewReport(communityId, level, labels)
      report <- generateConformanceOverviewReport(conformanceData, ReportType.ConformanceOverviewCertificate, reportSettings, transformer, certificateSettings, reportPath, Some(labels), communityId, isDemo = true, None)
    } yield report
  }

  def generateConformanceCertificate(reportPath: Path, certificateSettings: Option[ConformanceCertificateInfo], actorId: Long, systemId: Long, communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    getReportSettings(communityId, ReportType.ConformanceStatementCertificate).zip(
      conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).map(_.getDetails(None))
    ).flatMap { data =>
      generateConformanceCertificate(reportPath, data._1, None, certificateSettings, data._2, communityId, None, snapshotId: Option[Long], isDemo = false)
    }
  }

  private def generateConformanceCertificate(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path], loadedCertificateSettings: Option[ConformanceCertificateInfo], conformanceInfo: List[ConformanceStatementFull], communityId: Long, labels: Option[Map[Short, CommunityLabels]], snapshotId: Option[Long], isDemo: Boolean): Future[Path] = {
    val isDelegated = reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)
    for {
      certificateSettings <- {
        if (!isDelegated) {
          if (loadedCertificateSettings.isDefined) {
            Future.successful(loadedCertificateSettings)
          } else {
            communityManager.getConformanceCertificateSettingsForExport(communityId, snapshotId).map(Some(_))
          }
        } else {
          Future.successful(None)
        }
      }
      keystoreToSignWith <- {
        if (isDelegated && reportSettings.signPdfs) {
          communityManager.getCommunityKeystore(communityId, decryptKeys = true)
        } else if (!isDelegated && certificateSettings.exists(x => x.includeSignature && x.keystore.isDefined)) {
          Future.successful(certificateSettings.get.keystore)
        } else {
          Future.successful(None)
        }
      }
      _ <- {
        if (isDelegated) {
          // We have a PDF report and need to delegate its generation to an external service. First generate (or retrieve) the XML report.
          var transformerToUse: Option[Path] = None
          if (reportSettings.customPdfsWithCustomXml) {
            transformerToUse = transformer.orElse(repositoryUtils.getCommunityReportStylesheet(communityId, ReportType.ConformanceStatementCertificate))
          }
          val xmlReportPath = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          generateDemoConformanceStatementReportInXML(xmlReportPath, transformerToUse, addTestCases = true, communityId).flatMap { _ =>
            callCustomPdfGenerationService(reportSettings.customPdfService.get, xmlReportPath, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(xmlReportPath.toFile)
          }
        } else {
          val title = if (certificateSettings.get.includeTitle) {
            if (certificateSettings.get.title.isDefined && !StringUtils.isBlank(certificateSettings.get.title.get)) {
              Some(certificateSettings.get.title.get.trim)
            } else {
              Some("Conformance Certificate")
            }
          } else {
            None
          }
          labels.map(Future.successful).getOrElse(getReportLabels(communityId)).flatMap { labelsToUse =>
            generateCoreConformanceReport(reportPath, addTestCases = false, title, addDetails = certificateSettings.get.includeDetails, addTestCaseResults = certificateSettings.get.includeItems, addTestStatus = certificateSettings.get.includeItemStatus,
              addMessage = certificateSettings.get.includeMessage, addPageNumbers = certificateSettings.get.includePageNumbers, certificateSettings.get.message,
              conformanceInfo, labelsToUse, communityId, snapshotId, isDemo
            ).map { _ =>
              reportPath
            }
          }
        }
      }
      // Sign if needed
      report <- {
        // Add signature if needed.
        if (keystoreToSignWith.isDefined) {
          signReport(keystoreToSignWith.get, reportPath)
        }
        Future.successful(reportPath)
      }
    } yield report
  }

  private def signReportIfNeeded(reportSettings: CommunityReportSettings, reportPath: Path): Future[Path] = {
    if (reportSettings.signPdfs) {
      communityManager.getCommunityKeystore(reportSettings.community, decryptKeys = true).map { communityKeystore =>
        if (communityKeystore.isDefined) {
          signReport(communityKeystore.get, reportPath)
        }
        reportPath
      }
    } else {
      Future.successful(reportPath)
    }
  }

  private def signReport(communityKeystore: CommunityKeystore, reportPath: Path): Path = {
    val signedReportPath = reportPath.resolveSibling(UUID.randomUUID().toString + ".sign.pdf")
    signReport(communityKeystore, reportPath, signedReportPath)
    Files.move(signedReportPath, reportPath, StandardCopyOption.REPLACE_EXISTING)
    reportPath
  }

  private def signReport(communityKeystore: CommunityKeystore, tempPdfPath: Path, finalPdfPath: Path): Path = {
    val keystore = SigUtils.loadKeystore(
      Base64.decodeBase64(MimeUtil.getBase64FromDataURL(communityKeystore.keystoreFile)),
      communityKeystore.keystoreType,
      communityKeystore.keystorePassword.toCharArray
    )
    val signer = new CreateSignature(keystore, communityKeystore.keyPassword.toCharArray)
    try {
      Using.resource(Files.newInputStream(tempPdfPath)) { input =>
        Using.resource(Files.newOutputStream(finalPdfPath)) { output =>
          var tsaUrl: String = null
          if (Configurations.TSA_SERVER_ENABLED) {
            tsaUrl = Configurations.TSA_SERVER_URL
          }
          signer.signDetached(input, output, tsaUrl)
          output.flush()
        }
      }
    } finally {
      FileUtils.deleteQuietly(tempPdfPath.toFile)
    }
    finalPdfPath
  }

  def generateTestCaseDocumentationPreviewReport(reportPath: Path, communityId: Long, documentation: String): Path = {
    Files.createDirectories(reportPath.getParent)
    val fos = Files.newOutputStream(reportPath)
    try {
      ReportGenerator.getInstance().writeTestCaseDocumentationPreviewReport(documentation, fos, reportHelper.createReportSpecs(Some(communityId)))
      fos.flush()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Unable to generate PDF report", e)
    } finally {
      if (fos != null) fos.close()
    }
    reportPath
  }

  private def getConformanceDataForOverviewReport(conformanceInfoBuilder: ConformanceStatusBuilder[ConformanceStatementFull], reportLevel: OverviewLevelType, communityId: Long, actorIdsToDisplay: Option[Set[Long]], snapshotId: Option[Long]): Future[ConformanceData] = {
    // The overview is a list of aggregated conformance statements.
    val conformanceOverview = conformanceInfoBuilder.getOverview(None)
    // The details is a list of the detailed conformance results (at test case level).
    val conformanceDetails = conformanceInfoBuilder.getDetails(None)
    for {
      actorIdMap <- {
        val actorIdMap = new mutable.HashMap[Long, ConformanceStatementFull]
        conformanceOverview.foreach { aggregatedStatement =>
          actorIdMap += (aggregatedStatement.actorId -> aggregatedStatement)
        }
        Future.successful(actorIdMap.toMap)
      }
      // Actor info maps
      actorInfo <- {
        // Map actor IDs to test suites.
        val actorTestSuiteMap = new mutable.LinkedHashMap[Long, mutable.LinkedHashMap[Long, (ConformanceTestSuite, mutable.LinkedHashMap[Long, (TestCaseGroup, Counters)])]]() // Actor ID to test suite map -- test suite map maps test suite ID to group map (group ID to (group, counters))
        val actorLastUpdateTime = new mutable.HashMap[Long, Timestamp]()
        conformanceDetails.foreach { statement =>
          var actorTestSuites = actorTestSuiteMap.get(statement.actorId)
          if (actorTestSuites.isEmpty) {
            actorTestSuites = Some(new mutable.LinkedHashMap[Long, (ConformanceTestSuite, mutable.LinkedHashMap[Long, (TestCaseGroup, Counters)])])
            actorTestSuiteMap += (statement.actorId -> actorTestSuites.get)
          }
          var testSuite = actorTestSuites.get.get(statement.testSuiteId.get)
          if (testSuite.isEmpty) {
            testSuite = Some(new ConformanceTestSuite(
              statement.testSuiteId.get, statement.testSuiteName.get, statement.testSuiteDescription, Some(statement.testSuiteVersion), false, statement.testSuiteSpecReference, statement.testSuiteSpecDescription, statement.testSuiteSpecLink,
              TestResultType.UNDEFINED, 0, 0, 0, 0, 0, 0, 0, 0, 0, new ListBuffer[ConformanceTestCase], new ListBuffer[models.TestCaseGroup]),
              new mutable.LinkedHashMap[Long, (TestCaseGroup, Counters)]()
            )
            actorTestSuites.get += (testSuite.get._1.id -> testSuite.get)
          }
          var testCaseGroup: Option[(TestCaseGroup, Counters)] = None
          if (statement.testCaseGroupId.isDefined) {
            testCaseGroup = testSuite.get._2.get(statement.testCaseGroupId.get)
            if (testCaseGroup.isEmpty) {
              testCaseGroup = Some(TestCaseGroup(statement.testCaseGroupId.get, statement.testCaseGroupIdentifier.get, statement.testCaseGroupName, statement.testCaseGroupDescription, testSuite.get._1.id), new Counters(0, 0, 0))
              testSuite.get._2 += (statement.testCaseGroupId.get -> testCaseGroup.get)
              testSuite.get._1.testCaseGroups.asInstanceOf[mutable.ListBuffer[models.TestCaseGroup]] += testCaseGroup.get._1
            }
          }
          val testCase = new ConformanceTestCase(
            statement.testCaseId.get, statement.testCaseName.get, statement.testCaseDescription, Some(statement.testCaseVersion), None, statement.updateTime, None, false,
            statement.testCaseOptional.get, statement.testCaseDisabled.get, TestResultType.fromValue(statement.result), statement.testCaseTags,
            statement.testCaseSpecReference, statement.testCaseSpecDescription, statement.testCaseSpecLink, statement.testCaseGroupId
          )
          testSuite.get._1.testCases.asInstanceOf[ListBuffer[ConformanceTestCase]] += testCase
          // Record result to counters
          if (!testCase.disabled) {
            if (testCase.optional) {
              if (testCase.result == TestResultType.SUCCESS) {
                testSuite.get._1.completedOptional += 1
              } else if (testCase.result == TestResultType.FAILURE) {
                testSuite.get._1.failedOptional += 1
              } else {
                testSuite.get._1.undefinedOptional += 1
              }
            } else {
              if (testCase.result == TestResultType.SUCCESS) {
                testSuite.get._1.completed += 1
                if (testCase.group.isEmpty) {
                  testSuite.get._1.completedToConsider += 1
                } else {
                  groupResult(testSuite.get._2, testCaseGroup.get._1).successes += 1
                }
              } else if (testCase.result == TestResultType.FAILURE) {
                testSuite.get._1.failed += 1
                if (testCase.group.isEmpty) {
                  testSuite.get._1.failedToConsider += 1
                } else {
                  groupResult(testSuite.get._2, testCaseGroup.get._1).failures += 1
                }
              } else {
                testSuite.get._1.undefined += 1
                if (testCase.group.isEmpty) {
                  testSuite.get._1.undefinedToConsider += 1
                } else {
                  groupResult(testSuite.get._2, testCaseGroup.get._1).other += 1
                }
              }
            }
          }
          // Calculate last update time.
          if (statement.updateTime.isDefined) {
            if (actorLastUpdateTime.contains(statement.actorId)) {
              if (actorLastUpdateTime(statement.actorId).before(statement.updateTime.get)) {
                actorLastUpdateTime += (statement.actorId -> statement.updateTime.get)
              }
            } else {
              actorLastUpdateTime += (statement.actorId -> statement.updateTime.get)
            }
          }
        }
        // Set the status of the collected test suites
        actorTestSuiteMap.values.foreach { testSuites =>
          testSuites.values.foreach { testSuite =>
            // Process groups
            if (testSuite._2.nonEmpty) {
              // We have groups
              testSuite._2.values.foreach { group =>
                if (group._2.successes > 0) {
                  testSuite._1.completedToConsider += 1
                } else if (group._2.failures > 0) {
                  testSuite._1.failedToConsider += 1
                } else if (group._2.other > 0) {
                  testSuite._1.undefinedToConsider += 1
                }
              }
            }
            // Calculate test suite result
            if (testSuite._1.failedToConsider > 0) {
              testSuite._1.result = TestResultType.FAILURE
            } else if (testSuite._1.undefinedToConsider > 0) {
              testSuite._1.result = TestResultType.UNDEFINED
            } else if (testSuite._1.completedToConsider > 0) {
              testSuite._1.result = TestResultType.SUCCESS
            } else {
              testSuite._1.result = TestResultType.UNDEFINED
            }
          }
        }
        Future.successful {
          ActorInfo(
            testSuiteMap = ListMap.from(actorTestSuiteMap.view.mapValues { innerMap1 =>
              ListMap.from(innerMap1.view.mapValues { innerMap2 =>
                (innerMap2._1, ListMap.from(innerMap2._2))
              })
            }),
            lastUpdateMap = ListMap.from(actorLastUpdateTime)
          )
        }
      }
      conformanceItemTree <- {
        conformanceManager.createConformanceItemTree(ConformanceItemTreeData(conformanceOverview, actorIdsToDisplay), withResults = true, snapshotId, testSuiteMapper = Some((statement: models.ConformanceStatement) => {
          if (actorInfo.testSuiteMap.contains(statement.actorId)) {
            actorInfo.testSuiteMap(statement.actorId).values.map(_._1).toList
          } else {
            List.empty
          }
        }))
      }
      // Check to see if only one domain can ever apply for the community (in which case it should be hidden).
      displayDomainInStatementTree <- communityManager.getCommunityDomain(communityId).map(_.isEmpty)
      // Construct the DTOs expected by the report template.
      conformanceItems <- {
        val conformanceItems = toConformanceItems(conformanceItemTree, None, new ReportData(!displayDomainInStatementTree))
        // Set also the correct IDs.
        ConformanceItem.flattenStatements(conformanceItems).forEach { statementData =>
          val idData = actorIdMap(statementData.getActorId)
          statementData.setSpecificationId(idData.specificationId)
          statementData.setSystemId(idData.systemId)
        }
        Future.successful(conformanceItems)
      }
      // Return results.
      result <- {
        Future.successful {
          ConformanceData(
            reportLevel,
            conformanceOverview.headOption.map(_.domainNameFull),
            conformanceOverview.headOption.flatMap(_.domainDescription),
            conformanceOverview.headOption.flatMap(_.domainReportMetadata),
            conformanceOverview.headOption.flatMap(_.specificationGroupNameFull),
            conformanceOverview.headOption.flatMap(_.specificationGroupDescription),
            conformanceOverview.headOption.flatMap(_.specificationGroupReportMetadata),
            conformanceOverview.headOption.map(_.specificationGroupOptionNameFull),
            conformanceOverview.headOption.flatMap(_.specificationDescription),
            conformanceOverview.headOption.flatMap(_.specificationReportMetadata),
            conformanceOverview.headOption.map(_.organizationId),
            conformanceOverview.headOption.map(_.organizationName),
            conformanceOverview.headOption.map(_.systemId),
            conformanceOverview.headOption.map(_.systemName),
            conformanceOverview.headOption.flatMap(_.systemVersion),
            conformanceOverview.headOption.flatMap(_.systemDescription),
            displayDomainInStatementTree,
            getOverallConformanceOverviewStatus(conformanceItems),
            conformanceItems,
            conformanceItemTree,
            actorInfo.lastUpdateMap,
            Calendar.getInstance().getTime
          )
        }
      }
    } yield result
  }

  private def getConformanceDataForOverviewReport(systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], snapshotId: Option[Long], communityId: Long): Future[ConformanceData] = {
    val reportLevel = if (domainId.isDefined) {
      OverviewLevelType.DomainLevel
    } else if (groupId.isDefined) {
      OverviewLevelType.SpecificationGroupLevel
    } else if (specificationId.isDefined) {
      OverviewLevelType.SpecificationLevel
    } else {
      OverviewLevelType.OrganisationLevel
    }
    for {
      // Load conformance data.
      conformanceInfoBuilder <- conformanceManager.getConformanceStatementsResultBuilder(domainId.map(List(_)), specificationId.map(List(_)), groupId.map(List(_)), None, None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false)
      data <- getConformanceDataForOverviewReport(conformanceInfoBuilder, reportLevel, communityId, None, snapshotId)
    } yield data
  }

  private def getOverallConformanceOverviewStatus(items: util.List[ConformanceItem]): String = {
    val counters = new Counters(0, 0, 0)
    items.forEach { item =>
      val status = TestResultType.fromValue(item.getOverallStatus)
      if (status == TestResultType.SUCCESS) {
        counters.successes += 1
      } else if (status == TestResultType.FAILURE) {
        counters.failures += 1
      } else {
        counters.other += 1
      }
    }
    counters.resultStatus()
  }

  def generateConformanceOverviewCertificate(reportPath: Path, certificateSettingsWithMessages: Option[ConformanceOverviewCertificateWithMessages], systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    for {
      reportSettings <- getReportSettings(communityId, ReportType.ConformanceOverviewCertificate)
      conformanceData <- getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
      certificateSettings <- {
        if (reportSettings.customPdfs) {
          Future.successful(None)
        } else if (certificateSettingsWithMessages.isDefined) {
          Future.successful(certificateSettingsWithMessages)
        } else {
          communityManager.getConformanceOverviewCertificateSettingsWrapper(communityId, defaultIfMissing = true, snapshotId, None, None)
        }
      }
      certificateInfo <- {
        if (certificateSettings.isDefined) {
          // Get the message (if needed) for the specific level
          var reportIdentifier: Option[Long] = None
          var reportLevel: Option[OverviewLevelType] = None
          if (domainId.isDefined) {
            reportIdentifier = domainId
            reportLevel = Some(OverviewLevelType.DomainLevel)
          } else if (groupId.isDefined) {
            reportIdentifier = groupId
            reportLevel = Some(OverviewLevelType.SpecificationGroupLevel)
          } else if (specificationId.isDefined) {
            reportIdentifier = specificationId
            reportLevel = Some(OverviewLevelType.SpecificationLevel)
          } else {
            reportLevel = Some(OverviewLevelType.OrganisationLevel)
          }
          val customMessage = certificateSettings.get.messageToUse(reportLevel.get, reportIdentifier)
          // Get the keystore (if needed) to use for the signature
          val keystoreTask = if (certificateSettings.get.settings.includeSignature) {
            communityManager.getCommunityKeystore(communityId, decryptKeys = true)
          } else {
            Future.successful(None)
          }
          keystoreTask.map { keystore =>
            Some(certificateSettings.get.settings.toConformanceCertificateInfo(customMessage, keystore))
          }
        } else {
          Future.successful(None)
        }
      }
      report <- generateConformanceOverviewReport(conformanceData, ReportType.ConformanceOverviewCertificate, reportSettings, None, certificateInfo, reportPath, None, communityId, isDemo = false, snapshotId)
    } yield report
  }

  def generateConformanceOverviewReport(reportPath: Path, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    for {
      reportSettings <- getReportSettings(communityId, ReportType.ConformanceOverviewReport)
      conformanceData <- getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
      report <- generateConformanceOverviewReport(conformanceData, ReportType.ConformanceOverviewReport, reportSettings, None, None, reportPath, None, communityId, isDemo = false, snapshotId)
    } yield report
  }

  private def statementItemToXmlConformanceItemOverview(item: ConformanceStatementItem, tracker: ConformanceOverviewTracker): com.gitb.tr.ConformanceItemOverview = {
    var xmlItem: Option[com.gitb.tr.ConformanceItemOverview] = None
    var hasChildWithFailures = false
    var hasChildWithIncomplete = false
    item.itemType match {
      case ConformanceStatementItemType.DOMAIN =>
        xmlItem = Some(new DomainOverview)
        if (item.items.nonEmpty) {
          item.items.get.foreach { child =>
            val childXmlItem = statementItemToXmlConformanceItemOverview(child, tracker)
            if (childXmlItem.getResult == TestResultType.FAILURE) hasChildWithFailures = true
            if (childXmlItem.getResult == TestResultType.UNDEFINED) hasChildWithIncomplete = true
            // We can have specifications or groups under a domain
            xmlItem.get.asInstanceOf[DomainOverview].getSpecificationGroupOrSpecification.add(childXmlItem)
          }
        }
      case ConformanceStatementItemType.SPECIFICATION_GROUP =>
        xmlItem = Some(new SpecificationGroupOverview)
        if (item.items.nonEmpty) {
          item.items.get.foreach { child =>
            val childXmlItem = statementItemToXmlConformanceItemOverview(child, tracker)
            if (childXmlItem.getResult == TestResultType.FAILURE) hasChildWithFailures = true
            if (childXmlItem.getResult == TestResultType.UNDEFINED) hasChildWithIncomplete = true
            // We can only have specifications under a group
            xmlItem.get.asInstanceOf[SpecificationGroupOverview].getSpecification.add(childXmlItem.asInstanceOf[SpecificationOverview])
          }
        }
      case ConformanceStatementItemType.SPECIFICATION =>
        xmlItem = Some(new SpecificationOverview)
        if (item.items.nonEmpty) {
          item.items.get.foreach { child =>
            val childXmlItem = statementItemToXmlConformanceItemOverview(child, tracker)
            if (childXmlItem.getResult == TestResultType.FAILURE) hasChildWithFailures = true
            if (childXmlItem.getResult == TestResultType.UNDEFINED) hasChildWithIncomplete = true
            // We have actors under a specification
            xmlItem.get.asInstanceOf[SpecificationOverview].getActor.add(childXmlItem.asInstanceOf[ActorOverview])
          }
        }
      case _ =>
        xmlItem = Some(new ActorOverview) // ACTOR
        if (tracker.withIndexes) {
          xmlItem.get.asInstanceOf[ActorOverview].setStatement(tracker.nextIndex().toString)
        }
    }
    if (item.items.isDefined && item.items.get.nonEmpty) {
      // We have children - calculate the result from their results
      if (hasChildWithFailures) {
        xmlItem.get.setResult(TestResultType.FAILURE)
      } else if (hasChildWithIncomplete) {
        xmlItem.get.setResult(TestResultType.UNDEFINED)
      } else {
        xmlItem.get.setResult(TestResultType.SUCCESS)
      }
    } else if (item.results.nonEmpty) {
      // We have results
      if (item.results.get.failedTestsToConsider > 0) {
        xmlItem.get.setResult(TestResultType.FAILURE)
      } else if (item.results.get.undefinedTestsToConsider > 0) {
        xmlItem.get.setResult(TestResultType.UNDEFINED)
      } else {
        xmlItem.get.setResult(TestResultType.SUCCESS)
      }
      // This is a "leaf" item where we have a conformance statement - notify the tracker to maintain the statistics
      tracker.addResult(xmlItem.get.getResult)
    } else {
      // Not normal
      xmlItem.get.setResult(TestResultType.UNDEFINED)
    }
    xmlItem.get.setName(item.name)
    xmlItem.get.setDescription(item.description.orNull)
    xmlItem.get.setMetadata(item.reportMetadata.orNull)
    xmlItem.get
  }

  private def getStatementTreeForXmlReport(conformanceData: ConformanceData, tracker: ConformanceOverviewTracker): com.gitb.tr.ConformanceStatementOverview = {
    val overview = new com.gitb.tr.ConformanceStatementOverview
    if (conformanceData.conformanceItemTree.nonEmpty) {
      conformanceData.conformanceItemTree.foreach { item =>
        val xmlItem = statementItemToXmlConformanceItemOverview(item, tracker)
        xmlItem match {
          case domainOverview: DomainOverview => overview.getDomain.add(domainOverview)
          case groupOverview: SpecificationGroupOverview => overview.setSpecificationGroup(groupOverview)
          case specificationOverview: SpecificationOverview => overview.setSpecification(specificationOverview)
          case _ => // Not normal
        }
      }
    }
    overview
  }

  private def createDemoDataForConformanceOverviewReport(communityId: Long, level: OverviewLevelType, labels: Map[Short, CommunityLabels]): Future[ConformanceData] = {
    // Generate demo data
    val builder = new ConformanceStatusBuilder[ConformanceStatementFull](true)
    var actorIdsToDisplay: Option[Set[Long]] = None
    if (level == OverviewLevelType.SpecificationLevel) {
      // Actor 1
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      // Actor 2
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false, None)
      actorIdsToDisplay = Some(Set(1L, 2L))
    } else {
      // Specification 1
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false, None)
      // Specification 2
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 4, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 5, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false, None)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 6, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false, None)
      if (level == OverviewLevelType.OrganisationLevel || level == OverviewLevelType.DomainLevel) {
        // Group 2
        // Specification 3
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 7, labels, 1L, 2L, 3L, 3L), isOptional = false, isDisabled = false, None)
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 8, labels, 1L, 2L, 3L, 3L), isOptional = false, isDisabled = false, None)
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 9, labels, 1L, 2L, 3L, 3L), isOptional = false, isDisabled = false, None)
        // Specification 4
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 7, labels, 1L, 2L, 4L, 4L), isOptional = false, isDisabled = false, None)
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 8, labels, 1L, 2L, 4L, 4L), isOptional = false, isDisabled = false, None)
        builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 9, labels, 1L, 2L, 4L, 4L), isOptional = false, isDisabled = false, None)
      }
      actorIdsToDisplay = Some(Set.empty)
    }
    // Construct the report data
    getConformanceDataForOverviewReport(builder, level, communityId, actorIdsToDisplay, None)
  }

  def generateDemoConformanceOverviewReport(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path], communityId: Long, level: OverviewLevelType): Future[Path] = {
    for {
      labels <- getReportLabels(communityId)
      conformanceData <- createDemoDataForConformanceOverviewReport(communityId, level, labels)
      report <- generateConformanceOverviewReport(conformanceData, ReportType.ConformanceOverviewReport, reportSettings, transformer, None, reportPath, Some(labels), communityId, isDemo = true, None)
    } yield report
  }

  def generateDemoConformanceOverviewReportInXML(reportPath: Path, transformer: Option[Path], communityId: Long, level: OverviewLevelType): Future[Path] = {
    for {
      labels <- getReportLabels(communityId)
      conformanceData <- createDemoDataForConformanceOverviewReport(communityId, level, labels)
      report <- generateConformanceOverviewReportInXML(reportPath, transformer, communityId, conformanceData, isDemo = true)
    } yield report
  }

  def generateConformanceOverviewReportInXML(reportPath: Path, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    for {
      conformanceData <- getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
      report <- {
        val transformer = repositoryUtils.getCommunityReportStylesheet(communityId, ReportType.ConformanceOverviewReport)
        generateConformanceOverviewReportInXML(reportPath, transformer, communityId, conformanceData, isDemo = false)
      }
    } yield report
  }

  private def generateConformanceOverviewReportInXML(reportPath: Path, transformer: Option[Path], communityId: Long, conformanceData: ConformanceData, isDemo: Boolean): Future[Path] = {
    for {
      partyDefinition <- getPartyDefinitionForXmlReport(conformanceData.organisationId.get, conformanceData.organisationName.get, conformanceData.systemId.get, conformanceData.systemName.get, conformanceData.systemVersion, conformanceData.systemDescription, communityId, isDemo)
      report <- {
        val report = new ConformanceOverviewReportType
        // Metadata
        report.setMetadata(new com.gitb.tr.ReportMetadata)
        report.getMetadata.setReportTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
        report.setOverview(new com.gitb.tr.ConformanceOverview)
        // Definition
        report.getOverview.setDefinition(new ConformanceOverviewDefinition)
        if (conformanceData.reportLevel != OverviewLevelType.OrganisationLevel) {
          if (conformanceData.domainName.isDefined) {
            report.getOverview.getDefinition.setDomain(new ConformanceItemInformation)
            report.getOverview.getDefinition.getDomain.setName(conformanceData.domainName.orNull)
            report.getOverview.getDefinition.getDomain.setDescription(conformanceData.domainDescription.orNull)
            report.getOverview.getDefinition.getDomain.setMetadata(conformanceData.domainReportMetadata.orNull)
          }
          if (conformanceData.reportLevel == OverviewLevelType.SpecificationGroupLevel) {
            if (conformanceData.groupName.isDefined) {
              report.getOverview.getDefinition.setSpecificationGroup(new ConformanceItemInformation)
              report.getOverview.getDefinition.getSpecificationGroup.setName(conformanceData.groupName.orNull)
              report.getOverview.getDefinition.getSpecificationGroup.setDescription(conformanceData.groupDescription.orNull)
              report.getOverview.getDefinition.getSpecificationGroup.setMetadata(conformanceData.groupReportMetadata.orNull)
            }
          } else if (conformanceData.reportLevel == OverviewLevelType.SpecificationLevel) {
            if (conformanceData.specificationName.isDefined) {
              if (conformanceData.groupName.isDefined) {
                report.getOverview.getDefinition.setSpecificationGroup(new ConformanceItemInformation)
                report.getOverview.getDefinition.getSpecificationGroup.setName(conformanceData.groupName.orNull)
                report.getOverview.getDefinition.getSpecificationGroup.setDescription(conformanceData.groupDescription.orNull)
                report.getOverview.getDefinition.getSpecificationGroup.setMetadata(conformanceData.groupReportMetadata.orNull)
              }
              report.getOverview.getDefinition.setSpecification(new ConformanceItemInformation)
              report.getOverview.getDefinition.getSpecification.setName(conformanceData.specificationName.orNull)
              report.getOverview.getDefinition.getSpecification.setDescription(conformanceData.specificationDescription.orNull)
              report.getOverview.getDefinition.getSpecification.setMetadata(conformanceData.specificationReportMetadata.orNull)
            }
          }
        }
        // Party information
        report.getOverview.getDefinition.setParty(partyDefinition)
        // Statement overview
        val tracker = new ConformanceOverviewTracker(true)
        report.getOverview.setStatementOverview(getStatementTreeForXmlReport(conformanceData, tracker))
        // Summary
        report.getOverview.setSummary(new ResultSummary)
        report.getOverview.getSummary.setStatus(tracker.aggregateStatus())
        report.getOverview.getSummary.setFailed(BigInteger.valueOf(tracker.failureCount))
        report.getOverview.getSummary.setIncomplete(BigInteger.valueOf(tracker.incompleteCount))
        report.getOverview.getSummary.setSucceeded(BigInteger.valueOf(tracker.successCount))
        // Statement details
        val conformanceStatements = conformanceData.getConformanceStatements()
        if (!conformanceStatements.isEmpty) {
          report.getOverview.setStatementDetails(new com.gitb.tr.ConformanceStatements)
          var statementIndex = 0
          conformanceStatements.forEach { conformanceStatement =>
            statementIndex = statementIndex + 1
            val statement = new com.gitb.tr.ConformanceStatement
            statement.setId(statementIndex.toString)
            // Definition
            statement.setDefinition(new ConformanceStatementDefinition)
            // Domain
            statement.getDefinition.setDomain(new ConformanceItemInformation)
            statement.getDefinition.getDomain.setName(conformanceStatement.getTestDomain)
            statement.getDefinition.getDomain.setDescription(conformanceStatement.getTestDomainDescription)
            statement.getDefinition.getDomain.setMetadata(conformanceStatement.getTestDomainReportMetadata)
            // Specification group
            if (conformanceStatement.getTestSpecificationGroup != null) {
              statement.getDefinition.setSpecificationGroup(new ConformanceItemInformation)
              statement.getDefinition.getSpecificationGroup.setName(conformanceStatement.getTestSpecificationGroup)
              statement.getDefinition.getSpecificationGroup.setDescription(conformanceStatement.getTestSpecificationGroupDescription)
              statement.getDefinition.getSpecificationGroup.setMetadata(conformanceStatement.getTestSpecificationGroupReportMetadata)
            }
            // Specification
            statement.getDefinition.setSpecification(new ConformanceItemInformation)
            statement.getDefinition.getSpecification.setName(conformanceStatement.getTestSpecification)
            statement.getDefinition.getSpecification.setDescription(conformanceStatement.getTestSpecificationDescription)
            statement.getDefinition.getSpecification.setMetadata(conformanceStatement.getTestSpecificationReportMetadata)
            // Actor
            statement.getDefinition.setActor(new ConformanceItemInformation)
            statement.getDefinition.getActor.setName(conformanceStatement.getTestActorInternal) // We use getTestActorInternal as it is always populated
            statement.getDefinition.getActor.setDescription(conformanceStatement.getTestActorDescription)
            statement.getDefinition.getActor.setMetadata(conformanceStatement.getTestActorReportMetadata)
            // Party information
            statement.getDefinition.setParty(partyDefinition)
            // Last update
            if (conformanceData.actorLastUpdateTime.contains(conformanceStatement.getActorId)) {
              statement.setLastUpdate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(conformanceData.actorLastUpdateTime(conformanceStatement.getActorId)))
            }
            // Summary
            statement.setSummary(new ResultSummaryWithIgnoredResults)
            statement.getSummary.setStatus(TestResultType.fromValue(conformanceStatement.getOverallStatus))
            statement.getSummary.setSucceeded(BigInteger.valueOf(conformanceStatement.getCompletedTests))
            statement.getSummary.setFailed(BigInteger.valueOf(conformanceStatement.getFailedTests))
            statement.getSummary.setIncomplete(BigInteger.valueOf(conformanceStatement.getUndefinedTests))
            statement.getSummary.setSucceededIgnored(BigInteger.valueOf(conformanceStatement.getCompletedTestsIgnored))
            statement.getSummary.setFailedIgnored(BigInteger.valueOf(conformanceStatement.getFailedTestsIgnored))
            statement.getSummary.setIncompleteIgnored(BigInteger.valueOf(conformanceStatement.getUndefinedTestsIgnored))
            // Test overview
            statement.setTestOverview(new TestSuiteOverviews)
            conformanceStatement.getTestSuites.forEach { testSuiteInfo =>
              // Statement test suites
              val testSuite = new com.gitb.tr.TestSuiteOverview
              testSuite.setMetadata(new Metadata)
              testSuite.getMetadata.setName(testSuiteInfo.getTestSuiteName)
              testSuite.getMetadata.setDescription(testSuiteInfo.getTestSuiteDescription)
              testSuite.getMetadata.setVersion(testSuiteInfo.getVersion)
              if (testSuiteInfo.getSpecReference != null || testSuiteInfo.getSpecLink != null || testSuiteInfo.getSpecDescription != null) {
                testSuite.getMetadata.setSpecification(new SpecificationInfo)
                testSuite.getMetadata.getSpecification.setReference(testSuiteInfo.getSpecReference)
                testSuite.getMetadata.getSpecification.setLink(testSuiteInfo.getSpecLink)
                testSuite.getMetadata.getSpecification.setDescription(testSuiteInfo.getSpecDescription)
              }
              testSuite.setResult(TestResultType.fromValue(testSuiteInfo.getOverallStatus))
              // Test suite test case groups
              if (testSuiteInfo.getTestCaseGroups != null && !testSuiteInfo.getTestCaseGroups.isEmpty) {
                testSuite.setTestCaseGroups(new TestCaseGroups)
                testSuiteInfo.getTestCaseGroups.forEach { group =>
                  val testCaseGroup = new tr.TestCaseGroup
                  testCaseGroup.setId(group.getId)
                  testCaseGroup.setName(group.getName)
                  testCaseGroup.setDescription(group.getDescription)
                  testSuite.getTestCaseGroups.getGroup.add(testCaseGroup)
                }
              }
              // Test suite test cases
              testSuite.setTestCases(new TestCaseOverviews)
              if (!testSuiteInfo.getTestCases.isEmpty) {
                testSuite.setTestCases(new TestCaseOverviews)
                testSuiteInfo.getTestCases.forEach { testCaseInfo =>
                  val testCase = new com.gitb.tr.TestCaseOverview()
                  testCase.setGroup(testCaseInfo.getGroup)
                  testCase.setMetadata(new Metadata)
                  testCase.getMetadata.setName(testCaseInfo.getTestName)
                  testCase.getMetadata.setDescription(testCaseInfo.getTestDescription)
                  testCase.getMetadata.setVersion(testCaseInfo.getVersion)
                  if (testCaseInfo.getSpecReference != null || testCaseInfo.getSpecLink != null || testCaseInfo.getSpecDescription != null) {
                    testCase.getMetadata.setSpecification(new SpecificationInfo)
                    testCase.getMetadata.getSpecification.setReference(testCaseInfo.getSpecReference)
                    testCase.getMetadata.getSpecification.setLink(testCaseInfo.getSpecLink)
                    testCase.getMetadata.getSpecification.setDescription(testCaseInfo.getSpecDescription)
                  }
                  if (testCaseInfo.getTags != null && !testCaseInfo.getTags.isEmpty) {
                    testCase.getMetadata.setTags(new Tags)
                    testCaseInfo.getTags.forEach { tagInfo =>
                      val tag = new com.gitb.core.Tag
                      tag.setName(tagInfo.name())
                      tag.setValue(tagInfo.description())
                      tag.setBackground(tagInfo.background())
                      tag.setForeground(tagInfo.foreground())
                      testCase.getMetadata.getTags.getTag.add(tag)
                    }
                  }
                  if (testCaseInfo.getEndTimeInternal != null) {
                    testCase.setLastUpdate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testCaseInfo.getEndTimeInternal))
                  }
                  testCase.setResult(TestResultType.fromValue(testCaseInfo.getReportResult))
                  if (testCaseInfo.isOptional) testCase.setOptional(testCaseInfo.isOptional)
                  if (testCaseInfo.isDisabled) testCase.setDisabled(testCaseInfo.isDisabled)
                  testSuite.getTestCases.getTestCase.add(testCase)
                }
              }
              statement.getTestOverview.getTestSuite.add(testSuite)
            }
            report.getOverview.getStatementDetails.getStatement.add(statement)
          }
        }
        // Produce XML report
        Files.createDirectories(reportPath.getParent)
        Using.resource(Files.newOutputStream(reportPath)) { fos =>
          ReportGenerator.getInstance().writeConformanceOverviewXmlReport(report, fos)
          fos.flush()
        }
        // Apply XSLT if needed
        applyXsltToReportAndPrettyPrint(reportPath, transformer)
        Future.successful(reportPath)
      }
    } yield report
  }

  private def generateConformanceOverviewReport(conformanceData: ConformanceData, reportType: ReportType, reportSettings: CommunityReportSettings, transformer: Option[Path], certificateSettings: Option[ConformanceCertificateInfo], reportPath: Path, loadedLabels: Option[Map[Short, CommunityLabels]], communityId: Long, isDemo: Boolean, snapshotId: Option[Long]): Future[Path] = {
    val isDelegated = reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)
    val includeCustomMessage = !isDelegated && certificateSettings.exists(x => x.includeMessage && x.message.isDefined)
    for {
      keystoreToSignWith <- {
        if (isDelegated && reportSettings.signPdfs) {
          communityManager.getCommunityKeystore(communityId, decryptKeys = true)
        } else if (!isDelegated && certificateSettings.exists(x => x.includeSignature && x.keystore.isDefined)) {
          Future.successful(certificateSettings.get.keystore)
        } else {
          Future.successful(None)
        }
      }
      customMessage <- {
        if (includeCustomMessage) {
          // Replace message placeholders
          resolveConformanceOverviewCertificateMessage(certificateSettings.get.message.get, conformanceData, communityId, snapshotId, isDemo, useUrlPlaceholders = false).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      labels <- {
        if (!isDelegated) {
          if (loadedLabels.isEmpty) {
            getReportLabels(communityId).map(Some(_))
          } else {
            Future.successful(loadedLabels)
          }
        } else {
          Future.successful(None)
        }
      }
      _ <- {
        if (isDelegated) {
          // We have a PDF report and need to delegate its generation to an external service. First generate (or retrieve) the XML report.
          var transformerToUse: Option[Path] = None
          if (reportSettings.customPdfsWithCustomXml) {
            transformerToUse = transformer.orElse(repositoryUtils.getCommunityReportStylesheet(communityId, reportType))
          }
          val xmlReportPath = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          generateDemoConformanceOverviewReportInXML(xmlReportPath, transformerToUse, communityId, conformanceData.reportLevel).flatMap { _ =>
            callCustomPdfGenerationService(reportSettings.customPdfService.get, xmlReportPath, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(xmlReportPath.toFile)
          }
        } else {
          val overview = new com.gitb.reports.dto.ConformanceOverview()
          val specs = reportHelper.createReportSpecs(Some(communityId))
          // Labels
          overview.setLabelDomain(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.Domain))
          overview.setLabelSpecificationGroup(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.SpecificationGroup))
          overview.setLabelSpecificationInGroup(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.SpecificationInGroup))
          overview.setLabelSpecification(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.Specification))
          overview.setLabelActor(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.Actor))
          overview.setLabelOrganisation(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.Organisation))
          overview.setLabelSystem(communityLabelManager.getLabel(labels.get, models.Enums.LabelType.System))
          if (conformanceData.reportLevel != OverviewLevelType.OrganisationLevel) {
            overview.setTestDomain(conformanceData.domainName.orNull)
            if (conformanceData.reportLevel == OverviewLevelType.SpecificationGroupLevel) {
              overview.setTestSpecificationGroup(conformanceData.groupName.orNull)
            } else if (conformanceData.reportLevel == OverviewLevelType.SpecificationLevel) {
              overview.setTestSpecificationGroup(conformanceData.groupName.orNull)
              overview.setTestSpecification(conformanceData.specificationName.orNull)
            }
          }
          if (certificateSettings.isDefined) {
            if (certificateSettings.get.includeTitle) {
              overview.setTitle(certificateSettings.get.title.getOrElse("Conformance Overview Certificate"))
            }
          } else {
            overview.setTitle("Conformance Overview Report")
          }
          overview.setReportDate(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(conformanceData.reportDate))
          overview.setOrganisation(conformanceData.organisationName.getOrElse("-"))
          overview.setSystem(conformanceData.systemName.getOrElse("-"))
          if (certificateSettings.isDefined) {
            overview.setIncludeMessage(includeCustomMessage)
            overview.setIncludeDetails(certificateSettings.get.includeDetails)
            overview.setIncludeConformanceItems(certificateSettings.get.includeItems)
            overview.setIncludeTestCases(certificateSettings.get.includeItemDetails)
            overview.setIncludePageNumbers(certificateSettings.get.includePageNumbers)
            overview.setIncludeTestStatus(certificateSettings.get.includeItemStatus)
            if (includeCustomMessage && customMessage.isDefined) {
              overview.setMessage(customMessage.get)
            }
          }
          overview.setConformanceItems(conformanceData.conformanceItems)
          overview.setOverallStatus(conformanceData.overallResult)
          // Create PDF.
          Files.createDirectories(reportPath.getParent)
          Using.resource(Files.newOutputStream(reportPath)) { fos =>
            ReportGenerator.getInstance().writeConformanceOverviewReport(overview, fos, specs)
            fos.flush()
          }
          Future.successful(reportPath)
        }
      }
      // Sign if needed
      report <- {
        // Add signature if needed.
        if (keystoreToSignWith.isDefined) {
          signReport(keystoreToSignWith.get, reportPath)
        }
        Future.successful(reportPath)
      }
    } yield report
  }

  def resolveConformanceOverviewCertificateMessage(rawMessage: String, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], snapshotId: Option[Long], communityId: Long): Future[String] = {
    for {
      conformanceData <- getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
      message <- resolveConformanceOverviewCertificateMessage(rawMessage, conformanceData, communityId, snapshotId, isDemo = false, useUrlPlaceholders = true)
    } yield message
  }

  private def resolveConformanceOverviewCertificateMessage(rawMessage: String, conformanceData: ConformanceData, communityId: Long, snapshotId: Option[Long], isDemo: Boolean, useUrlPlaceholders: Boolean): Future[String] = {
    val dataLocator = conformanceData.createLocator()
    for {
      message <- {
        var messageToUse = rawMessage
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_DOMAIN_WITH_INDEX_REGEXP, ConformanceStatementItemType.DOMAIN) // Before we look for domain parameters
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_GROUP_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION_GROUP)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_GROUP_OPTION_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_ACTOR_WITH_INDEX_REGEXP, ConformanceStatementItemType.ACTOR)
        Future.successful(messageToUse)
      }
      message <- replaceDomainParameters(message, communityId, snapshotId)
      message <- replaceOrganisationPropertyPlaceholders(message, isDemo, communityId, conformanceData.organisationId, snapshotId)
      message <- replaceSystemPropertyPlaceholders(message, isDemo, communityId, conformanceData.systemId, snapshotId)
      message <- replaceSimplePlaceholders(message, communityId, None, conformanceData.specificationName, conformanceData.specificationName, conformanceData.groupName, conformanceData.domainName, conformanceData.organisationName, conformanceData.systemName, snapshotId)
      message <- {
        var messageToUse = message
        messageToUse = replaceBadgeListPlaceholders(messageToUse, isDemo, conformanceData, snapshotId, useUrlPlaceholders)
        messageToUse = replaceBadgePlaceholdersByIndex(messageToUse, isDemo, conformanceData, snapshotId, useUrlPlaceholders)
        messageToUse = replaceDatePlaceholder(messageToUse, Some(conformanceData.reportDate), PLACEHOLDER_REPORT_DATE_REGEXP)
        messageToUse = replaceDatePlaceholder(messageToUse, conformanceData.getOverallLastUpdateTime(), PLACEHOLDER_LAST_UPDATE_DATE_REGEXP)
        if (!useUrlPlaceholders) {
          messageToUse = replaceBadgePreviewUrls(messageToUse, snapshotId)
        }
        Future.successful(messageToUse)
      }
    } yield message
  }

  private def replacePlaceholdersByIndex(message: String, locator: ConformanceDataLocator, pattern: Pattern, itemType: ConformanceStatementItemType): String = {
    var messageToUse = message
    val matches = pattern.matcher(message).results().collect(Collectors.toList())
    // Collect the indexes we are looking for.
    val indexes = new ListBuffer[Int]()
    matches.forEach(result => indexes += result.group(2).toInt)
    if (indexes.nonEmpty) {
      // Locate the values to use corresponding to the indexes.
      val valuesToUse = locator.itemTypeNameByIndex(indexes, itemType)
      matches.forEach(result => {
        // Replace each indexed placeholder with the relevant value (if found).
        val valueToUse = valuesToUse.get(result.group(2).toInt)
        if (valueToUse.isDefined) {
          messageToUse = messageToUse.replace(result.group(1), valueToUse.get)
        }
      })
    }
    messageToUse
  }

  private def replaceDomainParameters(message: String, communityId: Long, snapshotId: Option[Long]): Future[String] = {
    if (message.contains(Constants.PlaceholderDomain+"{")) {
      // We are referring to domain parameters.
      if (snapshotId.isEmpty) {
        domainParameterManager.getDomainParametersByCommunityId(communityId, onlySimple = true, loadValues = true).map { params =>
          var messageToUse = message
          params.foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderDomain+"{"+param.name+"}", param.value.getOrElse(""))
          }
          messageToUse
        }
      } else {
        conformanceManager.getSnapshotDomainParameters(snapshotId.get).map { params =>
          var messageToUse = message
          params.foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderDomain + "{" + param.paramKey + "}", param.paramValue)
          }
          messageToUse
        }
      }
    } else {
      Future.successful(message)
    }
  }

  private def replaceOrganisationPropertyPlaceholders(message: String, isDemo: Boolean, communityId: Long, organisationId: Option[Long], snapshotId: Option[Long]): Future[String] = {
    if (message.contains(Constants.PlaceholderOrganisation+"{")) {
      // We are referring to organisation parameters.
      if (isDemo) {
        communityManager.getOrganisationParameters(communityId, Some(true)).map { params =>
          var messageToUse = message
          params.foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.testKey+"}", param.testKey)
          }
          messageToUse
        }
      } else if (organisationId.isDefined) {
        if (snapshotId.isEmpty) {
          organizationManager.getOrganisationParameterValues(organisationId.get, Some(true)).map { params =>
            var messageToUse = message
            params.foreach { param =>
              messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.parameter.testKey+"}", param.value.map(_.value).getOrElse(""))
            }
            messageToUse
          }
        } else {
          conformanceManager.getSnapshotOrganisationProperties(snapshotId.get, organisationId.get).map { params =>
            var messageToUse = message
            params.foreach { param =>
              messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.propertyKey+"}", param.propertyValue)
            }
            messageToUse
          }
        }
      } else {
        Future(message)
      }
    } else {
      Future(message)
    }
  }

  private def replaceSystemPropertyPlaceholders(message: String, isDemo: Boolean, communityId: Long, systemId: Option[Long], snapshotId: Option[Long]): Future[String] = {
    if (message.contains(Constants.PlaceholderSystem+"{")) {
      // We are referring to system parameters.
      if (isDemo) {
        communityManager.getSystemParameters(communityId, Some(true)).map { params =>
          var messageToUse = message
          params.foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.testKey+"}", param.testKey)
          }
          messageToUse
        }
      } else if (systemId.isDefined) {
        if (snapshotId.isEmpty) {
          systemManager.getSystemParameterValues(systemId.get, Some(true)).map { params =>
            var messageToUse = message
            params.foreach { param =>
              messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.parameter.testKey+"}", param.value.map(_.value).getOrElse(""))
            }
            messageToUse
          }
        } else {
          conformanceManager.getSnapshotSystemProperties(snapshotId.get, systemId.get).map { properties =>
            var messageToUse = message
            properties.foreach { param =>
              messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.propertyKey+"}", param.propertyValue)
            }
            messageToUse
          }
        }
      } else {
        Future.successful(message)
      }
    } else {
      Future.successful(message)
    }
  }

  private def replaceSimplePlaceholders(message: String, communityId: Long, actor: Option[String], specification: Option[String],
                                        option: Option[String], group: Option[String], domain: Option[String],
                                        organisation: Option[String], system: Option[String], snapshotId: Option[Long]): Future[String] = {
    for {
      messageToUse <- {
        var messageToUse = message
        if (actor.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderActor, actor.get)
        if (domain.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderDomain, domain.get)
        if (organisation.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation, organisation.get)
        if (option.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroupOption, option.get)
        if (group.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroup, group.get)
        if (specification.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecification, specification.get)
        if (system.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSystem, system.get)
        Future.successful(messageToUse)
      }
      messageToUse <- {
        if (messageToUse.contains(Constants.PlaceholderSnapshot)) {
          conformanceManager.getPublicSnapshotLabel(communityId, snapshotId).map { snapshotLabel =>
            messageToUse.replace(Constants.PlaceholderSnapshot, snapshotLabel.getOrElse(""))
          }
        } else {
          Future.successful(messageToUse)
        }
      }
    } yield messageToUse
  }

  private def getBadgeListPlaceholders(message: String): List[BadgeListPlaceholderInfo] = {
    val placeholders = new ListBuffer[BadgeListPlaceholderInfo]
    val matches = PLACEHOLDER_BADGE_LIST_REGEXP.matcher(message).results().collect(Collectors.toList())
    matches.forEach { result =>
      placeholders += BadgeListPlaceholderInfo(result.group(1), result.group(2) == "horizontal", Option(result.group(3)).map(_.toInt))
    }
    placeholders.toList
  }

  private def getBadgePlaceholders(message: String, indexedBadges: Boolean): List[BadgePlaceholderInfo] = {
    val placeholders = new ListBuffer[BadgePlaceholderInfo]
    if (indexedBadges) {
      val matches = PLACEHOLDER_BADGE_REGEXP.matcher(message).results().collect(Collectors.toList())
      matches.forEach { result =>
        placeholders += BadgePlaceholderInfo(result.group(1), Option(result.group(2)).map(_.toShort), Option(result.group(3)).map(_.toInt))
      }
    } else {
      val matches = PLACEHOLDER_BADGE_WITHOUT_INDEX_REGEXP.matcher(message).results().collect(Collectors.toList())
      matches.forEach { result =>
        placeholders += BadgePlaceholderInfo(result.group(1), None, Option(result.group(2)).map(_.toInt))
      }
    }
    placeholders.toList
  }

  private def replaceBadgePlaceholder(message: String, placeholderInfo: BadgePlaceholderInfo, isDemo: Boolean, specificationId: Option[Long], actorId: Option[Long], snapshotId: Option[Long], status: String, useActualInDemo: Boolean, useUrlPlaceholders: Boolean, systemId: Option[Long]): String = {
    var messageToUse = message
    var imagePath: Option[String] = None
    if (isDemo) {
      var badge: Option[File] = None
      if (useActualInDemo && specificationId.isDefined && actorId.isDefined) {
        badge = repositoryUtils.getConformanceBadge(specificationId.get, actorId, snapshotId, status, exactMatch = false, forReport = true)
      }
      if (badge.isEmpty) {
        imagePath = Some("classpath:reports/images/demo-badge.png")
      } else {
        imagePath = Some(badge.get.toURI.toString)
      }
    } else if (specificationId.isDefined && actorId.isDefined) {
      val badge = repositoryUtils.getConformanceBadge(specificationId.get, actorId, snapshotId, status, exactMatch = false, forReport = true)
      if (useUrlPlaceholders) {
        // This is a placeholder for a preview by the frontend. We create this as a placeholder with all information needed to create the image URL.
        if (systemId.isDefined && specificationId.isDefined && actorId.isDefined && badge.isDefined) {
          imagePath = Some("$com.gitb.placeholder.BadgeUrl{%s|%s|%s|%s|%s}".formatted(status, systemId.get, specificationId.get, actorId.get, snapshotId.getOrElse(0L)))
        } else {
          imagePath = None
        }
      } else {
        imagePath = badge.map(_.toURI.toString)
      }
    }
    if (imagePath.isDefined) {
      if (placeholderInfo.width.isDefined) {
        // With specific width.
        messageToUse = messageToUse.replace(placeholderInfo.placeholder, "<img width=\"%s\" src=\"%s\"/>".formatted(placeholderInfo.width.get, imagePath.get))
      } else {
        // With original image width.
        messageToUse = messageToUse.replace(placeholderInfo.placeholder, "<img src=\"%s\"/>".formatted(imagePath.get))
      }
    }
    messageToUse
  }

  private def replaceBadgeListPlaceholders(message: String, isDemo: Boolean, data: ConformanceData, snapshotId: Option[Long], useUrlPlaceholders: Boolean): String = {
    var messageToUse = message
    val placeholders = getBadgeListPlaceholders(message)
    val placeholdersToProcess = new ListBuffer[(BadgePlaceholderInfo, ConformanceStatementData)]
    val statements = data.getConformanceStatements()
    if (!statements.isEmpty) {
      placeholders.foreach { placeholder =>
        // Additions: 1: before all, 2: before each, 3: after each, 4: after all
        val additions = if (placeholder.horizontal) {
          ("<tr>", "", "", "</tr>")
        } else {
          ("", "<tr>", "</tr>", "")
        }
        val placeholderTable = new StringBuilder()
        placeholderTable.append("<table style='margin-left:auto;margin-right:auto'>")
        placeholderTable.append(additions._1)
        var index = 0
        statements.forEach { statement =>
          val badge = repositoryUtils.getConformanceBadge(statement.getSpecificationId, Some(statement.getActorId), snapshotId, statement.getOverallStatus, exactMatch = false, forReport = true)
          if (badge.isDefined) {
            // When generating a badge list we will only include badges for the specifications/actors that have a badge configured.
            val placeholderText = Constants.PlaceholderBadge+"{"+index+placeholder.width.map("|"+_).getOrElse("")+"}"
            placeholderTable
              .append(additions._2)
              .append("<td class='placeholder-badge-td'>")
              .append(placeholderText)
              .append("</td>")
              .append(additions._3)
            placeholdersToProcess.addOne((BadgePlaceholderInfo(placeholderText, Some(index.toShort), placeholder.width), statement))
          }
          index += 1
        }
        placeholderTable.append(additions._4)
        placeholderTable.append("</table>")
        messageToUse = messageToUse.replace(placeholder.placeholder, placeholderTable)
      }
    }
    placeholdersToProcess.foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo._1, isDemo, Some(placeholderInfo._2.getSpecificationId), Some(placeholderInfo._2.getActorId), snapshotId, placeholderInfo._2.getOverallStatus, useActualInDemo = false, useUrlPlaceholders, data.systemId)
    }
    messageToUse
  }

  private def replaceBadgePlaceholdersByIndex(message: String, isDemo: Boolean, data: ConformanceData, snapshotId: Option[Long], useUrlPlaceholders: Boolean): String = {
    var messageToUse = message
    val statements = data.getConformanceStatements()
    val statementSize = statements.size()
    getBadgePlaceholders(message, indexedBadges = true).filter(_.index.isDefined).foreach { indexedPlaceholder =>
      // Get the statement for the given index.
      val statement = if (statementSize > indexedPlaceholder.index.get) {
        Option(statements.get(indexedPlaceholder.index.get))
      } else {
        None
      }
      if (statement.isDefined) {
        messageToUse = replaceBadgePlaceholder(messageToUse, indexedPlaceholder, isDemo, Some(statement.get.getSpecificationId), Some(statement.get.getActorId), snapshotId, statement.get.getOverallStatus, useActualInDemo = false, useUrlPlaceholders, Some(statement.get.getSystemId))
      } else if (isDemo) {
        // A conformance statement at the given index was not found. If this is a demo report then replace it with the demo badge.
        messageToUse = replaceBadgePlaceholder(messageToUse, indexedPlaceholder, isDemo, None, None, snapshotId, data.overallResult, useActualInDemo = false, useUrlPlaceholders = false, None)
      }
    }
    messageToUse
  }

  private def getDateFormatter(format: String): Option[SimpleDateFormat] = {
      try {
        Some(new SimpleDateFormat(format))
      } catch {
        case e: Exception =>
          LOGGER.warn("Invalid date format {}", format, e)
          None
      }
  }

  private def replaceDatePlaceholder(message: String, dateValue: Option[Date], placeHolderPattern: Pattern): String = {
    var messageToUse = message
    val matches = placeHolderPattern.matcher(message).results().collect(Collectors.toList())
    if (matches.size() > 0) {
      if (dateValue.isDefined) {
        matches.forEach { result =>
          val textToSet = getDateFormatter(result.group(2)).map(_.format(dateValue.get)).getOrElse("")
          messageToUse = message.replace(result.group(1), textToSet)
        }
      } else {
        matches.forEach { result =>
          messageToUse = message.replace(result.group(1), "")
        }
      }
    }
    messageToUse
  }

  private def replaceBadgePlaceholders(message: String, isDemo: Boolean, specificationId: Option[Long], actorId: Option[Long], snapshotId: Option[Long], overallStatus: String, useUrlPlaceholders: Boolean, systemId: Option[Long]): String = {
    var messageToUse = message
    // Replace badge placeholders
    val placeholders = getBadgePlaceholders(message, indexedBadges = false)
    placeholders.filter(p => p.width.isDefined).foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo, isDemo, specificationId, actorId, snapshotId, overallStatus, useActualInDemo = false, useUrlPlaceholders, systemId)
    }
    placeholders.filter(p => p.width.isEmpty).foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo, isDemo, specificationId, actorId, snapshotId, overallStatus, useActualInDemo = false, useUrlPlaceholders, systemId)
    }
    messageToUse
  }

  private def replaceBadgePreviewUrls(message: String, snapshotId: Option[Long]): String = {
    // Replace badge preview URLs. This applies if an administrator is previewing a certificate message in which case the badge placeholders have been replaced with preview URLs. Note that
    // as IDs are provided directly here we need to ensure that they are valid for the requester.
    // URL format is: /badgereportpreview/:status/:systemId/:specId/:actorId/:snapshotId
    val badgeMap = new mutable.HashMap[String, String]()
    val matches = BADGE_PREVIEW_URL_REGEXP.matcher(message).results().collect(Collectors.toList())
    matches.forEach { result =>
      if (result.groupCount() >= 5) {
        val badgeUrl = result.group(1)
        val status = result.group(2)
        val specificationId = result.group(4).toLong
        val actorId = result.group(5).toLong
        val badgePath = repositoryUtils.getConformanceBadge(specificationId, Some(actorId), snapshotId, status, exactMatch = false, forReport = true).map(_.toURI.toString)
        if (badgePath.isDefined) {
          badgeMap += (badgeUrl -> badgePath.get)
        }
      }
    }
    var messageToUse = message
    badgeMap.foreach { entry =>
      messageToUse = messageToUse.replace(entry._1, entry._2)
    }
    messageToUse
  }

  private def toConformanceItems(items: Iterable[ConformanceStatementItem], parentItem: Option[ConformanceItem], reportData: ReportData): util.List[ConformanceItem] = {
    val newItems = new util.ArrayList[ConformanceItem]
    items.foreach { item =>
      val newItem = toConformanceItem(item, reportData)
      if (item.actorToShow || parentItem.isEmpty) {
        if (item.itemType == ConformanceStatementItemType.DOMAIN && reportData.skipDomain) {
          // Skip the domain and include the children directly.
          newItems.addAll(newItem.getItems)
        } else {
          newItems.add(newItem)
        }
      } else {
        // This is a hidden actor under a parent item - add all details to the parent.
        parentItem.get.setOverallStatus(newItem.getOverallStatus)
        parentItem.get.setData(newItem.getData)
      }
    }
    newItems
  }

  private def toConformanceItem(item: ConformanceStatementItem, reportData: ReportData): ConformanceItem  = {
    val newItem = new ConformanceItem
    newItem.setName(item.name)
    newItem.setDescription(item.description.orNull)
    newItem.setReportMetadata(item.reportMetadata.orNull)
    /*
     * Record names of conformance items before we iterate children. Like this e.g. a spec will always have the name of its domain and group.
     * The only case where we will use these names is when we record the results at leaf level in which case we'll have everything available.
     */
    if (item.itemType == ConformanceStatementItemType.DOMAIN) {
      reportData.domainName = Some(newItem.getName)
      reportData.domainDescription = Option(newItem.getDescription)
      reportData.domainReportMetadata = Option(newItem.getReportMetadata)
    } else if (item.itemType == ConformanceStatementItemType.SPECIFICATION_GROUP) {
      reportData.groupName = Some(newItem.getName)
      reportData.groupDescription = Option(newItem.getDescription)
      reportData.groupReportMetadata = Option(newItem.getReportMetadata)
    } else if (item.itemType == ConformanceStatementItemType.SPECIFICATION) {
      reportData.specName = Some(newItem.getName)
      reportData.specDescription = Option(newItem.getDescription)
      reportData.specReportMetadata = Option(newItem.getReportMetadata)
    } else if (item.itemType == ConformanceStatementItemType.ACTOR) {
      reportData.actorName = Some(newItem.getName)
      reportData.actorDescription = Option(newItem.getDescription)
      reportData.actorReportMetadata = Option(newItem.getReportMetadata)
    }
    if (item.items.isDefined) {
      val children = toConformanceItems(item.items.get, Some(newItem), reportData)
      if (!children.isEmpty) {
        newItem.setItems(children)
        // The status may already be set in toConformanceItems() if all children were hidden.
        val counters = new Counters(0, 0, 0)
        children.forEach { child =>
          val childStatus = TestResultStatus.withName(child.getOverallStatus)
          if (childStatus == TestResultStatus.SUCCESS) {
            counters.successes += 1
          } else if (childStatus == TestResultStatus.FAILURE) {
            counters.failures += 1
          } else {
            counters.other += 1
          }
        }
        newItem.setOverallStatus(counters.resultStatus())
      }
    } else if (item.results.isDefined) {
      val counters = new Counters(item.results.get.completedTests, item.results.get.failedTests, item.results.get.undefinedTests)
      val countersToConsider = new Counters(item.results.get.completedTestsToConsider, item.results.get.failedTestsToConsider, item.results.get.undefinedTestsToConsider)
      val countersOptional = new Counters(item.results.get.completedOptionalTests, item.results.get.failedOptionalTests, item.results.get.undefinedOptionalTests)
      newItem.setOverallStatus(countersToConsider.resultStatus())
      newItem.setData(new ConformanceStatementData())
      newItem.getData.setOverallStatus(newItem.getOverallStatus)
      if (item.itemType == ConformanceStatementItemType.ACTOR) {
        newItem.getData.setActorId(item.id)
      }
      // Set counters
      newItem.getData.setCompletedTests(countersToConsider.successes.toInt)
      newItem.getData.setFailedTests(countersToConsider.failures.toInt)
      newItem.getData.setUndefinedTests(countersToConsider.other.toInt)
      newItem.getData.setCompletedTestsIgnored(counters.successes.toInt - countersToConsider.successes.toInt + countersOptional.successes.toInt)
      newItem.getData.setFailedTestsIgnored(counters.failures.toInt - countersToConsider.failures.toInt + countersOptional.failures.toInt)
      newItem.getData.setUndefinedTestsIgnored(counters.other.toInt - countersToConsider.other.toInt + countersOptional.other.toInt)
      // Set context
      newItem.getData.setTestDomain(reportData.domainName.orNull)
      newItem.getData.setTestDomainDescription(reportData.domainDescription.orNull)
      newItem.getData.setTestDomainReportMetadata(reportData.domainReportMetadata.orNull)
      newItem.getData.setTestSpecificationGroup(reportData.groupName.orNull)
      newItem.getData.setTestSpecificationGroupDescription(reportData.groupDescription.orNull)
      newItem.getData.setTestSpecificationGroupReportMetadata(reportData.groupReportMetadata.orNull)
      newItem.getData.setTestSpecification(reportData.specName.orNull)
      newItem.getData.setTestSpecificationDescription(reportData.specDescription.orNull)
      newItem.getData.setTestSpecificationReportMetadata(reportData.specReportMetadata.orNull)
      if (item.itemType == ConformanceStatementItemType.ACTOR && item.actorToShow) {
        newItem.getData.setTestActor(reportData.actorName.orNull)
      }
      newItem.getData.setTestActorInternal(reportData.actorName.orNull)
      newItem.getData.setTestActorDescription(reportData.actorDescription.orNull)
      newItem.getData.setTestActorReportMetadata(reportData.actorReportMetadata.orNull)
      if (item.results.get.testSuites.isDefined) {
        newItem.getData.setTestSuites(new util.ArrayList[com.gitb.reports.dto.TestSuiteOverview]())
        item.results.get.testSuites.get.foreach { testSuite =>
          val testSuiteOverview = new com.gitb.reports.dto.TestSuiteOverview()
          newItem.getData.getTestSuites.add(testSuiteOverview)
          testSuiteOverview.setOverallStatus(testSuite.result.value())
          testSuiteOverview.setTestSuiteName(testSuite.name)
          testSuiteOverview.setTestSuiteDescription(testSuite.description.orNull)
          testSuiteOverview.setVersion(testSuite.version.orNull)
          testSuiteOverview.setSpecReference(testSuite.specReference.orNull)
          testSuiteOverview.setSpecLink(testSuite.specLink.orNull)
          testSuiteOverview.setSpecDescription(testSuite.specDescription.orNull)
          val groupMap = new mutable.HashMap[Long, String] // Group ID to group identifier
          if (testSuite.testCaseGroups.nonEmpty) {
            testSuiteOverview.setTestCaseGroups(new util.ArrayList[com.gitb.reports.dto.TestCaseGroup]())
            testSuite.testCaseGroups.foreach { group =>
              val testCaseGroup = new com.gitb.reports.dto.TestCaseGroup()
              testSuiteOverview.getTestCaseGroups.add(testCaseGroup)
              testCaseGroup.setId(group.identifier)
              testCaseGroup.setName(group.name.orNull)
              testCaseGroup.setDescription(group.description.orNull)
              groupMap += (group.id -> group.identifier)
            }
          }
          testSuiteOverview.setTestCases(new util.ArrayList[com.gitb.reports.dto.TestCaseOverview]())
          testSuite.testCases.foreach { testCase =>
            val testCaseOverview = new com.gitb.reports.dto.TestCaseOverview()
            testSuiteOverview.getTestCases.add(testCaseOverview)
            testCaseOverview.setGroup(testCase.group.flatMap(groupMap.get).orNull)
            testCaseOverview.setTestName(testCase.name)
            testCaseOverview.setReportResult(testCase.result.value())
            testCaseOverview.setEndTimeInternal(testCase.updateTime.orNull)
            testCaseOverview.setTestName(testCase.name)
            testCaseOverview.setTestDescription(testCase.description.orNull)
            testCaseOverview.setVersion(testCase.version.orNull)
            testCaseOverview.setSpecReference(testCase.specReference.orNull)
            testCaseOverview.setSpecLink(testCase.specLink.orNull)
            testCaseOverview.setSpecDescription(testCase.specDescription.orNull)
            testCaseOverview.setOptional(testCase.optional)
            testCaseOverview.setDisabled(testCase.disabled)
            if (testCase.tags.isDefined) {
              testCaseOverview.setTags(parseTestCaseTags(testCase.tags.get))
            }
          }
        }
      }
    }
    newItem
  }

  private def parseTestCaseTags(tags: String): util.List[com.gitb.reports.dto.TestCaseOverview.Tag] = {
    val parsedTags = JsonUtil.parseJsTags(tags).map(x => new com.gitb.reports.dto.TestCaseOverview.Tag(x.name, x.description.orNull, x.foreground.getOrElse("#777777"), x.background.getOrElse("#FFFFFF")))
    new util.ArrayList(parsedTags.asJavaCollection)
  }

  private def createDemoDataForConformanceStatementReport(labels: Map[Short, CommunityLabels]): List[ConformanceStatementFull] = {
    val conformanceInfo = new ListBuffer[ConformanceStatementFull]
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 1, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 2, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 3, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 4, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 5, labels, 0L, 0L, 0L, 0L)
    conformanceInfo.toList
  }

  def generateDemoConformanceStatementReportInXML(reportPath: Path, transformer: Option[Path], addTestCases: Boolean, communityId: Long): Future[Path] = {
    for {
      labels <- getReportLabels(communityId)
      report <- {
        val conformanceInfo = createDemoDataForConformanceStatementReport(labels)
        generateConformanceStatementReportInXML(reportPath, transformer, addTestCases, conformanceInfo, isDemo = true)
      }
    } yield report
  }

  def generateConformanceStatementReportViaApi(reportPath: Path, organisationKey: String, systemKey: String, actorKey: String, snapshotKey: Option[String], contentType: String): Future[Path] = {
    DB.run(
      for {
        // Load statement IDs.
        statementIds <- apiHelper.getStatementIdsForApiKeys(organisationKey, Some(systemKey), Some(actorKey), snapshotKey, None, None)
        // Check that snapshot key was correct.
        _ <- if (snapshotKey.isDefined && statementIds.snapshotId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SNAPSHOT_DOES_NOT_EXIST, "Unable to find conformance snapshot based on provided API key")
        } else {
          DBIO.successful(())
        }
        // Check to see if statement already exists
        statementExists <- if (statementIds.snapshotId.isEmpty) {
          PersistenceSchema.systemImplementsActors
          .filter(_.systemId === statementIds.systemId)
          .filter(_.actorId === statementIds.actorId)
          .exists
          .result
        } else {
          PersistenceSchema.conformanceSnapshotResults
            .join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
            .filter(_._1.systemId === statementIds.systemId)
            .filter(_._1.actorId === statementIds.actorId)
            .filter(_._1.organisationId === statementIds.organisationId)
            .filter(_._2.community === statementIds.communityId)
            .filter(_._2.id === statementIds.snapshotId.get)
            .exists
            .result
        }
        _ <- {
          if (statementExists) {
            DBIO.successful(())
          } else {
            throw AutomationApiException(ErrorCodes.API_STATEMENT_DOES_NOT_EXIST, "Unable to find conformance statement based on provided API keys")
          }
        }
      } yield statementIds
    ).flatMap { idsForReport =>
      if (contentType == Constants.MimeTypePDF) {
        getReportLabels(idsForReport.communityId).flatMap { labels =>
          generateConformanceStatementReport(reportPath, addTestCases = true, idsForReport.actorId, idsForReport.systemId, labels, idsForReport.communityId, idsForReport.snapshotId)
        }
      } else {
        generateConformanceStatementReportInXML(reportPath, addTestCases = true, idsForReport.actorId, idsForReport.systemId, idsForReport.communityId, idsForReport.snapshotId)
      }
    }
  }

  def generateConformanceStatementReportInXML(reportPath: Path, addTestCases: Boolean, actorId: Long, systemId: Long, communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    for {
      conformanceInfo <- conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).map(_.getDetails(None))
      report <- {
        val transformer = repositoryUtils.getCommunityReportStylesheet(communityId, ReportType.ConformanceStatementReport)
        generateConformanceStatementReportInXML(reportPath, transformer, addTestCases, conformanceInfo, isDemo = false)
      }
    } yield report
  }

  private def getPartyDefinitionForXmlReport(organisationId: Long, organisationName: String, systemId: Long, systemName: String, systemVersion: Option[String], systemDescription: Option[String], communityId: Long, isDemo: Boolean): Future[PartyDefinition] = {
    for {
      organisationProperties <- {
        if (isDemo) {
          communityManager.getSimpleOrganisationParameters(communityId, Some(true)).map { params =>
            params.map { param =>
              val property = new PartyProperty
              property.setName(param.testKey)
              property.setValue("Value for "+param.testKey)
              property
            }
          }
        } else {
          organizationManager.getOrganisationParameterValues(organisationId, Some(true), Some(true)).map { params =>
            params.filter(_.value.isDefined).map { param =>
              val property = new PartyProperty
              property.setName(param.parameter.testKey)
              property.setValue(param.value.get.value)
              property
            }
          }
        }
      }
      systemProperties <- {
        if (isDemo) {
          communityManager.getSimpleSystemParameters(communityId, Some(true)).map { params =>
            params.map { param =>
              val property = new PartyProperty
              property.setName(param.testKey)
              property.setValue("Value for "+param.testKey)
              property
            }
          }
        } else {
          systemManager.getSystemParameterValues(systemId, Some(true), Some(true)).map { params =>
            params.filter(_.value.isDefined).map { param =>
              val property = new PartyProperty
              property.setName(param.parameter.testKey)
              property.setValue(param.value.get.value)
              property
            }
          }
        }
      }
      party <- {
        val party = new PartyDefinition
        party.setOrganisation(new com.gitb.tr.Organisation)
        party.getOrganisation.setName(organisationName)
        if (organisationProperties.nonEmpty) {
          party.getOrganisation.setProperties(new PartyProperties)
          organisationProperties.foreach { property =>
            party.getOrganisation.getProperties.getProperty.add(property)
          }
        }
        party.setSystem(new com.gitb.tr.System)
        party.getSystem.setName(systemName)
        party.getSystem.setVersion(systemVersion.orNull)
        party.getSystem.setDescription(systemDescription.orNull)
        if (systemProperties.nonEmpty) {
          party.getSystem.setProperties(new PartyProperties)
          systemProperties.foreach { property =>
            party.getSystem.getProperties.getProperty.add(property)
          }
        }
        Future.successful(party)
      }
    } yield party
  }

  private def generateConformanceStatementReportInXML(reportPath: Path, transformer: Option[Path], addTestCases: Boolean, conformanceInfo: List[ConformanceStatementFull], isDemo: Boolean): Future[Path] = {
    // Load the conformance data
    val conformanceData = conformanceInfo.head
    for {
      partyDefinition <- getPartyDefinitionForXmlReport(conformanceData.organizationId, conformanceData.organizationName, conformanceData.systemId, conformanceData.systemName, conformanceData.systemVersion, conformanceData.systemDescription, conformanceData.communityId, isDemo)
      testResultMap <- {
        if (isDemo) {
          Future.successful(None)
        } else {
          testResultManager.getTestResultsForSessions(conformanceInfo.filter(_.sessionId.isDefined).map(_.sessionId.get)).map(Some(_))
        }
      }
      report <- {
        // Build the report
        val report = new ConformanceStatementReportType
        // Metadata
        report.setMetadata(new com.gitb.tr.ReportMetadata)
        report.getMetadata.setReportTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
        report.setStatement(new com.gitb.tr.ConformanceStatement)
        // Definition
        report.getStatement.setDefinition(new ConformanceStatementDefinition)
        report.getStatement.getDefinition.setDomain(new ConformanceItemInformation)
        report.getStatement.getDefinition.getDomain.setName(conformanceData.domainNameFull)
        report.getStatement.getDefinition.getDomain.setDescription(conformanceData.domainDescription.orNull)
        report.getStatement.getDefinition.getDomain.setMetadata(conformanceData.domainReportMetadata.orNull)
        if (conformanceData.specificationGroupNameFull.isDefined) {
          report.getStatement.getDefinition.setSpecificationGroup(new ConformanceItemInformation)
          report.getStatement.getDefinition.getSpecificationGroup.setName(conformanceData.specificationGroupNameFull.orNull)
          report.getStatement.getDefinition.getSpecificationGroup.setDescription(conformanceData.specificationGroupDescription.orNull)
          report.getStatement.getDefinition.getSpecificationGroup.setMetadata(conformanceData.specificationGroupReportMetadata.orNull)
        }
        report.getStatement.getDefinition.setSpecification(new ConformanceItemInformation)
        report.getStatement.getDefinition.getSpecification.setName(conformanceData.specificationNameFull)
        report.getStatement.getDefinition.getSpecification.setDescription(conformanceData.specificationDescription.orNull)
        report.getStatement.getDefinition.getSpecification.setMetadata(conformanceData.specificationReportMetadata.orNull)
        report.getStatement.getDefinition.setActor(new ConformanceItemInformation)
        report.getStatement.getDefinition.getActor.setName(conformanceData.actorFull)
        report.getStatement.getDefinition.getActor.setDescription(conformanceData.actorDescription.orNull)
        report.getStatement.getDefinition.getActor.setMetadata(conformanceData.actorReportMetadata.orNull)
        // Party information
        report.getStatement.getDefinition.setParty(partyDefinition)
        // Test overview
        var failedTests = 0
        var completedTests = 0
        var undefinedTests = 0
        var failedTestsIgnored = 0
        var completedTestsIgnored = 0
        var undefinedTestsIgnored = 0
        var index = 0
        val testMap = new mutable.TreeMap[Long, (com.gitb.tr.TestSuiteOverview, Counters, mutable.LinkedHashMap[Long, (TestCaseGroup, Counters)])]
        if (addTestCases) {
          report.getStatement.setTestDetails(new TestCaseDetails)
        }
        conformanceInfo.foreach { info =>
          val result = TestResultStatus.withName(info.result)
          // Test case
          val testCaseOverview = new com.gitb.tr.TestCaseOverview()
          testCaseOverview.setMetadata(new Metadata)
          testCaseOverview.getMetadata.setName(info.testCaseName.orNull)
          testCaseOverview.getMetadata.setDescription(info.testCaseDescription.orNull)
          testCaseOverview.getMetadata.setVersion(StringUtils.trimToNull(info.testCaseVersion))
          if (info.testCaseSpecReference.isDefined || info.testCaseSpecLink.isDefined || info.testCaseSpecDescription.isDefined) {
            testCaseOverview.getMetadata.setSpecification(new SpecificationInfo)
            if (info.testCaseSpecReference.isDefined) testCaseOverview.getMetadata.getSpecification.setReference(info.testCaseSpecReference.get)
            if (info.testCaseSpecLink.isDefined) testCaseOverview.getMetadata.getSpecification.setLink(info.testCaseSpecLink.get)
            if (info.testCaseSpecDescription.isDefined) testCaseOverview.getMetadata.getSpecification.setDescription(info.testCaseSpecDescription.get)
          }
          if (info.testCaseTags.isDefined) {
            testCaseOverview.getMetadata.setTags(new Tags)
            JsonUtil.parseJsTags(info.testCaseTags.get).map(tag => {
              val tagToExport = new com.gitb.core.Tag
              tagToExport.setName(tag.name)
              tagToExport.setValue(tag.description.orNull)
              tagToExport.setBackground(tag.background.orNull)
              tagToExport.setForeground(tag.foreground.orNull)
              tagToExport
            }).foreach(testCaseOverview.getMetadata.getTags.getTag.add(_))
          }
          testCaseOverview.setGroup(info.testCaseGroupIdentifier.orNull)
          testCaseOverview.setResult(TestResultType.fromValue(info.result))
          testCaseOverview.setLastUpdate(info.updateTime.map(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(_)).orNull)
          val optional = info.testCaseOptional.getOrElse(false)
          if (optional) testCaseOverview.setOptional(optional)
          val disabled = info.testCaseDisabled.getOrElse(false)
          if (disabled) testCaseOverview.setDisabled(disabled)
          // Add test session details
          if (addTestCases && (isDemo || info.sessionId.isDefined)) {
            index += 1
            testCaseOverview.setRef(index.toString)
            val testCaseReport = new TestCaseOverviewReportType
            testCaseReport.setId(testCaseOverview.getRef)
            testCaseReport.setMetadata(testCaseOverview.getMetadata)
            testCaseReport.setResult(testCaseOverview.getResult)
            info.outputMessage.foreach { msgs =>
              msgs.split('\n').foreach { msg =>
                testCaseReport.getMessage.add(msg)
              }
            }
            // Times and steps
            if (isDemo) {
              testCaseReport.setStartTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime())
              testCaseReport.setEndTime(testCaseReport.getStartTime)
              testCaseReport.setSteps(new TestCaseStepsType)
              testCaseReport.getSteps.getStep.add(createSimpleDemoSuccessStep(Some("1"), Some(testCaseReport.getStartTime)))
              testCaseReport.getSteps.getStep.add(createSimpleDemoSuccessStep(Some("2"), Some(testCaseReport.getStartTime)))
              testCaseReport.getSteps.getStep.add(createSimpleDemoSuccessStep(Some("3"), Some(testCaseReport.getStartTime)))
            } else {
              if (info.sessionId.exists(session => testResultMap.exists(_.contains(session)))) {
                val testResult = testResultMap.get(info.sessionId.get)
                testCaseReport.setStartTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testResult._1.startTime))
                testCaseReport.setEndTime(testResult._1.endTime.map(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(_)).orNull)
                // Add test steps
                val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult._2)))
                val sessionFolderInfo = repositoryUtils.getPathForTestSessionObj(info.sessionId.get, Some(testResult._1.startTime), isExpected = true)
                try {
                  val list = testCaseReportProducer.getListOfTestSteps(testcasePresentation, sessionFolderInfo.path.toFile)
                  if (list.nonEmpty) {
                    testCaseReport.setSteps(new TestCaseStepsType)
                    list.foreach { stepReport =>
                      val report = new TestCaseStepReportType()
                      report.setId(stepReport.getWrapped.getId)
                      report.setDescription(stepReport.getTitle)
                      report.setReport(stepReport.getWrapped)
                      testCaseReport.getSteps.getStep.add(report)
                    }
                  }
                } finally {
                  if (sessionFolderInfo.archived) {
                    FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
                  }
                }
              }
            }
            report.getStatement.getTestDetails.getTestCase.add(testCaseReport)
          }
          // Test suite
          val testSuiteOverview = testMap.getOrElseUpdate(info.testSuiteId.get, {
            val testSuite = new com.gitb.tr.TestSuiteOverview
            testSuite.setMetadata(new Metadata)
            testSuite.getMetadata.setName(info.testSuiteName.get)
            testSuite.getMetadata.setDescription(info.testSuiteDescription.orNull)
            testSuite.getMetadata.setVersion(StringUtils.trimToNull(info.testSuiteVersion))
            if (info.testSuiteSpecReference.isDefined || info.testSuiteSpecLink.isDefined || info.testSuiteSpecDescription.isDefined) {
              testSuite.getMetadata.setSpecification(new SpecificationInfo)
              if (info.testSuiteSpecReference.isDefined) testSuite.getMetadata.getSpecification.setReference(info.testSuiteSpecReference.get)
              if (info.testSuiteSpecLink.isDefined) testSuite.getMetadata.getSpecification.setLink(info.testSuiteSpecLink.get)
              if (info.testSuiteSpecDescription.isDefined) testSuite.getMetadata.getSpecification.setDescription(info.testSuiteSpecDescription.get)
            }
            testSuite.setResult(TestResultType.UNDEFINED)
            testSuite.setTestCases(new TestCaseOverviews)
            (testSuite, new Counters(0, 0, 0), new mutable.LinkedHashMap[Long, (models.TestCaseGroup, Counters)]())
          })
          // Record the group information
          val group = info.testCaseGroupId.map { _ =>
            models.TestCaseGroup(info.testCaseGroupId.get, info.testCaseGroupIdentifier.get, info.testCaseGroupName, info.testCaseGroupDescription, info.testSuiteId.get)
          }
          if (group.nonEmpty) {
            groupResult(testSuiteOverview._3, group.get)
          }
          // Update the test suite's results.
          if (!info.testCaseDisabled.get) {
            if (info.testCaseOptional.get) {
              if (result == TestResultStatus.SUCCESS) {
                completedTestsIgnored += 1
              } else if (result == TestResultStatus.FAILURE) {
                failedTestsIgnored += 1
              } else {
                undefinedTestsIgnored += 1
              }
            } else {
              // Required test
              if (info.testCaseGroupId.isEmpty) {
                if (result == TestResultStatus.SUCCESS) {
                  completedTests += 1
                  testSuiteOverview._2.successes += 1
                } else if (result == TestResultStatus.FAILURE) {
                  failedTests += 1
                  testSuiteOverview._2.failures += 1
                } else {
                  undefinedTests += 1
                  testSuiteOverview._2.other += 1
                }
              } else {
                if (result == TestResultStatus.SUCCESS) {
                  groupResult(testSuiteOverview._3, group.get).successes += 1
                } else if (result == TestResultStatus.FAILURE) {
                  groupResult(testSuiteOverview._3, group.get).failures += 1
                } else {
                  groupResult(testSuiteOverview._3, group.get).other += 1
                }
              }
            }
          }
          testSuiteOverview._1.getTestCases.getTestCase.add(testCaseOverview)
        }
        if (testMap.nonEmpty) {
          // Set the status of the collected test suites
          testMap.values.foreach { testSuiteInfo =>
            // Process groups
            if (testSuiteInfo._3.nonEmpty) {
              testSuiteInfo._1.setTestCaseGroups(new TestCaseGroups)
              testSuiteInfo._3.values.foreach { group =>
                val reportGroup = new tr.TestCaseGroup
                reportGroup.setId(group._1.identifier)
                reportGroup.setName(group._1.name.orNull)
                reportGroup.setDescription(group._1.description.orNull)
                testSuiteInfo._1.getTestCaseGroups.getGroup.add(reportGroup)
                if (group._2.successes > 0) {
                  completedTests += 1
                  failedTestsIgnored += group._2.failures.toInt
                  undefinedTestsIgnored += group._2.other.toInt
                  testSuiteInfo._2.successes += 1
                } else if (group._2.failures > 0) {
                  failedTests += 1
                  undefinedTestsIgnored += group._2.other.toInt
                  testSuiteInfo._2.failures += 1
                } else if (group._2.other > 0) {
                  undefinedTests += 1
                  testSuiteInfo._2.other += 1
                }
              }
            }
            // Calculate test suite result
            if (testSuiteInfo._2.failures > 0) {
              testSuiteInfo._1.setResult(TestResultType.FAILURE)
            } else if (testSuiteInfo._2.other > 0) {
              testSuiteInfo._1.setResult(TestResultType.UNDEFINED)
            } else if (testSuiteInfo._2.successes > 0) {
              testSuiteInfo._1.setResult(TestResultType.SUCCESS)
            } else {
              testSuiteInfo._1.setResult(TestResultType.UNDEFINED)
            }
          }
          report.getStatement.setTestOverview(new TestSuiteOverviews)
          // Add to overall output
          testMap.values.map(_._1).foreach { testSuite =>
            report.getStatement.getTestOverview.getTestSuite.add(testSuite)
          }
        }
        // Summary
        report.getStatement.setSummary(new ResultSummaryWithIgnoredResults)
        report.getStatement.getSummary.setStatus(TestResultType.UNDEFINED)
        if (failedTests > 0) {
          report.getStatement.getSummary.setStatus(TestResultType.FAILURE)
        } else if (undefinedTests > 0) {
          report.getStatement.getSummary.setStatus(TestResultType.UNDEFINED)
        } else if (completedTests > 0) {
          report.getStatement.getSummary.setStatus(TestResultType.SUCCESS)
        }
        report.getStatement.setLastUpdate(conformanceData.updateTime.map(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(_)).orNull)
        report.getStatement.getSummary.setSucceeded(BigInteger.valueOf(completedTests))
        report.getStatement.getSummary.setFailed(BigInteger.valueOf(failedTests))
        report.getStatement.getSummary.setIncomplete(BigInteger.valueOf(undefinedTests))
        report.getStatement.getSummary.setSucceededIgnored(BigInteger.valueOf(completedTestsIgnored))
        report.getStatement.getSummary.setFailedIgnored(BigInteger.valueOf(failedTestsIgnored))
        report.getStatement.getSummary.setIncompleteIgnored(BigInteger.valueOf(undefinedTestsIgnored))
        // Produce XML report
        Files.createDirectories(reportPath.getParent)
        Using.resource(Files.newOutputStream(reportPath)) { fos =>
          ReportGenerator.getInstance().writeConformanceStatementXmlReport(report, fos)
          fos.flush()
        }
        // Apply XSLT if needed
        applyXsltToReportAndPrettyPrint(reportPath, transformer)
        Future.successful(reportPath)
      }
    } yield report
  }

  private def groupResult(groupMap: mutable.Map[Long, (TestCaseGroup, Counters)], group: TestCaseGroup): Counters = {
    var groupResults = groupMap.get(group.id)
    if (groupResults.isEmpty) {
      groupResults = Some(group, new Counters(0, 0, 0))
      groupMap += (group.id -> groupResults.get)
    }
    groupResults.get._2
  }

  private def callCustomPdfGenerationService(serviceUri: String, inputXmlPath: Path, outputPdfPath: Path): Future[Unit] = {
    val task = for {
      request <- {
        Future.successful {
          HttpRequest.newBuilder()
            .header("Content-Type", Constants.MimeTypeXML)
            .header("Accept", Constants.MimeTypePDF)
            .POST(HttpRequest.BodyPublishers.ofInputStream(() => Files.newInputStream(inputXmlPath)))
            .uri(URI.create(serviceUri))
            .build()
        }
      }
      _ <- {
        val clientBuilder = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.ALWAYS)
        Using.resource(clientBuilder.build()) { client =>
          client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).asScala.map { response =>
            if (response.statusCode() >= 400) {
              throw ServiceCallException("PDF generation service call at [%s] returned status [%s]".formatted(serviceUri, response.statusCode()), None,
                Some(response.statusCode()),
                Option(response.headers().firstValue("Content-Type").orElse(null)),
                Option(IOUtils.toString(response.body(), StandardCharsets.UTF_8))
              )
            }
            Using.resource(Files.newOutputStream(outputPdfPath)) { output =>
              Using.resource(response.body()) { input =>
                IOUtils.copy(input, output)
              }
            }
          }
        }
      }
    } yield ()
    task.recover {
      case e: ServiceCallException =>
        throw e
      case e: Exception =>
        throw ServiceCallException("Failed to call custom PDF generation service at [%s]".formatted(serviceUri), Some(e), None, None, None)
    }
  }

  private def applyXsltToReportAndPrettyPrint(reportPath: Path, xsltPath: Option[Path]): Path = {
    if (xsltPath.isDefined) {
      val tempReportPath = reportPath.resolveSibling("temp."+reportPath.getFileName.toString)
      try {
        Using.resource(Files.newInputStream(xsltPath.get)) { xsltStream =>
          Using.resource(Files.newInputStream(reportPath)) { xmlStream =>
            val transformer = XMLUtils.getSecureTransformerFactory.newTransformer(
              new StAXSource(XMLUtils.getSecureXMLInputFactory.createXMLStreamReader(xsltStream))
            )
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.transform(
              new StAXSource(XMLUtils.getSecureXMLInputFactory.createXMLStreamReader(xmlStream)),
              new StreamResult(tempReportPath.toFile))
          }
        }
      } catch {
        case e: TransformerConfigurationException =>
          throw new IllegalArgumentException("An error occurred while preparing the XSLT transformation", e)
        case e: TransformerException =>
          throw new IllegalArgumentException("An error occurred during the XSLT transformation", e)
      }
      Files.move(tempReportPath, reportPath, StandardCopyOption.REPLACE_EXISTING)
    } else {
      XMLUtils.prettyPrintXmlFile(reportPath)
    }
    reportPath
  }

  def generateDemoConformanceStatementReport(reportPath: Path, reportSettings: CommunityReportSettings, transformer: Option[Path], addTestCases: Boolean, communityId: Long): Future[Path] = {
    for {
      _ <- {
        if (reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)) {
          // Delegate to external service. First create XML report.
          val tempXmlReport = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          // Generate the XML report and apply stylesheet if defined and needed.
          generateDemoConformanceStatementReportInXML(tempXmlReport, transformer.filter(_ => reportSettings.customPdfsWithCustomXml), addTestCases, communityId).flatMap { _ =>
            // Call service.
            callCustomPdfGenerationService(reportSettings.customPdfService.get, tempXmlReport, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(tempXmlReport.toFile)
          }
        } else {
          getReportLabels(communityId).flatMap { labels =>
            val conformanceInfo = createDemoDataForConformanceStatementReport(labels)
            generateCoreConformanceReport(reportPath, addTestCases, Some("Conformance Statement Report"), addDetails = true, addTestCaseResults = true, addTestStatus = true, addMessage = false, addPageNumbers = true, None, conformanceInfo, labels, communityId, None, isDemo = true).map { _ =>
              reportPath
            }
          }
        }
      }
      // Sign report if needed.
      report <- signReportIfNeeded(reportSettings, reportPath)
    } yield report
  }

  def generateConformanceStatementReport(reportPath: Path, addTestCases: Boolean, actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    generateCoreConformanceReport(reportPath, addTestCases, None, actorId, systemId, labels, communityId, snapshotId)
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, message: Option[String], actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long]): Future[Path] = {
    for {
      // Load report data.
      conformanceInfo <- conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).map(_.getDetails(None))
      reportSettings <- getReportSettings(communityId, ReportType.ConformanceStatementReport)
      _ <- {
        if (reportSettings.customPdfs && reportSettings.customPdfService.exists(StringUtils.isNotBlank)) {
          // We have a PDF report and need to delegate its generation to an external service. First generate (or retrieve) the XML report.
          var transformer: Option[Path] = None
          if (reportSettings.customPdfsWithCustomXml) {
            transformer = repositoryUtils.getCommunityReportStylesheet(communityId, ReportType.ConformanceStatementReport)
          }
          val xmlReportPath = reportPath.resolveSibling(UUID.randomUUID().toString + ".xml")
          generateConformanceStatementReportInXML(xmlReportPath, transformer, addTestCases, conformanceInfo, isDemo = false).flatMap { _ =>
            callCustomPdfGenerationService(reportSettings.customPdfService.get, xmlReportPath, reportPath).map { _ =>
              reportPath
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(xmlReportPath.toFile)
          }
        } else {
          generateCoreConformanceReport(reportPath, addTestCases, Some("Conformance Statement Report"), addDetails = true, addTestCaseResults = true, addTestStatus = true, addMessage = false, addPageNumbers = true, message, conformanceInfo, labels, communityId, snapshotId, isDemo = false)
        }
      }
      // Sign report if needed.
      report <- signReportIfNeeded(reportSettings, reportPath)
    } yield report
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, title: Option[String], addDetails: Boolean, addTestCaseResults: Boolean, addTestStatus: Boolean, addMessage: Boolean, addPageNumbers: Boolean, message: Option[String], conformanceInfo: List[ConformanceStatementFull], labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long], isDemo: Boolean): Future[Path] = {
    val conformanceData = conformanceInfo.head
    val reportDate = Calendar.getInstance().getTime
    val specs = reportHelper.createReportSpecs(Some(communityId))
    for {
      displayActor <- {
        if (isDemo) {
          Future.successful(true)
        } else {
          conformanceManager.getActorIdsToDisplayInStatementsWrapper(List(conformanceData), snapshotId).map(_.contains(conformanceData.actorId))
        }
      }
      testResultMap <- {
        if (addTestCases) {
          testResultManager.getTestResultsForSessions(conformanceInfo.filter(_.sessionId.isDefined).map(_.sessionId.get)).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      overview <- {
        val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val overview = new com.gitb.reports.dto.ConformanceStatementOverview()
        // Labels
        overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
        overview.setLabelSpecificationGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup))
        overview.setLabelSpecificationInGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationInGroup))
        overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
        overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
        overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
        overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))

        overview.setIncludeTestCases(addTestCases)
        overview.setTitle(title.orNull)
        overview.setTestDomain(conformanceData.domainNameFull)
        overview.setTestSpecificationGroup(conformanceData.specificationGroupNameFull.orNull)
        overview.setTestSpecification(conformanceData.specificationNameFull)

        if (displayActor) {
          overview.setTestActor(conformanceData.actorFull)
        }

        overview.setOrganisation(conformanceData.organizationName)
        overview.setSystem(conformanceData.systemName)
        overview.setIncludeDetails(addDetails)
        overview.setIncludeMessage(addMessage)
        overview.setIncludePageNumbers(addPageNumbers)

        if (addTestCaseResults) {
          overview.setTestSuites(new util.ArrayList[com.gitb.reports.dto.TestSuiteOverview]())
        }
        var failedTests = 0
        var completedTests = 0
        var undefinedTests = 0
        var failedTestsIgnored = 0
        var completedTestsIgnored = 0
        var undefinedTestsIgnored = 0
        var index = 1
        val testMap = new mutable.TreeMap[Long, (com.gitb.reports.dto.TestSuiteOverview, Counters, mutable.LinkedHashMap[Long, (TestCaseGroup, Counters)])]
        conformanceInfo.foreach { info =>
          val result = TestResultStatus.withName(info.result)
          if (addTestCaseResults) {
            val testCaseOverview = new com.gitb.reports.dto.TestCaseOverview()
            testCaseOverview.setId(index.toString)
            testCaseOverview.setTestName(info.testCaseName.get)
            if (info.testCaseDescription.isDefined) {
              testCaseOverview.setTestDescription(info.testCaseDescription.get)
            } else {
              testCaseOverview.setTestDescription("-")
            }
            testCaseOverview.setGroup(info.testCaseGroupIdentifier.orNull)
            testCaseOverview.setSpecReference(info.testCaseSpecReference.orNull)
            testCaseOverview.setSpecDescription(info.testCaseSpecDescription.orNull)
            testCaseOverview.setSpecLink(info.testCaseSpecLink.orNull)
            testCaseOverview.setReportResult(info.result)
            info.outputMessage.foreach { msgs =>
              testCaseOverview.setOutputMessages(new util.ArrayList())
              msgs.split('\n').foreach { msg =>
                testCaseOverview.getOutputMessages.add(msg)
              }
            }
            testCaseOverview.setOptional(info.testCaseOptional.get)
            testCaseOverview.setDisabled(info.testCaseDisabled.get)
            if (info.testCaseTags.isDefined) {
              testCaseOverview.setTags(parseTestCaseTags(info.testCaseTags.get))
            }
            if (addTestCases) {
              testCaseOverview.setTitle("Test Case Report #" + index)
              testCaseOverview.setOrganisation(info.organizationName)
              testCaseOverview.setSystem(info.systemName)
              testCaseOverview.setTestDomain(info.domainNameFull)
              testCaseOverview.setTestSpecification(info.specificationNameFull)
              testCaseOverview.setTestActor(info.actorFull)
              if (info.sessionId.exists(session => testResultMap.exists(_.contains(session)))) {
                val testResult = testResultMap.get(info.sessionId.get)
                testCaseOverview.setStartTime(sdf.format(new Date(testResult._1.startTime.getTime)))
                if (testResult._1.endTime.isDefined) {
                  testCaseOverview.setEndTime(sdf.format(new Date(testResult._1.endTime.get.getTime)))
                }
                val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult._2)))
                val sessionFolderInfo = repositoryUtils.getPathForTestSessionObj(info.sessionId.get, Some(testResult._1.startTime), isExpected = true)
                try {
                  val list = testCaseReportProducer.getListOfTestSteps(testcasePresentation, sessionFolderInfo.path.toFile)
                  for (stepReport <- list) {
                    testCaseOverview.getSteps.add(ReportGenerator.getInstance().fromTestStepReportType(stepReport.getWrapped, stepReport.getTitle, specs))
                  }
                  if (testCaseOverview.getSteps.isEmpty) {
                    testCaseOverview.setSteps(null)
                  }
                } finally {
                  if (sessionFolderInfo.archived) {
                    FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
                  }
                }
              } else {
                testCaseOverview.setStartTime("-")
                testCaseOverview.setEndTime("-")
                testCaseOverview.setSteps(null)
              }
            }
            val testSuiteOverview = testMap.getOrElseUpdate(info.testSuiteId.get, {
              val testSuite = new com.gitb.reports.dto.TestSuiteOverview
              testSuite.setTestSuiteId(info.testSuiteId.get)
              testSuite.setTestSuiteName(info.testSuiteName.get)
              testSuite.setTestSuiteDescription(info.testSuiteDescription.orNull)
              testSuite.setSpecReference(info.testSuiteSpecReference.orNull)
              testSuite.setSpecDescription(info.testSuiteSpecDescription.orNull)
              testSuite.setSpecLink(info.testSuiteSpecLink.orNull)
              testSuite.setOverallStatus("UNDEFINED")
              testSuite.setTestCases(new util.ArrayList[com.gitb.reports.dto.TestCaseOverview]())
              (testSuite, new Counters(0, 0, 0), new mutable.LinkedHashMap[Long, (models.TestCaseGroup, Counters)]())
            })
            // Record the group information
            val group = info.testCaseGroupId.map { _ =>
              models.TestCaseGroup(info.testCaseGroupId.get, info.testCaseGroupIdentifier.get, info.testCaseGroupName, info.testCaseGroupDescription, info.testSuiteId.get)
            }
            if (group.nonEmpty) {
              groupResult(testSuiteOverview._3, group.get)
            }
            // Update the test suite's results.
            if (!info.testCaseDisabled.get) {
              if (info.testCaseOptional.get) {
                if (result == TestResultStatus.SUCCESS) {
                  completedTestsIgnored += 1
                } else if (result == TestResultStatus.FAILURE) {
                  failedTestsIgnored += 1
                } else {
                  undefinedTestsIgnored += 1
                }
              } else {
                // Required test
                if (info.testCaseGroupId.isEmpty) {
                  if (result == TestResultStatus.SUCCESS) {
                    completedTests += 1
                    testSuiteOverview._2.successes += 1
                  } else if (result == TestResultStatus.FAILURE) {
                    failedTests += 1
                    testSuiteOverview._2.failures += 1
                  } else {
                    undefinedTests += 1
                    testSuiteOverview._2.other += 1
                  }
                } else {
                  if (result == TestResultStatus.SUCCESS) {
                    groupResult(testSuiteOverview._3, group.get).successes += 1
                  } else if (result == TestResultStatus.FAILURE) {
                    groupResult(testSuiteOverview._3, group.get).failures += 1
                  } else {
                    groupResult(testSuiteOverview._3, group.get).other += 1
                  }
                }
              }
            }
            testSuiteOverview._1.getTestCases.add(testCaseOverview)
            index += 1
          }
        }
        if (testMap.nonEmpty) {
          // Set the status of the collected test suites
          testMap.values.foreach { testSuiteInfo =>
            // Process groups
            if (testSuiteInfo._3.nonEmpty) {
              testSuiteInfo._1.setTestCaseGroups(new util.ArrayList[dto.TestCaseGroup]())
              testSuiteInfo._3.values.foreach { group =>
                val reportGroup = new dto.TestCaseGroup
                reportGroup.setId(group._1.identifier)
                reportGroup.setName(group._1.name.orNull)
                reportGroup.setDescription(group._1.description.orNull)
                testSuiteInfo._1.getTestCaseGroups.add(reportGroup)
                if (group._2.successes > 0) {
                  completedTests += 1
                  failedTestsIgnored += group._2.failures.toInt
                  undefinedTestsIgnored += group._2.other.toInt
                  testSuiteInfo._2.successes += 1
                } else if (group._2.failures > 0) {
                  failedTests += 1
                  undefinedTestsIgnored += group._2.other.toInt
                  testSuiteInfo._2.failures += 1
                } else if (group._2.other > 0) {
                  undefinedTests += 1
                  testSuiteInfo._2.other += 1
                }
              }
            }
            // Calculate test suite result
            if (testSuiteInfo._2.failures > 0) {
              testSuiteInfo._1.setOverallStatus(TestResultType.FAILURE.value())
            } else if (testSuiteInfo._2.other > 0) {
              testSuiteInfo._1.setOverallStatus(TestResultType.UNDEFINED.value())
            } else if (testSuiteInfo._2.successes > 0) {
              testSuiteInfo._1.setOverallStatus(TestResultType.SUCCESS.value())
            } else {
              testSuiteInfo._1.setOverallStatus(TestResultType.UNDEFINED.value())
            }
          }
          // Add to overall output
          overview.setTestSuites(new util.ArrayList[com.gitb.reports.dto.TestSuiteOverview](testMap.values.map(_._1).toList.asJavaCollection))
        }
        // Summary
        overview.setOverallStatus(TestResultType.UNDEFINED.value())
        if (failedTests > 0) {
          overview.setOverallStatus(TestResultType.FAILURE.value())
        } else if (undefinedTests > 0) {
          overview.setOverallStatus(TestResultType.UNDEFINED.value())
        } else if (completedTests > 0) {
          overview.setOverallStatus(TestResultType.SUCCESS.value())
        }
        overview.setCompletedTests(completedTests)
        overview.setFailedTests(failedTests)
        overview.setUndefinedTests(undefinedTests)
        overview.setCompletedTestsIgnored(completedTestsIgnored)
        overview.setFailedTestsIgnored(failedTestsIgnored)
        overview.setUndefinedTestsIgnored(undefinedTestsIgnored)
        overview.setIncludeTestStatus(addTestStatus)
        overview.setReportDate(sdf.format(reportDate))
        Future.successful(overview)
      }
      // Add also custom message (doing here as we need the overall result to be calculated)
      overview <- {
        // Replace message placeholders
        if (addMessage && message.isDefined) {
          resolveConformanceStatementCertificateMessage(message.get, communityId, snapshotId, conformanceData, overview.getOverallStatus, isDemo, useUrlPlaceholders = false, reportDate).map { messageToUse =>
            overview.setMessage(messageToUse)
            overview
          }
        } else {
          Future.successful(overview)
        }
      }
      report <- {
        Files.createDirectories(reportPath.getParent)
        Using.resource(Files.newOutputStream(reportPath)) { fos =>
          ReportGenerator.getInstance().writeConformanceStatementOverviewReport(overview, fos, specs)
          fos.flush()
        }
        Future.successful(reportPath)
      }
    } yield report
  }

  def resolveConformanceStatementCertificateMessage(rawMessage: String, actorId: Long, systemId: Long, communityId: Long, snapshotId: Option[Long]): Future[String] = {
    for {
      conformanceInfo <- conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).map(_.getDetails(None))
      message <- {
        // Calculate overall status
        var failedTests = 0
        var completedTests = 0
        var undefinedTests = 0
        conformanceInfo.foreach { info =>
          val result = TestResultStatus.withName(info.result)
          if (!info.testCaseDisabled.get && !info.testCaseOptional.get) {
            if (result == TestResultStatus.SUCCESS) completedTests += 1
            else if (result == TestResultStatus.FAILURE) failedTests += 1
            else undefinedTests += 1
          }
        }
        val overallStatus = if (failedTests > 0) {
          "FAILURE"
        } else if (undefinedTests > 0) {
          "UNDEFINED"
        } else if (completedTests > 0) {
          "SUCCESS"
        } else {
          "UNDEFINED"
        }
        // Process placeholders
        val reportDate = Calendar.getInstance().getTime
        resolveConformanceStatementCertificateMessage(rawMessage, communityId, snapshotId, conformanceInfo.head, overallStatus, isDemo = false, useUrlPlaceholders = true, reportDate)
      }
    } yield message
  }

  private def resolveConformanceStatementCertificateMessage(rawMessage: String, communityId: Long, snapshotId: Option[Long], conformanceData: ConformanceStatementFull, overallStatus: String, isDemo: Boolean, useUrlPlaceholders: Boolean, reportDate: Date): Future[String] = {
    for {
      message <- Future.successful(rawMessage)
      message <- replaceDomainParameters(message, communityId, snapshotId)
      message <- replaceOrganisationPropertyPlaceholders(message, isDemo, communityId, Some(conformanceData.organizationId), snapshotId)
      message <- replaceSystemPropertyPlaceholders(message, isDemo, communityId, Some(conformanceData.systemId), snapshotId)
      message <- replaceSimplePlaceholders(message, communityId, Some(conformanceData.actorFull), Some(conformanceData.specificationNameFull), Some(conformanceData.specificationGroupOptionNameFull), conformanceData.specificationGroupNameFull, Some(conformanceData.domainNameFull), Some(conformanceData.organizationName), Some(conformanceData.systemName), snapshotId)
      message <- {
        var messageToUse = message
        messageToUse = replaceBadgePlaceholders(messageToUse, isDemo, Some(conformanceData.specificationId), Some(conformanceData.actorId), snapshotId, overallStatus, useUrlPlaceholders, Some(conformanceData.systemId))
        messageToUse = replaceDatePlaceholder(messageToUse, Some(reportDate), PLACEHOLDER_REPORT_DATE_REGEXP)
        messageToUse = replaceDatePlaceholder(messageToUse, conformanceData.updateTime, PLACEHOLDER_LAST_UPDATE_DATE_REGEXP)
        if (!useUrlPlaceholders) {
          messageToUse = replaceBadgePreviewUrls(messageToUse, snapshotId)
        }
        Future.successful(messageToUse)
      }
    } yield message
  }

  def getAllReportSettings(communityId: Long): Future[List[CommunityReportSettings]] = {
    DB.run(
      PersistenceSchema.communityReportSettings
      .filter(_.community === communityId)
      .result
    ).map(_.toList)
  }

  def getReportSettings(communityId: Long, reportType: ReportType): Future[CommunityReportSettings] = {
    DB.run(
      PersistenceSchema.communityReportSettings
        .filter(_.community === communityId)
        .filter(_.reportType === reportType.id.toShort)
        .result
        .headOption
    ).map { persistedSettings =>
      persistedSettings.getOrElse(CommunityReportSettings(
        reportType.id.toShort, signPdfs = false, customPdfs = false, customPdfsWithCustomXml = false, None, communityId)
      )
    }
  }

  def updateReportSettingsInternal(reportSettings: CommunityReportSettings, stylesheetFile: Option[Option[Path]], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      // Check if we already have persisted settings.
      settingsExist <- PersistenceSchema.communityReportSettings
        .filter(_.community === reportSettings.community)
        .filter(_.reportType === reportSettings.reportType)
        .exists
        .result
      // Update settings in DB.
      _ <- {
        if (settingsExist) {
          // Update
          PersistenceSchema.communityReportSettings
            .filter(_.community === reportSettings.community)
            .filter(_.reportType === reportSettings.reportType)
            .map(x => (x.signPdfs, x.customPdfs, x.customPdfsWithCustomXml, x.customPdfService))
            .update((reportSettings.signPdfs, reportSettings.customPdfs, reportSettings.customPdfsWithCustomXml, reportSettings.customPdfService))
        } else {
          // Create
          PersistenceSchema.communityReportSettings += reportSettings
        }
      }
      // Update stylesheet.
      _ <- {
        if (stylesheetFile.isDefined) {
          onSuccessCalls += (() => {
            if (stylesheetFile.get.isEmpty) {
              repositoryUtils.deleteCommunityReportStylesheet(reportSettings.community, ReportType.apply(reportSettings.reportType))
            } else {
              repositoryUtils.saveCommunityReportStylesheet(reportSettings.community, ReportType.apply(reportSettings.reportType), stylesheetFile.flatten.get)
            }
          })
        }
        DBIO.successful(())
      }
    } yield ()
  }

  def updateReportSettings(reportSettings: CommunityReportSettings, stylesheetFile: Option[Option[Path]]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = updateReportSettingsInternal(reportSettings, stylesheetFile, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally).map(_ => ())
  }

  def updateConformanceCertificateSettings(certificateSettings: ConformanceCertificate, reportSettings: CommunityReportSettings, stylesheetPath: Option[Option[Path]]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      _ <- updateReportSettingsInternal(reportSettings, stylesheetPath, onSuccessCalls)
      _ <- updateConformanceCertificateSettingsInternal(certificateSettings)
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateConformanceOverviewCertificateSettings(certificateSettings: ConformanceOverviewCertificateWithMessages, reportSettings: CommunityReportSettings, stylesheetPath: Option[Option[Path]]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      _ <- updateReportSettingsInternal(reportSettings, stylesheetPath, onSuccessCalls)
      _ <- updateConformanceOverviewCertificateSettingsInternal(certificateSettings)
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateConformanceCertificateSettingsInternal(data: ConformanceCertificate): DBIO[_] = {
    for {
      existingId <- PersistenceSchema.conformanceCertificates.filter(_.community === data.community).map(_.id).result.headOption
      _ <- {
        if (existingId.isEmpty) {
          // Create settings
          PersistenceSchema.insertConformanceCertificate += data
        } else {
          // Update settings
          PersistenceSchema.conformanceCertificates.filter(_.id === existingId)
            .map(x => (x.title, x.message, x.includePageNumbers, x.includeTitle, x.includeDetails, x.includeMessage, x.includeSignature, x.includeTestCases, x.includeTestStatus))
            .update((data.title, data.message, data.includePageNumbers, data.includeTitle, data.includeDetails, data.includeMessage, data.includeSignature, data.includeTestCases, data.includeTestStatus))
        }
      }
    } yield ()
  }

  def updateConformanceOverviewCertificateSettingsInternal(data: ConformanceOverviewCertificateWithMessages): DBIO[_] = {
    for {
      existingId <- PersistenceSchema.conformanceOverviewCertificates.filter(_.community === data.settings.community).map(_.id).result.headOption
      _ <- {
        if (existingId.isEmpty) {
          // Create settings
          PersistenceSchema.insertConformanceOverviewCertificate += data.settings
        } else {
          // Update settings
          PersistenceSchema.conformanceOverviewCertificates.filter(_.id === existingId)
            .map(x => (x.title, x.includePageNumbers, x.includeTitle, x.includeDetails, x.includeMessage, x.includeSignature, x.includeStatements, x.includeStatementDetails, x.includeStatementStatus, x.enableAllLevel, x.enableDomainLevel, x.enableGroupLevel, x.enableSpecificationLevel))
            .update((data.settings.title, data.settings.includePageNumbers, data.settings.includeTitle, data.settings.includeDetails, data.settings.includeMessage, data.settings.includeSignature, data.settings.includeStatements, data.settings.includeStatementDetails, data.settings.includeStatementStatus, data.settings.enableAllLevel, data.settings.enableDomainLevel, data.settings.enableGroupLevel, data.settings.enableSpecificationLevel))
        }
      }
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        val updatedIds = new mutable.HashSet[Long]()
        // Update matching messages
        if (data.settings.includeMessage) {
          data.messages.foreach { message =>
            if (message.id != 0L) {
              updatedIds += message.id
              actions += PersistenceSchema.conformanceOverviewCertificateMessages.filter(_.id === message.id).map(_.message).update(message.message)
            }
          }
        }
        // Delete other existing messages
        actions += PersistenceSchema.conformanceOverviewCertificateMessages
          .filter(_.community === data.settings.community)
          .filterNot(_.id inSet updatedIds)
          .delete
        // Insert new messages
        if (data.settings.includeMessage) {
          data.messages.foreach { message =>
            if (message.id == 0L) {
              actions += (PersistenceSchema.conformanceOverviewCertificateMessages += message)
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def processAutomationReportRequest(reportPath: Path, organisationKey: String, sessionId: String, contentType: String): Future[Option[Path]] = {
    DB.run(
      for {
        organisationData <- apiHelper.loadOrganisationDataForAutomationProcessing(organisationKey)
        _ <- apiHelper.checkOrganisationForAutomationApiUse(organisationData)
        sessionData <- PersistenceSchema.testResults
          .filter(_.organizationId === organisationData.get.organisationId)
          .filter(_.testSessionId === sessionId)
          .map(_.testSessionId)
          .result
          .headOption
      } yield (organisationData.get.communityId, sessionData)
    ).flatMap { result =>
      if (result._2.isDefined) {
        generateTestCaseReport(reportPath, result._2.get, contentType, Some(result._1), None)
      } else {
        Future.successful(None)
      }
    }
  }

  def processAutomationStatusRequest(organisationKey: String, sessionIds: List[String], withLogs: Boolean, withReports: Boolean): Future[Seq[TestSessionStatus]] = {
    for {
      result <- {
        DB.run(
          for {
            organisationData <- apiHelper.loadOrganisationDataForAutomationProcessing(organisationKey)
            _ <- apiHelper.checkOrganisationForAutomationApiUse(organisationData)
            sessionData <- PersistenceSchema.testResults
              .filter(_.organizationId === organisationData.get.organisationId)
              .filter(_.testSessionId inSet sessionIds)
              .map(x => (x.testSessionId, x.startTime, x.endTime, x.result, x.outputMessage))
              .result
          } yield (organisationData.get.communityId, sessionData)
        )
      }
      report <- {
        val communityId = result._1
        val sessionData = result._2
        Future.traverse(sessionData) { session =>
          for {
            logs <- Future.successful {
              if (withLogs) {
                testResultManager.getTestSessionLog(session._1, session._2, isExpected = true)
              } else {
                None
              }
            }
            reportContent <- {
              if (withReports) {
                generateTestCaseReport(repositoryUtils.getReportTempFile(".xml"), session._1, Constants.MimeTypeXML, Some(communityId), None).map { report =>
                  try {
                    report.filter(Files.exists(_)).map(Files.readString)
                  } finally {
                    if (report.exists(Files.exists(_))) {
                      FileUtils.deleteQuietly(report.get.toFile)
                    }
                  }
                }
              } else {
                Future.successful(None)
              }
            }
            result <- Future.successful(TestSessionStatus(session._1, session._2, session._3, session._4, session._5, logs, reportContent))
          } yield result
        }
      }
    } yield report
  }

}

private class ReportData(
  val skipDomain: Boolean,
  var domainName: Option[String] = None,
  var domainDescription: Option[String] = None,
  var domainReportMetadata: Option[String] = None,
  var groupName: Option[String] = None,
  var groupDescription: Option[String] = None,
  var groupReportMetadata: Option[String] = None,
  var specName: Option[String] = None,
  var specDescription: Option[String] = None,
  var specReportMetadata: Option[String] = None,
  var actorName: Option[String] = None,
  var actorDescription: Option[String] = None,
  var actorReportMetadata: Option[String] = None
) {}

private class ConformanceOverviewTracker(var withIndexes: Boolean) {

  private var index = 0
  var successCount = 0
  var failureCount = 0
  var incompleteCount = 0

  def nextIndex(): Int = {
    index = index + 1
    index
  }

  def aggregateStatus(): TestResultType = {
    if (failureCount > 0) {
      TestResultType.FAILURE
    } else if (incompleteCount > 0) {
      TestResultType.UNDEFINED
    } else {
      TestResultType.SUCCESS
    }
  }

  def addResult(result: TestResultType): Unit = {
    result match {
      case TestResultType.SUCCESS => successCount = successCount + 1
      case TestResultType.FAILURE => failureCount = failureCount + 1
      case _ => incompleteCount = incompleteCount + 1
    }
  }

}

private class Counters(var successes: Long, var failures: Long, var other: Long) extends ResultCountHolder {

  override def completedCount(): Long = successes

  override def failedCount(): Long = failures

  override def otherCount(): Long = other

}
