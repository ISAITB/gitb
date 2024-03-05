package managers

import actors.events.{ConformanceStatementSucceededEvent, TestSessionFailedEvent, TestSessionSucceededEvent}
import com.gitb.core.StepStatus
import com.gitb.reports.ReportGenerator
import com.gitb.reports.dto._
import com.gitb.tbs.{ObjectFactory, TestStepStatus}
import com.gitb.tpl.TestCase
import com.gitb.tr._
import com.gitb.utils.XMLUtils
import config.Configurations
import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType
import models.Enums.OverviewLevelType.OverviewLevelType
import models.Enums.{ConformanceStatementItemType, OverviewLevelType, TestResultStatus}
import models._
import models.statement.{BadgeListPlaceholderInfo, BadgePlaceholderInfo, ConformanceData, ConformanceDataLocator, ConformanceItemTreeData, ResultCountHolder}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils._
import utils.signature.{CreateSignature, SigUtils}

import java.io.{File, FileOutputStream, StringReader}
import java.nio.file.{Files, Path}
import java.text.SimpleDateFormat
import java.util
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.{Calendar, Date}
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
class ReportManager @Inject() (communityManager: CommunityManager, organizationManager: OrganizationManager, systemManager: SystemManager, domainParameterManager: DomainParameterManager, reportHelper: ReportHelper, triggerHelper: TriggerHelper, testCaseReportProducer: TestCaseReportProducer, testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, dbConfigProvider: DatabaseConfigProvider, communityLabelManager: CommunityLabelManager, repositoryUtils: RepositoryUtils, testResultManager: TestResultManager) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val PLACEHOLDER_DOMAIN_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderDomain+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_GROUP_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecificationGroup+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_GROUP_OPTION_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecificationGroupOption+"\\{(\\d+)\\})")
  private val PLACEHOLDER_SPECIFICATION_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderSpecification+"\\{(\\d+)\\})")
  private val PLACEHOLDER_ACTOR_WITH_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderActor+"\\{(\\d+)\\})")
  private val PLACEHOLDER_BADGE_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadge+"(?:\\{(\\d+)(?:\\|(\\d+))?\\})?)")
  private val PLACEHOLDER_BADGE_WITHOUT_INDEX_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadge+"(?:\\{(\\d+)\\})?)")
  private val PLACEHOLDER_BADGE_LIST_REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadges+"\\{((?:horizontal)|(?:vertical))(?:\\|(\\d+))?\\})")

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
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      startTime <- PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(_.startTime).result.headOption
      _ <- {
        if (startTime.isDefined) {
          // Test session finalisation and cleanup actions.
          for {
            _ <- PersistenceSchema.testResults
              .filter(_.testSessionId === sessionId)
              .map(x => (x.result, x.endTime, x.outputMessage))
              .update(status.value(), now, outputMessage)
            // Delete any pending test interactions
            _ <- testResultManager.deleteTestInteractions(sessionId, None)
            // Update also the conformance results for the system
            _ <- PersistenceSchema.conformanceResults
              .filter(_.testsession === sessionId)
              .map(x => (x.result, x.outputMessage, x.updateTime))
              .update(status.value(), outputMessage, now)
            // Delete temporary test session data (used for user interactions).
            _ <- {
              onSuccessCalls += (() => {
                val sessionFolderInfo = repositoryUtils.getPathForTestSessionObj(sessionId, startTime, isExpected = true)
                val tempDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = true)
                FileUtils.deleteQuietly(tempDataFolder.toFile)
              })
              DBIO.successful(())
            }
          } yield ()
        } else {
          // The test session was not recorded - nothing to do.
          DBIO.successful(())
        }
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
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
              repositoryUtils.decoupleLargeData(tar.getContext, sessionFolder, isTempData = false)
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

  private def getSampleConformanceStatement(addPrefixes: Boolean, testSuiteIndex: Int, testCaseIndex: Int, labels: Map[Short, CommunityLabels], domainId: Long, groupId: Long, specificationId: Long, actorId: Long): ConformanceStatementFull = {
    val domainName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain, single = true, lowercase = true) + (if (addPrefixes) " "+domainId else "")
    val groupName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup, single = true, lowercase = true) + (if (addPrefixes) " "+groupId else "")
    val specificationName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification, single = true, lowercase = true) + (if (addPrefixes) " "+specificationId else "")
    val actorName = "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor, single = true, lowercase = true) + (if (addPrefixes) " "+actorId else "")
    new ConformanceStatementFull(
      0L, "Sample community",
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation, single = true, lowercase = true),
      0L, "Sample " + communityLabelManager.getLabel(labels, models.Enums.LabelType.System, single = true, lowercase = true), "",
      domainId, domainName, domainName,
      actorId, actorName, actorName, "",
      specificationId, specificationName, specificationName, 0,
      Some(groupId), Some(groupName), Some(groupName), Some(0),
      specificationName, specificationName,
      Some(testSuiteIndex), Some("Sample test suite "+testSuiteIndex), Some("Description for Sample test suite "+testSuiteIndex), None, None, None,
      Some(testCaseIndex), Some("Sample test case "+testCaseIndex), Some("Description for Sample test case "+testCaseIndex), Some(false), Some(false), None,  None, None, None, None,
      "SUCCESS", Some("An output message for the test session"),
      None, None, 0L, 0L, 0L, 0L, 0L, 0L)
  }

  def generateDemoConformanceCertificate(reportPath: Path, settings: ConformanceCertificateInfo, communityId: Long): Path = {
    val conformanceInfo = new ListBuffer[ConformanceStatementFull]
    val labels = communityLabelManager.getLabels(communityId)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 1, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 2, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 3, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 4, labels, 0L, 0L, 0L, 0L)
    conformanceInfo += getSampleConformanceStatement(addPrefixes = false, 1, 5, labels, 0L, 0L, 0L, 0L)
    generateConformanceCertificate(reportPath, settings, conformanceInfo.toList, communityId, None, isDemo = true)
  }

  def generateDemoConformanceOverviewCertificate(reportPath: Path, settings: ConformanceCertificateInfo, communityId: Long, level: OverviewLevelType): Path = {
    val labels = communityLabelManager.getLabels(communityId)
    // Generate demo data
    val builder = new ConformanceStatusBuilder[ConformanceStatementFull](true)
    var actorIdsToDisplay: Option[Set[Long]] = None
    if (level == OverviewLevelType.SpecificationLevel) {
      // Actor 1
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      // Actor 2
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 2L), isOptional = false, isDisabled = false)
      actorIdsToDisplay = Some(Set(1L, 2L))
    } else {
      // Specification 1
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 1, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 2, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 1, 3, labels, 1L, 1L, 1L, 1L), isOptional = false, isDisabled = false)
      // Specification 2
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 4, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 5, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 2, 6, labels, 1L, 1L, 2L, 2L), isOptional = false, isDisabled = false)
      // Specification 3
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 7, labels, 1L, 1L, 3L, 3L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 8, labels, 1L, 1L, 3L, 3L), isOptional = false, isDisabled = false)
      builder.addConformanceResult(getSampleConformanceStatement(addPrefixes = true, 3, 9, labels, 1L, 1L, 3L, 3L), isOptional = false, isDisabled = false)
      actorIdsToDisplay = Some(Set.empty)
    }
    // Construct the report data
    val conformanceData = getConformanceDataForOverviewReport(builder, level, communityId, actorIdsToDisplay)
    // Create the report
    generateConformanceOverviewReport(conformanceData, Some(settings), reportPath, labels, communityId, isDemo = true, None)
  }

  def generateConformanceCertificate(reportPath: Path, settings: ConformanceCertificateInfo, actorId: Long, systemId: Long, communityId: Long, snapshotId: Option[Long]): Path = {
    val conformanceInfo = conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).getDetails(None)
    generateConformanceCertificate(reportPath, settings, conformanceInfo, communityId, snapshotId: Option[Long], isDemo = false)
  }

  private def generateConformanceCertificate(reportPath: Path, settings: ConformanceCertificateInfo, conformanceInfo: List[ConformanceStatementFull], communityId: Long, snapshotId: Option[Long], isDemo: Boolean): Path = {
    var pathToUseForPdf = if (settings.includeSignature) {
      reportPath.resolveSibling(reportPath.getFileName.toString + ".signed.pdf")
    } else {
      reportPath
    }
    var title: Option[String] = None
    if (settings.includeTitle) {
      if (settings.title.isDefined && !StringUtils.isBlank(settings.title.get)) {
        title = Some(settings.title.get.trim)
      } else {
        title = Some("Conformance Certificate")
      }
    }
    val labels = communityLabelManager.getLabels(settings.community)
    generateCoreConformanceReport(pathToUseForPdf, addTestCases = false, title, addDetails = settings.includeDetails, addTestCaseResults = settings.includeItems, addTestStatus = settings.includeItemStatus,
      addMessage = settings.includeMessage, addPageNumbers = settings.includePageNumbers, settings.message,
      conformanceInfo, labels, communityId, snapshotId, isDemo
    )
    // Add signature if needed.
    if (settings.includeSignature && settings.keystore.isDefined) {
      pathToUseForPdf = signReport(settings.keystore.get, pathToUseForPdf, reportPath)
    }
    pathToUseForPdf
  }

  private def signReport(communityKeystore: CommunityKeystore, tempPdfPath: Path, finalPdfPath: Path): Path = {
    val keystore = SigUtils.loadKeystore(
      Base64.decodeBase64(MimeUtil.getBase64FromDataURL(communityKeystore.keystoreFile)),
      communityKeystore.keystoreType,
      communityKeystore.keystorePassword.toCharArray
    )
    val signer = new CreateSignature(keystore, communityKeystore.keyPassword.toCharArray)
    try {
      Using.Manager { use =>
        val input = use(Files.newInputStream(tempPdfPath))
        val output = use(Files.newOutputStream(finalPdfPath))
        var tsaUrl: String = null
        if (Configurations.TSA_SERVER_ENABLED) {
          tsaUrl = Configurations.TSA_SERVER_URL
        }
        signer.signDetached(input, output, tsaUrl)
        output.flush()
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

  private def getConformanceDataForOverviewReport(conformanceInfoBuilder: ConformanceStatusBuilder[ConformanceStatementFull], reportLevel: OverviewLevelType, communityId: Long, actorIdsToDisplay: Option[Set[Long]]): ConformanceData = {
    // The overview is a list of aggregated conformance statements.
    val conformanceOverview = conformanceInfoBuilder.getOverview(None)
    // The details is a list of the detailed conformance results (at test case level).
    val conformanceDetails = conformanceInfoBuilder.getDetails(None)
    // Map actor IDs to test suites.
    val actorTestSuiteMap = new mutable.LinkedHashMap[Long, mutable.LinkedHashMap[Long, ConformanceTestSuite]]() // Actor ID to test suite map, test suite map
    conformanceDetails.foreach { statement =>
      var actorTestSuites = actorTestSuiteMap.get(statement.actorId)
      if (actorTestSuites.isEmpty) {
        actorTestSuites = Some(new mutable.LinkedHashMap[Long, ConformanceTestSuite])
        actorTestSuiteMap += (statement.actorId -> actorTestSuites.get)
      }
      var testSuite = actorTestSuites.get.get(statement.testSuiteId.get)
      if (testSuite.isEmpty) {
        testSuite = Some(new ConformanceTestSuite(
          statement.testSuiteId.get, statement.testSuiteName.get, statement.testSuiteDescription, false, statement.testSuiteSpecReference, statement.testSuiteSpecDescription, statement.testSuiteSpecLink,
          TestResultType.UNDEFINED, 0, 0, 0, 0, 0, 0, new ListBuffer[ConformanceTestCase]
        ))
        actorTestSuites.get += (testSuite.get.id -> testSuite.get)
      }
      val testCase = new ConformanceTestCase(
        statement.testCaseId.get, statement.testCaseName.get, statement.testCaseDescription, None, statement.updateTime, None, false,
        statement.testCaseOptional.get, statement.testCaseDisabled.get, TestResultType.fromValue(statement.result), statement.testCaseTags,
        statement.testCaseSpecReference, statement.testCaseSpecDescription, statement.testCaseSpecLink
      )
      testSuite.get.testCases.asInstanceOf[ListBuffer[ConformanceTestCase]] += testCase
      if (!testCase.disabled) {
        if (testCase.result == TestResultType.SUCCESS) {
          if (testCase.optional) {
            testSuite.get.completedOptional += 1
          } else {
            testSuite.get.completed += 1
          }
        } else if (testCase.result == TestResultType.FAILURE) {
          if (testCase.optional) {
            testSuite.get.failedOptional += 1
          } else {
            testSuite.get.failed += 1
          }
        } else {
          if (testCase.optional) {
            testSuite.get.undefinedOptional += 1
          } else {
            testSuite.get.undefined += 1
          }
        }
      }
    }
    actorTestSuiteMap.values.foreach { testSuites =>
      testSuites.values.foreach { testSuite =>
        testSuite.result = TestResultType.fromValue(testSuite.resultStatus())
      }
    }
    val conformanceItemTree = conformanceManager.createConformanceItemTree(ConformanceItemTreeData(conformanceOverview, actorIdsToDisplay), withResults = true, testSuiteMapper = Some((statement: ConformanceStatement) => {
      if (actorTestSuiteMap.contains(statement.actorId)) {
        actorTestSuiteMap(statement.actorId).values.toList
      } else {
        List.empty
      }
    }))
    // Check to see if only one domain can ever apply for the community (in which case it should be hidden).
    val displayDomainInStatementTree = communityManager.getCommunityDomain(communityId).isEmpty
    // Construct the DTOs expected by the report template.
    val conformanceItems = toConformanceItems(conformanceItemTree, None, new ReportData(!displayDomainInStatementTree))
    // Return results.
    ConformanceData(
      reportLevel,
      conformanceOverview.headOption.map(_.domainNameFull),
      conformanceOverview.headOption.flatMap(_.specificationGroupNameFull),
      conformanceOverview.headOption.map(_.specificationGroupOptionNameFull),
      conformanceOverview.headOption.map(_.organizationId),
      conformanceOverview.headOption.map(_.organizationName),
      conformanceOverview.headOption.map(_.systemId),
      conformanceOverview.headOption.map(_.systemName),
      displayDomainInStatementTree,
      getOverallConformanceOverviewStatus(conformanceItems),
      conformanceItems,
      conformanceItemTree,
      conformanceOverview
    )
  }

  private def getConformanceDataForOverviewReport(systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], snapshotId: Option[Long], communityId: Long): ConformanceData = {
    val reportLevel = if (domainId.isDefined) {
      OverviewLevelType.DomainLevel
    } else if (groupId.isDefined) {
      OverviewLevelType.SpecificationGroupLevel
    } else if (specificationId.isDefined) {
      OverviewLevelType.SpecificationLevel
    } else {
      OverviewLevelType.OrganisationLevel
    }
    // Load conformance data.
    val conformanceInfoBuilder = conformanceManager.getConformanceStatementsResultBuilder(domainId.map(List(_)), specificationId.map(List(_)), groupId.map(List(_)), None, None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false)
    // Check to see if only one domain can ever apply for the system (in which case it should be hidden).
    getConformanceDataForOverviewReport(conformanceInfoBuilder, reportLevel, communityId, None)
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

  def generateConformanceOverviewCertificate(reportPath: Path, settings: Option[ConformanceCertificateInfo], systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], communityId: Long, snapshotId: Option[Long]): Path = {
    val labels = communityLabelManager.getLabels(communityId)
    val conformanceData = getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
    generateConformanceOverviewReport(conformanceData, settings, reportPath, labels, communityId, isDemo = false, snapshotId)
  }

  def generateConformanceOverviewReport(reportPath: Path, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long]): Path = {
    val conformanceData = getConformanceDataForOverviewReport(systemId, domainId, groupId, specificationId, snapshotId, communityId)
    generateConformanceOverviewReport(conformanceData, None, reportPath, labels, communityId, isDemo = false, snapshotId)
  }

  private def generateConformanceOverviewReport(conformanceData: ConformanceData, settings: Option[ConformanceCertificateInfo], reportPath: Path, labels: Map[Short, CommunityLabels], communityId: Long, isDemo: Boolean, snapshotId: Option[Long]): Path = {
    var pathToUseForPdf = if (settings.isDefined && settings.get.includeSignature) {
      reportPath.resolveSibling(reportPath.getFileName.toString + ".signed.pdf")
    } else {
      reportPath
    }
    val overview = new ConformanceOverview()
    val specs = reportHelper.createReportSpecs(Some(communityId))
    // Labels
    overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
    overview.setLabelSpecificationGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup))
    overview.setLabelSpecificationInGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationInGroup))
    overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
    overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
    overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
    overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))
    if (conformanceData.reportLevel != OverviewLevelType.OrganisationLevel) {
      overview.setTestDomain(conformanceData.domainName.orNull)
      if (conformanceData.reportLevel == OverviewLevelType.SpecificationGroupLevel) {
        overview.setTestSpecificationGroup(conformanceData.groupName.orNull)
      } else if (conformanceData.reportLevel == OverviewLevelType.SpecificationLevel) {
        overview.setTestSpecificationGroup(conformanceData.groupName.orNull)
        overview.setTestSpecification(conformanceData.specificationName.orNull)
      }
    }
    overview.setTitle("Conformance Overview Report")
    overview.setReportDate(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime))
    overview.setOrganisation(conformanceData.organisationName.getOrElse("-"))
    overview.setSystem(conformanceData.systemName.getOrElse("-"))
    if (settings.isDefined) {
      overview.setIncludeMessage(settings.get.includeMessage && settings.get.message.isDefined)
      overview.setIncludeDetails(settings.get.includeDetails)
      overview.setIncludeConformanceItems(settings.get.includeItems)
      overview.setIncludeTestCases(settings.get.includeItemDetails)
      overview.setIncludePageNumbers(settings.get.includePageNumbers)
      overview.setIncludeTestStatus(settings.get.includeItemStatus)
      if (overview.getIncludeMessage) {
        val dataLocator = conformanceData.createLocator()
        // Replace message placeholders
        var messageToUse = settings.get.message.get
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_DOMAIN_WITH_INDEX_REGEXP, ConformanceStatementItemType.DOMAIN) // Before we look for domain parameters
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_GROUP_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION_GROUP)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_GROUP_OPTION_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_SPECIFICATION_WITH_INDEX_REGEXP, ConformanceStatementItemType.SPECIFICATION)
        messageToUse = replacePlaceholdersByIndex(messageToUse, dataLocator, PLACEHOLDER_ACTOR_WITH_INDEX_REGEXP, ConformanceStatementItemType.ACTOR)
        messageToUse = replaceDomainParameters(messageToUse, communityId, snapshotId)
        messageToUse = replaceOrganisationPropertyPlaceholders(messageToUse, isDemo, communityId, conformanceData.organisationId, snapshotId)
        messageToUse = replaceSystemPropertyPlaceholders(messageToUse, isDemo, communityId, conformanceData.systemId, snapshotId)
        messageToUse = replaceSimplePlaceholders(messageToUse, None, conformanceData.specificationName, conformanceData.specificationName, conformanceData.groupName, conformanceData.domainName, conformanceData.organisationName, conformanceData.systemName)
        messageToUse = replaceBadgeListPlaceholders(messageToUse, isDemo, conformanceData, snapshotId)
        messageToUse = replaceBadgePlaceholdersByIndex(messageToUse, isDemo, conformanceData, snapshotId)
        overview.setMessage(messageToUse)
      }
    }
    overview.setConformanceItems(conformanceData.conformanceItems)
    overview.setOverallStatus(conformanceData.overallResult)
    // Create PDF.
    Files.createDirectories(pathToUseForPdf.getParent)
    Using(Files.newOutputStream(pathToUseForPdf)) { fos =>
      ReportGenerator.getInstance().writeConformanceOverviewReport(overview, fos, specs)
      fos.flush()
    }
    // Add signature if needed.
    if (settings.isDefined) {
      if (settings.get.includeSignature && settings.get.keystore.isDefined) {
        pathToUseForPdf = signReport(settings.get.keystore.get, pathToUseForPdf, reportPath)
      }
    }
    pathToUseForPdf
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

  private def replaceDomainParameters(message: String, communityId: Long, snapshotId: Option[Long]): String = {
    var messageToUse = message
    if (messageToUse.contains(Constants.PlaceholderDomain+"{")) {
      // We are referring to domain parameters.
      if (snapshotId.isEmpty) {
        domainParameterManager.getDomainParametersByCommunityId(communityId, onlySimple = true, loadValues = true).foreach { param =>
          messageToUse = messageToUse.replace(Constants.PlaceholderDomain+"{"+param.name+"}", param.value.getOrElse(""))
        }
      } else {
        conformanceManager.getSnapshotDomainParameters(snapshotId.get).foreach { param =>
          messageToUse = messageToUse.replace(Constants.PlaceholderDomain+"{"+param.paramKey+"}", param.paramValue)
        }
      }
    }
    messageToUse
  }

  private def replaceOrganisationPropertyPlaceholders(message: String, isDemo: Boolean, communityId: Long, organisationId: Option[Long], snapshotId: Option[Long]): String = {
    var messageToUse = message
    if (messageToUse.contains(Constants.PlaceholderOrganisation+"{")) {
      // We are referring to organisation parameters.
      if (isDemo) {
        communityManager.getOrganisationParameters(communityId, Some(true)).foreach { param =>
          messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.testKey+"}", param.testKey)
        }
      } else if (organisationId.isDefined) {
        if (snapshotId.isEmpty) {
          organizationManager.getOrganisationParameterValues(organisationId.get, Some(true)).foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.parameter.testKey+"}", param.value.map(_.value).getOrElse(""))
          }
        } else {
          conformanceManager.getSnapshotOrganisationProperties(snapshotId.get, organisationId.get).foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation+"{"+param.propertyKey+"}", param.propertyValue)
          }
        }
      }
    }
    messageToUse
  }

  private def replaceSystemPropertyPlaceholders(message: String, isDemo: Boolean, communityId: Long, systemId: Option[Long], snapshotId: Option[Long]): String = {
    var messageToUse = message
    if (messageToUse.contains(Constants.PlaceholderSystem+"{")) {
      // We are referring to system parameters.
      if (isDemo) {
        communityManager.getSystemParameters(communityId, Some(true)).foreach { param =>
          messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.testKey+"}", param.testKey)
        }
      } else if (systemId.isDefined) {
        if (snapshotId.isEmpty) {
          systemManager.getSystemParameterValues(systemId.get, Some(true)).foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.parameter.testKey+"}", param.value.map(_.value).getOrElse(""))
          }
        } else {
          conformanceManager.getSnapshotSystemProperties(snapshotId.get, systemId.get).foreach { param =>
            messageToUse = messageToUse.replace(Constants.PlaceholderSystem+"{"+param.propertyKey+"}", param.propertyValue)
          }
        }
      }
    }
    messageToUse
  }

  private def replaceSimplePlaceholders(message: String, actor: Option[String], specification: Option[String], option: Option[String], group: Option[String], domain: Option[String], organisation: Option[String], system: Option[String]): String = {
    var messageToUse = message
    if (actor.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderActor, actor.get)
    if (domain.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderDomain, domain.get)
    if (organisation.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderOrganisation, organisation.get)
    if (option.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroupOption, option.get)
    if (group.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecificationGroup, group.get)
    if (specification.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSpecification, specification.get)
    if (system.isDefined) messageToUse = messageToUse.replace(Constants.PlaceholderSystem, system.get)
    messageToUse
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

  private def replaceBadgePlaceholder(message: String, placeholderInfo: BadgePlaceholderInfo, isDemo: Boolean, specificationId: Option[Long], actorId: Option[Long], snapshotId: Option[Long], status: String, useActualInDemo: Boolean): String = {
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
      imagePath = badge.map(_.toURI.toString)
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

  private def replaceBadgeListPlaceholders(message: String, isDemo: Boolean, data: ConformanceData, snapshotId: Option[Long]): String = {
    var messageToUse = message
    val placeholders = getBadgeListPlaceholders(message)
    val placeholdersToProcess = new ListBuffer[(BadgePlaceholderInfo, ConformanceStatement)]
    if (data.statements.nonEmpty) {
      placeholders.foreach { placeholder =>
        // Additions: 1: before all, 2: before each, 3: after each, 4: after all
        val additions = if (placeholder.horizontal) {
          ("<tr>", "", "", "</tr>")
        } else {
          ("", "<tr>", "</tr>", "")
        }
        val placeholderTable = new StringBuilder()
        placeholderTable.append("<table>")
        placeholderTable.append(additions._1)
        var index = 0
        data.statements.foreach { statement =>
          val placeholderText = Constants.PlaceholderBadge+"{"+index+placeholder.width.map("|"+_).getOrElse("")+"}"
          placeholderTable
            .append(additions._2)
            .append("<td>")
            .append(placeholderText)
            .append("</td>")
            .append(additions._3)
          placeholdersToProcess.addOne((BadgePlaceholderInfo(placeholderText, Some(index.toShort), placeholder.width), statement))
          index += 1
        }
        placeholderTable.append(additions._4)
        placeholderTable.append("</table>")
        messageToUse = messageToUse.replace(placeholder.placeholder, placeholderTable)
      }
    }
    placeholdersToProcess.foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo._1, isDemo, Some(placeholderInfo._2.specificationId), Some(placeholderInfo._2.actorId), snapshotId, placeholderInfo._2.result, useActualInDemo = false)
    }
    messageToUse
  }

  private def replaceBadgePlaceholdersByIndex(message: String, isDemo: Boolean, data: ConformanceData, snapshotId: Option[Long]): String = {
    var messageToUse = message
    getBadgePlaceholders(message, indexedBadges = true).filter(_.index.isDefined).foreach { indexedPlaceholder =>
      // Get the statement for the given index.
      val statement = data.statements.lift(indexedPlaceholder.index.get)
      if (statement.isDefined) {
        messageToUse = replaceBadgePlaceholder(messageToUse, indexedPlaceholder, isDemo, Some(statement.get.specificationId), Some(statement.get.actorId), snapshotId, statement.get.result, useActualInDemo = false)
      } else if (isDemo) {
        // A conformance statement at the given index was not found. If this is a demo report then replace it with the demo badge.
        messageToUse = replaceBadgePlaceholder(messageToUse, indexedPlaceholder, isDemo, None, None, snapshotId, data.overallResult, useActualInDemo = false)
      }
    }
    messageToUse
  }

  private def replaceBadgePlaceholders(message: String, isDemo: Boolean, specificationId: Option[Long], actorId: Option[Long], snapshotId: Option[Long], overallStatus: String): String = {
    var messageToUse = message
    val placeholders = getBadgePlaceholders(message, indexedBadges = false)
    placeholders.filter(p => p.width.isDefined).foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo, isDemo, specificationId, actorId, snapshotId, overallStatus, useActualInDemo = false)
    }
    placeholders.filter(p => p.width.isEmpty).foreach { placeholderInfo =>
      messageToUse = replaceBadgePlaceholder(messageToUse, placeholderInfo, isDemo, specificationId, actorId, snapshotId, overallStatus, useActualInDemo = false)
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
    /*
     * Record names of conformance items before we iterate children. Like this e.g. a spec will always have the name of its domain and group.
     * The only case where we will use these names is when we record the results at leaf level in which case we'll have everything available.
     */
    if (item.itemType == ConformanceStatementItemType.DOMAIN) reportData.domainName = Some(newItem.getName)
    if (item.itemType == ConformanceStatementItemType.SPECIFICATION_GROUP) reportData.groupName = Some(newItem.getName)
    if (item.itemType == ConformanceStatementItemType.SPECIFICATION) reportData.specName = Some(newItem.getName)
    if (item.itemType == ConformanceStatementItemType.ACTOR) reportData.actorName = Some(newItem.getName)
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
      newItem.setOverallStatus(counters.resultStatus())
      newItem.setData(new ConformanceStatementData())
      newItem.getData.setOverallStatus(newItem.getOverallStatus)
      newItem.getData.setCompletedTests(counters.successes.toInt)
      newItem.getData.setFailedTests(counters.failures.toInt)
      newItem.getData.setUndefinedTests(counters.other.toInt)
      newItem.getData.setTestDomain(reportData.domainName.orNull)
      newItem.getData.setTestSpecificationGroup(reportData.groupName.orNull)
      newItem.getData.setTestSpecification(reportData.specName.orNull)
      if (item.itemType == ConformanceStatementItemType.ACTOR && item.actorToShow) {
        newItem.getData.setTestActor(reportData.actorName.orNull)
      }
      if (item.results.get.testSuites.isDefined) {
        newItem.getData.setTestSuites(new util.ArrayList[TestSuiteOverview]())
        item.results.get.testSuites.get.foreach { testSuite =>
          val testSuiteOverview = new TestSuiteOverview()
          newItem.getData.getTestSuites.add(testSuiteOverview)
          testSuiteOverview.setOverallStatus(testSuite.result.value())
          testSuiteOverview.setTestSuiteName(testSuite.name)
          testSuiteOverview.setTestSuiteDescription(testSuite.description.orNull)
          testSuiteOverview.setSpecReference(testSuite.specReference.orNull)
          testSuiteOverview.setSpecLink(testSuite.specLink.orNull)
          testSuiteOverview.setSpecDescription(testSuite.specDescription.orNull)
          testSuiteOverview.setTestCases(new util.ArrayList[TestCaseOverview]())
          testSuite.testCases.foreach { testCase =>
            val testCaseOverview = new TestCaseOverview()
            testSuiteOverview.getTestCases.add(testCaseOverview)
            testCaseOverview.setTestName(testCase.name)
            testCaseOverview.setReportResult(testCase.result.value())
            testCaseOverview.setTestName(testCase.name)
            testCaseOverview.setTestDescription(testCase.description.orNull)
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

  private def parseTestCaseTags(tags: String): util.List[TestCaseOverview.Tag] = {
    val parsedTags = JsonUtil.parseJsTags(tags).map(x => new TestCaseOverview.Tag(x.name, x.description.orNull, x.foreground.getOrElse("#777777"), x.background.getOrElse("#FFFFFF")))
    new util.ArrayList(parsedTags.asJavaCollection)
  }

  def generateConformanceStatementReport(reportPath: Path, addTestCases: Boolean, actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long]): Path = {
    generateCoreConformanceReport(reportPath, addTestCases, None, actorId, systemId, labels, communityId, snapshotId)
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, message: Option[String], actorId: Long, systemId: Long, labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long]): Path = {
    val conformanceInfo = conformanceManager.getConformanceStatementsResultBuilder(None, None, None, Some(List(actorId)), None, None, Some(List(systemId)), None, None, None, None, snapshotId, prefixSpecificationNameWithGroup = false).getDetails(None)
    generateCoreConformanceReport(reportPath, addTestCases, Some("Conformance Statement Report"), addDetails = true, addTestCaseResults = true, addTestStatus = true, addMessage = false, addPageNumbers = true, message, conformanceInfo, labels, communityId, snapshotId, isDemo = false)
  }

  private def generateCoreConformanceReport(reportPath: Path, addTestCases: Boolean, title: Option[String], addDetails: Boolean, addTestCaseResults: Boolean, addTestStatus: Boolean, addMessage: Boolean, addPageNumbers: Boolean, message: Option[String], conformanceInfo: List[ConformanceStatementFull], labels: Map[Short, CommunityLabels], communityId: Long, snapshotId: Option[Long], isDemo: Boolean): Path = {
    val sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    val overview = new ConformanceStatementOverview()
    val specs = reportHelper.createReportSpecs(Some(communityId))
    // Labels
    overview.setLabelDomain(communityLabelManager.getLabel(labels, models.Enums.LabelType.Domain))
    overview.setLabelSpecificationGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationGroup))
    overview.setLabelSpecificationInGroup(communityLabelManager.getLabel(labels, models.Enums.LabelType.SpecificationInGroup))
    overview.setLabelSpecification(communityLabelManager.getLabel(labels, models.Enums.LabelType.Specification))
    overview.setLabelActor(communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor))
    overview.setLabelOrganisation(communityLabelManager.getLabel(labels, models.Enums.LabelType.Organisation))
    overview.setLabelSystem(communityLabelManager.getLabel(labels, models.Enums.LabelType.System))

    val conformanceData = conformanceInfo.head
    overview.setIncludeTestCases(addTestCases)
    overview.setTitle(title.orNull)
    overview.setTestDomain(conformanceData.domainNameFull)
    overview.setTestSpecificationGroup(conformanceData.specificationGroupNameFull.orNull)
    overview.setTestSpecification(conformanceData.specificationNameFull)
    if (isDemo || conformanceManager.getActorIdsToDisplayInStatementsWrapper(List(conformanceData)).contains(conformanceData.actorId)) {
      overview.setTestActor(conformanceData.actorFull)
    }
    overview.setOrganisation(conformanceData.organizationName)
    overview.setSystem(conformanceData.systemName)
    overview.setIncludeDetails(addDetails)
    overview.setIncludeMessage(addMessage)
    overview.setIncludePageNumbers(addPageNumbers)

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
        testCaseOverview.setSpecReference(info.testCaseSpecReference.orNull)
        testCaseOverview.setSpecDescription(info.testCaseSpecDescription.orNull)
        testCaseOverview.setSpecLink(info.testCaseSpecLink.orNull)
        testCaseOverview.setReportResult(info.result)
        testCaseOverview.setOutputMessage(info.outputMessage.orNull)
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
          testSuite.setSpecReference(info.testSuiteSpecReference.orNull)
          testSuite.setSpecDescription(info.testSuiteSpecDescription.orNull)
          testSuite.setSpecLink(info.testSuiteSpecLink.orNull)
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
    // Replace message placeholders
    if (addMessage && message.isDefined) {
      var messageToUse = message.get
      messageToUse = replaceDomainParameters(messageToUse, communityId, snapshotId)
      messageToUse = replaceOrganisationPropertyPlaceholders(messageToUse, isDemo, communityId, Some(conformanceData.organizationId), snapshotId)
      messageToUse = replaceSystemPropertyPlaceholders(messageToUse, isDemo, communityId, Some(conformanceData.systemId), snapshotId)
      messageToUse = replaceSimplePlaceholders(messageToUse, Some(conformanceData.actorFull), Some(conformanceData.specificationNameFull), Some(conformanceData.specificationGroupOptionNameFull), conformanceData.specificationGroupNameFull, Some(conformanceData.domainNameFull), Some(conformanceData.organizationName), Some(conformanceData.systemName))
      messageToUse = replaceBadgePlaceholders(messageToUse, isDemo, Some(conformanceData.specificationId), Some(conformanceData.actorId), snapshotId, overview.getOverallStatus)
      overview.setMessage(messageToUse)
    }
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

private class ReportData(val skipDomain: Boolean, var domainName: Option[String] = None, var groupName: Option[String] = None, var specName: Option[String] = None, var actorName: Option[String] = None) {}

private class Counters(var successes: Long, var failures: Long, var other: Long) extends ResultCountHolder {

  override def completedCount(): Long = successes

  override def failedCount(): Long = failures

  override def otherCount(): Long = other

}
