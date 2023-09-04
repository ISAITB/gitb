package managers

import actors.events.{ConformanceStatementSucceededEvent, TestSessionFailedEvent, TestSessionSucceededEvent}
import com.gitb.core.{AnyContent, StepStatus, ValueEmbeddingEnumeration}
import com.gitb.reports.ReportGenerator
import com.gitb.reports.dto.{ConformanceStatementOverview, TestCaseOverview, TestSuiteOverview}
import com.gitb.tbs.{ObjectFactory, TestStepStatus}
import com.gitb.tpl.TestCase
import com.gitb.tr._
import com.gitb.utils.XMLUtils
import config.Configurations
import models.Enums.TestResultStatus
import models._
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.signature.{CreateSignature, SigUtils}
import utils.{JacksonUtil, JsonUtil, MimeUtil, RepositoryUtils, TimeUtil}

import java.io.{File, FileOutputStream, StringReader}
import java.nio.file.{Files, Path}
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, UUID}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.util.Using

/**
  * Created by senan on 03.12.2014.
  */
@Singleton
class ReportManager @Inject() (reportHelper: ReportHelper, triggerHelper: TriggerHelper, testCaseReportProducer: TestCaseReportProducer, testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, dbConfigProvider: DatabaseConfigProvider, communityLabelManager: CommunityLabelManager, repositoryUtils: RepositoryUtils, testResultManager: TestResultManager) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getOrganisationActiveTestResults(organisationId: Long,
                                       systemIds: Option[List[Long]],
                                       domainIds: Option[List[Long]],
                                       specIds: Option[List[Long]],
                                       specGroupIds: Option[List[Long]],
                                       actorIds: Option[List[Long]],
                                       testSuiteIds: Option[List[Long]],
                                       testCaseIds: Option[List[Long]],
                                       startTimeBegin: Option[String],
                                       startTimeEnd: Option[String],
                                       sessionId: Option[String],
                                       sortColumn: Option[String],
                                       sortOrder: Option[String]): List[TestResult] = {
    exec(
      getTestResultsQuery(None, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, Some(List(organisationId)), systemIds, None, startTimeBegin, startTimeEnd, None, None, sessionId, Some(false), sortColumn, sortOrder)
        .result.map(_.toList)
    )
  }

  def getTestResults(page: Long,
                     limit: Long,
                     organisationId: Long,
                     systemIds: Option[List[Long]],
                     domainIds: Option[List[Long]],
                     specIds: Option[List[Long]],
                     specGroupIds: Option[List[Long]],
                     actorIds: Option[List[Long]],
                     testSuiteIds: Option[List[Long]],
                     testCaseIds: Option[List[Long]],
                     results: Option[List[String]],
                     startTimeBegin: Option[String],
                     startTimeEnd: Option[String],
                     endTimeBegin: Option[String],
                     endTimeEnd: Option[String],
                     sessionId: Option[String],
                     sortColumn: Option[String],
                     sortOrder: Option[String]): (Iterable[TestResult], Int) = {

    val query = getTestResultsQuery(None, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, Some(List(organisationId)), systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, Some(true), sortColumn, sortOrder)
    val output = exec(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield (results, resultCount)
    )
    output
  }

  def getActiveTestResults(communityIds: Option[List[Long]],
                           domainIds: Option[List[Long]],
                           specIds: Option[List[Long]],
                           specGroupIds: Option[List[Long]],
                           actorIds: Option[List[Long]],
                           testSuiteIds: Option[List[Long]],
                           testCaseIds: Option[List[Long]],
                           organisationIds: Option[List[Long]],
                           systemIds: Option[List[Long]],
                           startTimeBegin: Option[String],
                           startTimeEnd: Option[String],
                           sessionId: Option[String],
                           orgParameters: Option[Map[Long, Set[String]]],
                           sysParameters: Option[Map[Long, Set[String]]],
                           sortColumn: Option[String],
                           sortOrder: Option[String]): List[TestResult] = {
    exec(
      getTestResultsQuery(communityIds, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, conformanceManager.organisationIdsToUse(organisationIds, orgParameters), conformanceManager.systemIdsToUse(systemIds, sysParameters), None, startTimeBegin, startTimeEnd, None, None, sessionId, Some(false), sortColumn, sortOrder)
        .result.map(_.toList)
    )
  }

  def getFinishedTestResults(page: Long,
                             limit: Long,
                             communityIds: Option[List[Long]],
                             domainIds: Option[List[Long]],
                             specIds: Option[List[Long]],
                             specGroupIds: Option[List[Long]],
                             actorIds: Option[List[Long]],
                             testSuiteIds: Option[List[Long]],
                             testCaseIds: Option[List[Long]],
                             organisationIds: Option[List[Long]],
                             systemIds: Option[List[Long]],
                             results: Option[List[String]],
                             startTimeBegin: Option[String],
                             startTimeEnd: Option[String],
                             endTimeBegin: Option[String],
                             endTimeEnd: Option[String],
                             sessionId: Option[String],
                             orgParameters: Option[Map[Long, Set[String]]],
                             sysParameters: Option[Map[Long, Set[String]]],
                             sortColumn: Option[String],
                             sortOrder: Option[String]): (Iterable[TestResult], Int) = {

    val query = getTestResultsQuery(communityIds, domainIds, getSpecIdsCriterionToUse(specIds, specGroupIds), actorIds, testSuiteIds, testCaseIds, conformanceManager.organisationIdsToUse(organisationIds, orgParameters), conformanceManager.systemIdsToUse(systemIds, sysParameters), results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, Some(true), sortColumn, sortOrder)
    val output = exec(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield (results, resultCount)
    )
    output
  }

  def getTestResult(sessionId: String): Option[TestResult] = {
    val query = getTestResultsQuery(None, None, None, None, None, None, None, None, None, None, None, None, None, Some(sessionId), None, None, None)
    exec(query.result.headOption)
  }

  private def getSpecIdsCriterionToUse(specIds: Option[List[Long]], specGroupIds: Option[List[Long]]): Option[List[Long]] = {
    // We use the groups to get the applicable spec IDs. This is because specs can move around in groups and we shouldn't link
    // test results directly to the groups.
    val specIdsToUse = if (specGroupIds.isDefined && specGroupIds.get.nonEmpty) {
      exec(PersistenceSchema.specifications.filter(_.group inSet specGroupIds.get).map(_.id).result.map(x => Some(x.toList)))
    } else {
      specIds
    }
    specIdsToUse
  }

  private def getTestResultsQuery(communityIds: Option[List[Long]],
                                  domainIds: Option[List[Long]],
                                  specIds: Option[List[Long]],
                                  actorIds: Option[List[Long]],
                                  testSuiteIds: Option[List[Long]],
                                  testCaseIds: Option[List[Long]],
                                  organizationIds: Option[Iterable[Long]],
                                  systemIds: Option[Iterable[Long]],
                                  results: Option[List[String]],
                                  startTimeBegin: Option[String],
                                  startTimeEnd: Option[String],
                                  endTimeBegin: Option[String],
                                  endTimeEnd: Option[String],
                                  sessionId: Option[String],
                                  completedStatus: Option[Boolean],
                                  sortColumn: Option[String],
                                  sortOrder: Option[String]) = {
    var query = PersistenceSchema.testResults
      .filterOpt(communityIds)((table, ids) => table.communityId inSet ids)
      .filterOpt(domainIds)((table, ids) => table.domainId inSet ids)
      .filterOpt(specIds)((table, ids) => table.specificationId inSet ids)
      .filterOpt(actorIds)((table, ids) => table.actorId inSet ids)
      .filterOpt(testCaseIds)((table, ids) => table.testCaseId inSet ids)
      .filterOpt(organizationIds)((table, ids) => table.organizationId inSet ids)
      .filterOpt(systemIds)((table, ids) => table.sutId inSet ids)
      .filterOpt(results)((table, results) => table.result inSet results)
      .filterOpt(testSuiteIds)((table, ids) => table.testSuiteId inSet ids)
      .filterOpt(startTimeBegin)((table, timeStr) => table.startTime >= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(startTimeEnd)((table, timeStr) => table.startTime <= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(endTimeBegin)((table, timeStr) => table.endTime >= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(endTimeEnd)((table, timeStr) => table.endTime <= TimeUtil.parseTimestamp(timeStr))
      .filterOpt(sessionId)((table, id) => table.testSessionId === id)
      .filterOpt(completedStatus)((table, completed) => if (completed) table.endTime.isDefined else table.endTime.isEmpty)
    // Apply sorting
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
          case "actor" => query.sortBy(_.actor)
          case _ => query
        }
      }
      if (sortOrder.get == "desc") {
        query = sortColumn.get match {
          case "specification" => query.sortBy(_.specification.desc)
          case "session" => query.sortBy(_.testSessionId.desc)
          case "startTime" => query.sortBy(_.startTime.desc)
          case "endTime" => query.sortBy(_.endTime.desc)
          case "organization" => query.sortBy(_.organization.desc)
          case "system" => query.sortBy(_.sut.desc)
          case "result" => query.sortBy(_.result.desc)
          case "testCase" => query.sortBy(_.testCase.desc)
          case "actor" => query.sortBy(_.actor.desc)
          case _ => query
        }
      }
    }
    query
  }

  def getTestResultOfSession(sessionId: String): (TestResult, String) = {
    val result = testResultManager.getTestResultForSessionWrapper(sessionId)
    val testcase = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(result.get._2)))
    val json = JacksonUtil.serializeTestCasePresentation(testcase)
    (result.get._1, json)
  }

  private def removeStepDocumentation(testCase: com.gitb.tpl.TestCase): Unit = {
    if (testCase.getSteps != null && testCase.getSteps.getSteps != null) {
      import scala.jdk.CollectionConverters._
      testCase.getSteps.getSteps.asScala.foreach { step =>
        step.setDocumentation(null)
      }
    }
  }

  def createTestReport(sessionId: String, systemId: Long, testId: String, actorId: Long, testCasePresentation: com.gitb.tpl.TestCase): Unit = {
    val initialStatus = TestResultType.UNDEFINED.value()
    val startTime = TimeUtil.getCurrentTimestamp()
    val testCaseId = testId.toLong
    // Remove the step documentation because it can greatly increase the size without any use (documentation links are not displayed for non-active test sessions)
    removeStepDocumentation(testCasePresentation)
    val presentation = XMLUtils.marshalToString(new com.gitb.tpl.ObjectFactory().createTestcase(testCasePresentation))
    exec(
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

  def finishTestReport(sessionId: String, status: TestResultType, outputMessage: Option[String]): Unit = {
    val now = Some(TimeUtil.getCurrentTimestamp())
    exec(
      (for {
          _ <- PersistenceSchema.testResults
            .filter(_.testSessionId === sessionId)
            .map(x => (x.result, x.endTime, x.outputMessage))
            .update(status.value(), now, outputMessage)
          // Update also the conformance results for the system
          _ <- PersistenceSchema.conformanceResults
            .filter(_.testsession === sessionId)
            .map(x => (x.result, x.outputMessage, x.updateTime))
            .update(status.value(), outputMessage, now)
        } yield ()
      ).transactionally
    )
    // Triggers linked to test sessions: (communityID, systemID, actorID)
    val sessionIds: Option[(Option[Long], Option[Long], Option[Long])] = exec(
      PersistenceSchema.testResults
        .filter(_.testSessionId === sessionId)
        .map(x => (x.communityId, x.sutId, x.actorId))
        .result
        .headOption
    )
    if (sessionIds.isDefined && sessionIds.get._1.isDefined && sessionIds.get._2.isDefined && sessionIds.get._3.isDefined) {
      val communityId = sessionIds.get._1.get
      val systemId = sessionIds.get._2.get
      // We have all the data we need to fire the triggers.
      if (status == TestResultType.SUCCESS) {
        triggerHelper.publishTriggerEvent(new TestSessionSucceededEvent(communityId, sessionId))
      } else if (status == TestResultType.FAILURE) {
        triggerHelper.publishTriggerEvent(new TestSessionFailedEvent(communityId, sessionId))
      }
      // See if the conformance statement is now successfully completed and fire an additional trigger if so.
      val completedActors = conformanceManager.getCompletedConformanceStatementsForTestSession(systemId, sessionId)
      completedActors.foreach { actorId =>
        triggerHelper.publishTriggerEvent(new ConformanceStatementSucceededEvent(communityId, systemId, actorId))
      }
    }
    // Flush remaining log messages
    testResultManager.flushSessionLogs(sessionId, None)
  }

  def setEndTimeNow(sessionId: String): Unit = {
    val now = Some(TimeUtil.getCurrentTimestamp())
    exec (
      (for {
        testSession <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
        _ <- {
          if (testSession.isDefined) {
            (for {t <- PersistenceSchema.testResults if t.testSessionId === sessionId} yield t.endTime).update(now) andThen
              (for {c <- PersistenceSchema.conformanceResults if c.testsession === sessionId} yield (c.result, c.updateTime)).update(testSession.get.result, now)
          } else {
            DBIO.successful(())
          }
        }
      } yield ()
      ).transactionally
    )
  }

  private def writeValueToFile(item: AnyContent, sessionFolder: Path, writeFn: Path => Unit): Unit = {
    val dataFolder = repositoryUtils.getPathForTestSessionData(sessionFolder)
    Files.createDirectories(dataFolder)
    val fileUuid = UUID.randomUUID().toString
    val dataFile = Path.of(dataFolder.toString, fileUuid)
    writeFn.apply(dataFile)
    item.setValue(s"___[[$fileUuid]]___")
  }

  private def decoupleLargeData(item: AnyContent, sessionFolder: Path): Unit = {
    if (item != null) {
      // We check first the length of the string as for large content this will already be over the threshold.
      if (item.getValue != null && (item.getValue.length > Configurations.TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD || item.getValue.getBytes.length > Configurations.TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD)) {
        if (item.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64) {
          if (MimeUtil.isDataURL(item.getValue)) {
            writeValueToFile(item, sessionFolder, file => { Files.write(file, Base64.decodeBase64(MimeUtil.getBase64FromDataURL(item.getValue))) })
          } else {
            writeValueToFile(item, sessionFolder, file => { Files.write(file, Base64.decodeBase64(item.getValue)) })
          }
        } else {
          writeValueToFile(item, sessionFolder, file => { Files.writeString(file, item.getValue) })
        }
      }
      // Check children.
      item.getItem.forEach { child =>
        decoupleLargeData(child, sessionFolder)
      }
    }
  }

  def createTestStepReport(sessionId: String, step: TestStepStatus): Option[String] = {
    var savedPath: Option[String] = None
    //save status reports only when step is concluded with either COMPLETED or ERROR state
    if (step.getReport != null && (step.getStatus == StepStatus.COMPLETED || step.getStatus == StepStatus.ERROR || step.getStatus == StepStatus.WARNING)) {
      // Check to see if we have already recorded this to avoid potential concurrency errors popping up that
      // would just lead to unique constraint errors.
      val existingTestStepReport = exec(PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).filter(_.testStepId === step.getStepId).result.headOption)
      if (existingTestStepReport.isEmpty) {
        step.getReport.setId(step.getStepId)

        val sessionFolder = repositoryUtils.getPathForTestSession(sessionId, isExpected = false).path
        Files.createDirectories(sessionFolder)

        val testStepReportPath = step.getStepId + ".xml"
        savedPath = Some(testStepReportPath)

        // Write the report into a file
        if (step.getReport != null) {
          step.getReport match {
            case tar: TAR =>
              decoupleLargeData(tar.getContext, sessionFolder)
            case _ =>
          }
          val file = new File(sessionFolder.toFile, testStepReportPath)
          file.createNewFile()
          Using(new FileOutputStream(file)) { stream =>
            stream.write(XMLUtils.marshalToString(new ObjectFactory().createUpdateStatusRequest(step)).getBytes)
          }
        }
        // Save the path of the report file to the DB
        val result = TestStepResult(sessionId, step.getStepId, step.getStatus.ordinal().toShort, testStepReportPath)
        exec((PersistenceSchema.testStepReports += result).transactionally)
        // Flush the current log messages
        testResultManager.flushSessionLogs(sessionId, Some(sessionFolder.toFile))
      }
    }
    savedPath
  }

  def getTestStepResults(sessionId: String): List[TestStepResult] = {
    exec(PersistenceSchema.testStepReports.filter(_.testSessionId === sessionId).result.map(_.toList))
  }

  def generateTestStepXmlReport(xmlFile: Path, xmlReport: Path): Path = {
    val fis = Files.newInputStream(xmlFile)
    val fos = Files.newOutputStream(xmlReport)
    try {
      ReportGenerator.getInstance().writeTestStepStatusXmlReport(fis, fos, false)
      fos.flush()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Unable to generate XML report", e)
    } finally {
      if (fis != null) fis.close()
      if (fos != null) fos.close()
    }
    xmlReport
  }

  def generateTestStepReport(xmlFile: Path, pdfReport: Path): Path = {
    val fis = Files.newInputStream(xmlFile)
    val fos = Files.newOutputStream(pdfReport)
    try {
      ReportGenerator.getInstance().writeTestStepStatusReport(fis, "Test step report", fos, reportHelper.createReportSpecs())
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

  private def getSampleConformanceStatement(index: Int, labels: Map[Short, CommunityLabels]): ConformanceStatementFull = {
    new ConformanceStatementFull(
      0L, "Sample Community",
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation),
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.System),
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain), "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain),
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor), "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor),
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification), "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification),
      Some("Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup)), Some("Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup)),
      "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationInGroup), "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationInGroup),
      Some(index), Some("Sample Test Suite "+index), Some("Description for Sample Test Suite "+index), Some("Sample Test Case "+index), Some("Description for Sample Test Case "+index), Some(false), Some(false), None,  "SUCCESS", Some("An output message for the test session"),
      None, None, 0L, 0L, 0L, 0L, 0L, 0L)
  }

  def generateDemoConformanceCertificate(reportPath: Path, settings: ConformanceCertificates, communityId: Long): Path = {
    val conformanceInfo = new ListBuffer[ConformanceStatementFull]
    val labels = communityLabelManager.getLabels(communityId)

    conformanceInfo += getSampleConformanceStatement(1, labels)
    conformanceInfo += getSampleConformanceStatement(2, labels)
    conformanceInfo += getSampleConformanceStatement(3, labels)
    conformanceInfo += getSampleConformanceStatement(4, labels)
    conformanceInfo += getSampleConformanceStatement(5, labels)
    generateConformanceCertificate(reportPath, settings, conformanceInfo.toList, communityId)
  }

  def generateConformanceCertificate(reportPath: Path, settings: ConformanceCertificates, actorId: Long, systemId: Long, communityId: Long): Path = {
    val conformanceInfo = conformanceManager.getConformanceStatementsFull(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, None, None, None)
    generateConformanceCertificate(reportPath, settings, conformanceInfo, communityId)
  }

  private def generateConformanceCertificate(reportPath: Path, settings: ConformanceCertificates, conformanceInfo: List[ConformanceStatementFull], communityId: Long): Path = {
    var pathToUseForPdf = reportPath
    if (settings.includeSignature) {
      pathToUseForPdf = reportPath.resolveSibling(reportPath.getFileName.toString + ".signed.pdf")
    }
    var title = "Conformance Certificate"
    if (settings.title.isDefined && !StringUtils.isBlank(settings.title.get)) {
      title = settings.title.get.trim
    }
    val labels = communityLabelManager.getLabels(settings.community)
    generateCoreConformanceReport(pathToUseForPdf, addTestCases = false, title, addDetails = settings.includeDetails, addTestCaseResults = settings.includeTestCases, addTestStatus = settings.includeTestStatus, addMessage = settings.includeMessage, settings.message, conformanceInfo, labels, communityId)
    // Add signature is needed.
    if (settings.includeSignature) {
      val keystore = SigUtils.loadKeystore(
        Base64.decodeBase64(MimeUtil.getBase64FromDataURL(settings.keystoreFile.get)),
        settings.keystoreType.get,
        settings.keystorePassword.get.toCharArray
      )
      val signer = new CreateSignature(keystore, settings.keyPassword.get.toCharArray)
      val fis = Files.newInputStream(pathToUseForPdf)
      val fos = Files.newOutputStream(reportPath)
      try {
        var tsaUrl: String = null
        if (Configurations.TSA_SERVER_ENABLED) {
          tsaUrl = Configurations.TSA_SERVER_URL
        }
        signer.signDetached(fis, fos, tsaUrl)
        fos.flush()
      } catch {
        case e: Exception =>
          throw new IllegalStateException("Unable to generate signed PDF report", e)
      } finally {
        if (fos != null) fos.close()
        if (fis != null) {
          fis.close()
          FileUtils.deleteQuietly(pathToUseForPdf.toFile)
        }
      }
    }
    reportPath
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

  def generateConformanceStatementReport(reportPath: Path, addTestCases: Boolean, actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long): Path = {
    generateCoreConformanceReport(reportPath, addTestCases, None, actorId, systemId, labels, communityId)
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, message: Option[String], actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long): Path = {
    val conformanceInfo = conformanceManager.getConformanceStatementsFull(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, None, None, None)
    generateCoreConformanceReport(reportPath, addTestCases, "Conformance Statement Report", addDetails = true, addTestCaseResults = true, addTestStatus = true, addMessage = false, message, conformanceInfo, labels, communityId)
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, title: String, addDetails: Boolean, addTestCaseResults: Boolean, addTestStatus: Boolean, addMessage: Boolean, message: Option[String], conformanceInfo: List[ConformanceStatementFull], labels: Map[Short, CommunityLabels], communityId: Long): Path = {
    val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    val overview = new ConformanceStatementOverview()
    val specs = reportHelper.createReportSpecs(Some(communityId))
    // Labels
    overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
    overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
    overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
    overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
    overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))

    val conformanceData = conformanceInfo.head
    overview.setIncludeTestCases(addTestCases)
    overview.setTitle(title)
    overview.setTestDomain(conformanceData.domainNameFull)
    overview.setTestSpecification(conformanceData.specificationNameFull)
    overview.setTestActor(conformanceData.actorFull)
    overview.setOrganisation(conformanceData.organizationName)
    overview.setSystem(conformanceData.systemName)
    overview.setIncludeDetails(addDetails)
    overview.setIncludeMessage(addMessage)

    // Prepare message
    var messageToUse:String  = null
    if (addMessage && message.isDefined) {
      // Replace placeholders
      messageToUse = message.get.replace(Constants.PlaceholderActor, overview.getTestActor)
      messageToUse = messageToUse.replace(Constants.PlaceholderDomain, overview.getTestDomain)
      messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation, overview.getOrganisation)
      messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroupOption, conformanceData.specificationGroupOptionNameFull)
      messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroup, conformanceData.specificationGroupNameFull.getOrElse(""))
      messageToUse = messageToUse.replace(Constants.PlaceholderSpecification, overview.getTestSpecification)
      messageToUse = messageToUse.replace(Constants.PlaceholderSystem, overview.getSystem)
      overview.setMessage(messageToUse)
    }

    if (addTestCaseResults) {
      overview.setTestSuites(new util.ArrayList[TestSuiteOverview]())
    }
    var failedTests = 0
    var completedTests = 0
    var undefinedTests = 0
    var totalTests = 0
    var index = 1
    val testMap = new mutable.TreeMap[Long, (TestSuiteOverview, Counters)]
    conformanceInfo.foreach { info =>
      val result = TestResultStatus.withName(info.result)
      if (!info.testCaseDisabled.get && !info.testCaseOptional.get) {
        totalTests += 1
        if (result == TestResultStatus.SUCCESS) {
          completedTests += 1
        } else if (result == TestResultStatus.FAILURE) {
          failedTests += 1
        } else {
          undefinedTests += 1
        }
      }
      if (addTestCaseResults) {
        val testCaseOverview = new TestCaseOverview()
        testCaseOverview.setId(index.toString)
        testCaseOverview.setTestName(info.testCaseName.get)
        if (info.testCaseDescription.isDefined) {
          testCaseOverview.setTestDescription(info.testCaseDescription.get)
        } else {
          testCaseOverview.setTestDescription("-")
        }
        testCaseOverview.setReportResult(info.result)
        testCaseOverview.setOutputMessage(info.outputMessage.orNull)
        testCaseOverview.setOptional(info.testCaseOptional.get)
        testCaseOverview.setDisabled(info.testCaseDisabled.get)
        if (info.testCaseTags.isDefined) {
          val parsedTags = JsonUtil.parseJsTags(info.testCaseTags.get).map(x => new TestCaseOverview.Tag(x.name, x.foreground.getOrElse("#777777"), x.background.getOrElse("#FFFFFF")))
          testCaseOverview.setTags(new util.ArrayList(parsedTags.asJavaCollection))
        }
        if (addTestCases) {
          testCaseOverview.setTitle("Test Case Report #" + index)
          testCaseOverview.setOrganisation(info.organizationName)
          testCaseOverview.setSystem(info.systemName)
          testCaseOverview.setTestDomain(info.domainNameFull)
          testCaseOverview.setTestSpecification(info.specificationNameFull)
          testCaseOverview.setTestActor(info.actorFull)
          if (info.sessionId.isDefined) {
            val testResult = testResultManager.getTestResultForSessionWrapper(info.sessionId.get)
            testCaseOverview.setStartTime(sdf.format(new Date(testResult.get._1.startTime.getTime)))
            if (testResult.get._1.endTime.isDefined) {
              testCaseOverview.setEndTime(sdf.format(new Date(testResult.get._1.endTime.get.getTime)))
            }
            val testcasePresentation = XMLUtils.unmarshal(classOf[TestCase], new StreamSource(new StringReader(testResult.get._2)))
            val sessionFolderInfo = repositoryUtils.getPathForTestSessionObj(info.sessionId.get, Some(testResult.get._1.startTime), isExpected = true)
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
          val testSuite = new TestSuiteOverview
          testSuite.setTestSuiteName(info.testSuiteName.get)
          testSuite.setTestSuiteDescription(info.testSuiteDescription.orNull)
          testSuite.setOverallStatus("UNDEFINED")
          testSuite.setTestCases(new util.ArrayList[TestCaseOverview]())
          (testSuite, new Counters(0, 0, 0))
        })
        // Update the test suite's results.
        if (!info.testCaseDisabled.get && !info.testCaseOptional.get) {
          if (result == TestResultStatus.SUCCESS) {
            testSuiteOverview._2.successes += 1
          } else if (result == TestResultStatus.FAILURE) {
            testSuiteOverview._2.failures += 1
          } else {
            testSuiteOverview._2.other += 1
          }
        }
        testSuiteOverview._1.getTestCases.add(testCaseOverview)
        index += 1
      }
    }
    overview.setOverallStatus("UNDEFINED")
    if (failedTests > 0) {
      overview.setOverallStatus("FAILURE")
    } else if (undefinedTests > 0) {
      overview.setOverallStatus("UNDEFINED")
    } else if (completedTests > 0) {
      overview.setOverallStatus("SUCCESS")
    }
    overview.setCompletedTests(completedTests)
    overview.setFailedTests(failedTests)
    overview.setUndefinedTests(undefinedTests)
    if (testMap.nonEmpty) {
      // Set the status of the collected test suites
      testMap.values.foreach { testSuiteInfo =>
        if (testSuiteInfo._2.failures > 0) {
          testSuiteInfo._1.setOverallStatus("FAILURE")
        } else if (testSuiteInfo._2.other > 0) {
          testSuiteInfo._1.setOverallStatus("UNDEFINED")
        } else if (testSuiteInfo._2.successes > 0) {
          testSuiteInfo._1.setOverallStatus("SUCCESS")
        } else {
          testSuiteInfo._1.setOverallStatus("UNDEFINED")
        }
      }
      // Add to overall output
      overview.setTestSuites(new util.ArrayList[TestSuiteOverview](testMap.values.map(_._1).toList.asJavaCollection))
    }
    overview.setIncludeTestStatus(addTestStatus)
    Files.createDirectories(reportPath.getParent)
    val fos = Files.newOutputStream(reportPath)
    try {
      ReportGenerator.getInstance().writeConformanceStatementOverviewReport(overview, fos, specs)
      fos.flush()
    } catch {
      case e: Exception =>
        throw new IllegalStateException("Unable to generate PDF report", e)
    } finally {
      if (fos != null) fos.close()
    }
    reportPath
  }

}

private class Counters(var successes: Int, var failures: Int, var other: Int)
