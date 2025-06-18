/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import com.gitb.core.Metadata
import com.gitb.reports.dto.TestCaseOverview
import com.gitb.reports.{ReportGenerator, ReportSpecs}
import com.gitb.tbs.TestStepStatus
import com.gitb.tpl._
import com.gitb.tr._
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import managers.TestCaseReportProducer.ReportGenerationInput
import models.{CommunityLabels, Constants, SessionReportPath}
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import java.io.{File, StringReader}
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, UUID}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Using

object TestCaseReportProducer {

  case class ReportGenerationInput(stepReports: List[TitledTestStepReportType], exportedReportPath: File, testCase: Option[models.TestCase], sessionId: String)

}

@Singleton
class TestCaseReportProducer @Inject() (reportHelper: ReportHelper,
                                        testResultManager: TestResultManager,
                                        testCaseManager: TestCaseManager,
                                        communityLabelManager: CommunityLabelManager,
                                        repositoryUtils: RepositoryUtils,
                                        dbConfigProvider: DatabaseConfigProvider)
                                       (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  val logger: Logger = LoggerFactory.getLogger("TestCaseReportProducer")
  private val codec = new URLCodec()
  import dbConfig.profile.api._

  def generateDetailedTestCaseReport(sessionId: String, contentType: Option[String], labelSupplier: Option[() => Future[Map[Short, CommunityLabels]]], reportSpecSupplier: Option[() => ReportSpecs] = None): Future[SessionReportPath] = {
    for {
      // Load test session folder
      sessionFolderInfo <- {
        repositoryUtils.getPathForTestSession(codec.decode(sessionId), isExpected = true).map { sessionFolderInfo =>
          if (logger.isDebugEnabled) {
            logger.debug("Reading test case report [{}] from the file [{}]", codec.decode(sessionId), sessionFolderInfo)
          }
          sessionFolderInfo
        }
      }
      // Load test result
      testResult <- testResultManager.getTestResultForSessionWrapper(sessionId)
      // Generate the report
      exportedReport <- {
        if (testResult.isDefined) {
          val reportData = contentType match {
            case Some(Constants.MimeTypePDF) => (".report.pdf", (input: ReportGenerationInput) => {
              generateDetailedTestCaseReportPdf(input, labelSupplier.getOrElse(() => Future.successful(Map.empty[Short, CommunityLabels])).apply(), reportSpecSupplier.getOrElse(() => reportHelper.createReportSpecs()).apply())
            })
            case _ => (".report.v2.xml", (input: ReportGenerationInput) => {
              generateDetailedTestCaseReportXml(input)
            })
          }
          val exportedReport = if (testResult.get._1.endTime.isEmpty || contentType.contains(Constants.MimeTypePDF)) {
            // This name will be unique to ensure that a report generated for a pending session never gets cached. Also, PDF reports are never cached.
            new File(sessionFolderInfo.path.toFile, UUID.randomUUID().toString + reportData._1)
          } else {
            // XML reports for completed test sessions are cached as they will never change.
            new File(sessionFolderInfo.path.toFile, "report" + reportData._1)
          }
          if (!exportedReport.exists() && testResult.get._1.testCaseId.isDefined) {
            testCaseManager.getTestCase(testResult.get._1.testCaseId.get.toString).flatMap { testCase =>
              val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult.get._2)))
              val list = getListOfTestSteps(testcasePresentation, sessionFolderInfo.path.toFile)
              reportData._2.apply(ReportGenerationInput(list, exportedReport, testCase, sessionId)).map { _ =>
                Some(exportedReport)
              }
            }
          } else {
            Future.successful(Some(exportedReport))
          }
        } else {
          Future.successful(None)
        }
      }
      // Finalise the response
      result <- {
        if (exportedReport.exists(_.exists())) {
          Future.successful {
            SessionReportPath(Some(exportedReport.get.toPath), sessionFolderInfo)
          }
        } else {
          Future.successful {
            SessionReportPath(None, sessionFolderInfo)
          }
        }
      }
    } yield result
  }

  private def generateDetailedTestCaseReportXml(input: ReportGenerationInput): Future[Path] = {
    val reportPath = Paths.get(input.exportedReportPath.getAbsolutePath)
    for {
      overview <- {
        DB.run(PersistenceSchema.testResults.filter(_.testSessionId === input.sessionId).result.head).map { testResult =>
          val overview = new TestCaseOverviewReportType()
          overview.setResult(TestResultType.fromValue(testResult.result))
          testResult.outputMessage.foreach { msgs =>
            msgs.split('\n').foreach { msg =>
              overview.getMessage.add(msg)
            }
          }
          overview.setStartTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testResult.startTime))
          if (testResult.endTime.isDefined) {
            overview.setEndTime(XMLDateTimeUtils.getXMLGregorianCalendarDateTime(testResult.endTime.get))
          }
          if (input.testCase.isDefined) {
            overview.setId(input.testCase.get.identifier)
            overview.setMetadata(new Metadata())
            overview.getMetadata.setName(input.testCase.get.fullname)
            overview.getMetadata.setDescription(input.testCase.get.description.orNull)
            overview.getMetadata.setVersion(StringUtils.trimToNull(input.testCase.get.version))
          }
          if (input.stepReports.nonEmpty) {
            overview.setSteps(new TestCaseStepsType())
            input.stepReports.foreach { stepReport =>
              val report = new TestCaseStepReportType()
              report.setId(stepReport.getWrapped.getId)
              report.setDescription(stepReport.getTitle)
              report.setReport(stepReport.getWrapped)
              overview.getSteps.getStep.add(report)
            }
          }
          overview
        }
      }
      reportPath <- {
        Files.createDirectories(reportPath.getParent)
        Using.resource(Files.newOutputStream(reportPath)) { fos =>
          ReportGenerator.getInstance().writeTestCaseOverviewXmlReport(overview, fos)
          fos.flush()
        }
        Future.successful(reportPath)
      }
    } yield reportPath
  }

  private def generateDetailedTestCaseReportPdf(input: ReportGenerationInput, labels: Future[Map[Short, CommunityLabels]], specs: ReportSpecs): Future[Path] = {
    val reportPath = Paths.get(input.exportedReportPath.getAbsolutePath)
    val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    for {
      // Overview
      overview <- {
        val overview = new TestCaseOverview()
        overview.setTitle("Test Case Report")
        // Labels
        labels.map { labels =>
          overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
          overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
          overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
          overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
          overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))
          overview
        }
      }
      // Result
      overview <- {
        DB.run(PersistenceSchema.testResults.filter(_.testSessionId === input.sessionId).result.head).map { testResult =>
          // Test results
          overview.setReportResult(testResult.result)
          testResult.outputMessage.foreach { msgs =>
            overview.setOutputMessages(new util.ArrayList())
            msgs.split('\n').foreach { msg =>
              overview.getOutputMessages.add(msg)
            }
          }
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
          if (input.testCase.isDefined) {
            if (input.testCase.get.description.isDefined) {
              overview.setTestDescription(input.testCase.get.description.get)
            }
            overview.setSpecReference(input.testCase.get.specReference.orNull)
            overview.setSpecDescription(input.testCase.get.specDescription.orNull)
            overview.setSpecLink(input.testCase.get.specLink.orNull)
          } else {
            // This is a deleted test case - get data as possible from TestResult
            overview.setTestDescription("-")
          }
          for (stepReport <- input.stepReports) {
            overview.getSteps.add(ReportGenerator.getInstance().fromTestStepReportType(stepReport.getWrapped, stepReport.getTitle, specs))
          }
          if (overview.getSteps.isEmpty) {
            overview.setSteps(null)
          }
          overview
        }
      }
      // Documentation and logs
      overview <- {
        if (input.testCase.isDefined) {
          for {
            // Documentation
            overview <- {
              if (specs.isIncludeDocumentation) {
                testCaseManager.getTestCaseDocumentation(input.testCase.get.id).map { documentation =>
                  if (documentation.isDefined) {
                    overview.setDocumentation(documentation.get)
                  }
                  overview
                }
              } else {
                Future.successful(overview)
              }
            }
            // Logs
            overview <- {
              if (specs.isIncludeLogs) {
                testResultManager.getTestSessionLog(input.sessionId, isExpected = true).map { logContents =>
                  if (logContents.isDefined && logContents.get.nonEmpty) {
                    overview.setLogMessages(logContents.get.asJava)
                  }
                  overview
                }
              } else {
                Future.successful(overview)
              }
            }
          } yield overview
        } else {
          Future.successful(overview)
        }
      }
      reportPath <- {
        // Needed if no reports have been received.
        Files.createDirectories(reportPath.getParent)
        Using.resource(Files.newOutputStream(reportPath)) { fos =>
          try {
            ReportGenerator.getInstance().writeTestCaseOverviewReport(overview, fos, specs)
          } catch {
            case e: Exception =>
              throw new IllegalStateException("Unable to generate PDF report", e)
          }
        }
        Future.successful(reportPath)
      }
    } yield reportPath
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
      if (report.getReport != null && report.getReport.isInstanceOf[TAR]
        && report.getReport.asInstanceOf[TAR].getReports != null
        && report.getReport.asInstanceOf[TAR].getReports.getReports.isEmpty
        && report.getReport.asInstanceOf[TAR].getReports.getInfoOrWarningOrError.isEmpty) {
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

  def getListOfTestSteps(testPresentation: TestCase, folder: File): List[TitledTestStepReportType] = {
    val buffer = ListBuffer[TitledTestStepReportType]()
    collectStepReportsForSequence(testPresentation.getSteps, buffer, folder)
    buffer.toList
  }

}
