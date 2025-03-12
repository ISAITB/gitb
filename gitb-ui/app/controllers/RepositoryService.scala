package controllers

import com.gitb.tbs.TestStepStatus
import com.gitb.utils.{XMLDateTimeUtils, XMLUtils}
import com.gitb.xml.export.Export
import config.Configurations
import controllers.dto.ExportDemoCertificateInfo
import controllers.util.ParameterExtractor.requiredBodyParameter
import controllers.util.{Parameters, _}
import exceptions.{ErrorCodes, ServiceCallException}
import jakarta.xml.bind.JAXBElement
import managers._
import managers.export._
import models.Enums.ReportType.ReportType
import models.Enums.{OverviewLevelType, ReportType}
import models._
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils._

import java.io._
import java.nio.file.{Files, Path, Paths}
import javax.inject.Inject
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

class RepositoryService @Inject() (authorizedAction: AuthorizedAction,
                                   cc: ControllerComponents,
                                   communityManager: CommunityManager,
                                   systemManager: SystemManager,
                                   testCaseManager: TestCaseManager,
                                   testSuiteManager: TestSuiteManager,
                                   reportManager: ReportManager,
                                   testResultManager: TestResultManager,
                                   conformanceManager: ConformanceManager,
                                   authorizationManager: AuthorizationManager,
                                   communityLabelManager: CommunityLabelManager,
                                   exportManager: ExportManager,
                                   importPreviewManager: ImportPreviewManager,
                                   importCompleteManager: ImportCompleteManager,
                                   repositoryUtils: RepositoryUtils,
                                   systemConfigurationManager: SystemConfigurationManager)
                                  (implicit ec: ExecutionContext) extends AbstractController(cc) {

	private final val logger = LoggerFactory.getLogger(classOf[RepositoryService])
	private final val codec = new URLCodec()
  private final val EXPORT_QNAME:QName = new QName("http://www.gitb.com/export/v1/", "export")

	def getTestSuiteResource(locationKey: String, filePath:String): Action[AnyContent] = authorizedAction.async { request =>
    // Location key is either a test case ID (e.g. '123') or a test case ID prefixed by a test suite string identifier [TEST_SUITE_IDENTIFIER]|123.
    authorizationManager.canViewTestSuiteResource(request, locationKey).flatMap { _ =>
      var testSuiteIdentifier: Option[String] = None
      var testIdentifier: Option[String] = None
      val locationKeyToUse = codec.decode(locationKey)
      val locationSeparatorIndex = locationKeyToUse.lastIndexOf('|')
      if (locationSeparatorIndex != -1) {
        testSuiteIdentifier = Some(locationKeyToUse.substring(0, locationSeparatorIndex))
        testIdentifier = Some(locationKeyToUse.substring(locationSeparatorIndex+1))
      } else {
        testIdentifier = Some(locationKeyToUse)
      }
      val testCaseId = testIdentifier.get.toLong
      val testSuiteLookup = if (testSuiteIdentifier.isEmpty) {
        // Consider test suite of test case.
        testSuiteManager.getTestSuiteOfTestCase(testCaseId).map(Some(_))
      } else {
        // Find test suite in specification or domain.
        testCaseManager.getSpecificationsOfTestCases(List(testCaseId)).flatMap { specificationsOfTestCase =>
          testCaseManager.getDomainOfTestCase(testCaseId).flatMap { domainId =>
            repositoryUtils.findTestSuiteByIdentifier(testSuiteIdentifier.get, domainId, specificationsOfTestCase.headOption)
          }
        }
      }
      testSuiteLookup.map { testSuite =>
        if (testSuite.isEmpty) {
          NotFound("Resource not found")
        } else {
          var filePathToLookup = codec.decode(filePath)
          if (filePathToLookup.startsWith("/")) {
            filePathToLookup = filePathToLookup.substring(1)
          }
          var filePathToAlsoCheck: Option[String] = null
          if (!filePathToLookup.startsWith(testSuite.get.identifier)) {
            filePathToLookup = testSuite.get.filename + "/" + filePathToLookup
            filePathToAlsoCheck = None
          } else {
            filePathToAlsoCheck = Some(testSuite.get.filename + "/" + filePathToLookup)
            filePathToLookup = StringUtils.replaceOnce(filePathToLookup, testSuite.get.identifier, testSuite.get.filename)
          }
          // Ensure that the requested resource is within the test suite folder (to avoid path traversal)
          val testSuiteFolder = repositoryUtils.getTestSuitesResource(testSuite.get.domain, testSuite.get.filename, None)
          val file = repositoryUtils.getTestSuitesResource(testSuite.get.domain, filePathToLookup, filePathToAlsoCheck)
          logger.debug("Reading test resource ["+codec.decode(filePath)+"] definition from the file ["+file+"]")
          if (file.exists() && file.toPath.normalize().startsWith(testSuiteFolder.toPath.normalize())) {
            Ok.sendFile(file, inline = true)
          } else {
            NotFound("Resource not found")
          }
        }
      }
    }
	}

  def getTestResult(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestSession(request, sessionId, requireAdmin = false).flatMap { _ =>
      getTestResultInternal(sessionId, isAdmin = false)
    }
  }

  def getTestResultAdmin(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestSession(request, sessionId, requireAdmin = true).flatMap { _ =>
      getTestResultInternal(sessionId, isAdmin = true)
    }
  }

  private def getTestResultInternal(sessionId: String, isAdmin: Boolean): Future[Result] = {
    testResultManager.getTestResult(sessionId).flatMap { result =>
      if (result.isDefined) {
        // Load also logs.
        testResultManager.getTestSessionLog(sessionId, isExpected = true).flatMap { logContents =>
          // Load also pending interactions.
          val adminInteractions = if (isAdmin) None else Some(false)
          testResultManager.getTestInteractions(sessionId, adminInteractions).map { pendingInteractions =>
            // Serialise.
            val json = JsonUtil.jsTestResultReport(result.get, None, withOutputMessage = true, logContents, Some(pendingInteractions)).toString()
            ResponseConstructor.constructJsonResponse(json)
          }
        }
      } else {
        Future.successful {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getPendingTestSessionsForAdminInteraction(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.COMMUNITY_ID)
    authorizationManager.canCheckPendingTestSessionInteractions(request, communityId).flatMap { _ =>
      testResultManager.getPendingTestSessionsForAdminInteraction(communityId).map { results =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsStringArray(results).toString())
      }
    }
  }

  def getPendingTestSessionInteractionsAdmin(session: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestSession(request, session, requireAdmin = true).flatMap { _ =>
      testResultManager.getTestInteractions(session, None).map { results =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestInteractions(results).toString())
      }
    }
  }

  def getPendingTestSessionInteractions(session: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestSession(request, session, requireAdmin = false).flatMap { _ =>
      testResultManager.getTestInteractions(session, Some(false)).map { results =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestInteractions(results).toString())
      }
    }
  }

  private def getTestSessionData(sessionFolderInfo: SessionFolderInfo, dataId: String): Option[Path] = {
    var sessionDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = false)
    var dataFile = Path.of(sessionDataFolder.toString, dataId)
    if (!Files.exists(dataFile)) {
      sessionDataFolder = repositoryUtils.getPathForTestSessionData(sessionFolderInfo, tempData = true)
      dataFile = Path.of(sessionDataFolder.toString, dataId)
      if (!Files.exists(dataFile)) {
        None
      } else {
        Some(dataFile)
      }
    } else {
      Some(dataFile)
    }
  }

  def getTestSessionLog(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      testResultManager.getTestSessionLog(sessionId, isExpected = true).map { logContents =>
        if (logContents.isDefined) {
          ResponseConstructor.constructStringResponse(JsonUtil.jsStringArray(logContents.get).toString())
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getTestStepReportDataAsDataUrl(sessionId: String, dataId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      repositoryUtils.getPathForTestSession(sessionId, isExpected = true).map { sessionFolderInfo =>
        try {
          val dataFile = getTestSessionData(sessionFolderInfo, dataId)
          if (dataFile.isEmpty) {
            NotFound
          } else {
            val requestedMimeType = request.headers.get(ACCEPT)
            val detectedMimeType = Option(MimeUtil.getMimeType(dataFile.get))
            var mimeTypeToUse: Option[String] = None
            var dataUrl: Option[String] = None
            if (requestedMimeType.isEmpty || requestedMimeType.get.contains("*")) {
              mimeTypeToUse = detectedMimeType
              dataUrl = Some(MimeUtil.getFileAsDataURL(dataFile.get.toFile, mimeTypeToUse.getOrElse("application/octet-stream")))
            } else {
              mimeTypeToUse = requestedMimeType
              if (MimeUtil.isImageType(requestedMimeType.get) && detectedMimeType.exists(_.equals("text/plain"))) {
                // This is an image stored as a base64-encoded string.
                dataUrl = Some(MimeUtil.base64AsDataURL(Files.readString(dataFile.get), requestedMimeType.get))
              } else {
                dataUrl = Some(MimeUtil.getFileAsDataURL(dataFile.get.toFile, requestedMimeType.get))
              }
            }
            ResponseConstructor.constructJsonResponse(JsonUtil.jsFileReference(dataUrl.get, mimeTypeToUse.getOrElse("application/octet-stream")).toString())
          }
        } finally {
          if (sessionFolderInfo.archived) {
            FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
          }
        }
      }
    }
  }

  def getTestStepReportData(sessionId: String, dataId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      repositoryUtils.getPathForTestSession(sessionId, isExpected = true).map { sessionFolderInfo =>
        val dataFile = getTestSessionData(sessionFolderInfo, dataId)
        if (dataFile.isEmpty) {
          NotFound
        } else {
          var mimeTypeToUse = request.headers.get(ACCEPT)
          if (mimeTypeToUse.isEmpty || mimeTypeToUse.get.contains("*")) {
            mimeTypeToUse = Option(MimeUtil.getMimeType(dataFile.get))
          }
          val extension = MimeUtil.getExtensionFromMimeType(mimeTypeToUse.orNull)
          Ok.sendFile(
            content = dataFile.get.toFile,
            fileName = _ => Some(dataId+extension),
            onClose = () => {
              if (sessionFolderInfo.archived) {
                FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
              }
            }
          )
        }
      }
    }
  }

  def getTestStepReport(sessionId: String, reportPath: String): Action[AnyContent] = authorizedAction.async { request =>
//    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      repositoryUtils.getPathForTestSessionWrapper(sessionId, isExpected = true).flatMap { sessionFolderInfo =>
        Future.successful {
          var path: String = null
          path = reportPath
            .replace("__SQS__", "[")
            .replace("__SQE__", "]")

          if (path.startsWith(sessionId)) {
            // Backwards compatibility.
            val pathParts = StringUtils.split(codec.decode(path), "/")
            path = pathParts(1)
          } else {
            path = path
          }
          path = codec.decode(path)
          val file = new File(sessionFolderInfo.path.toFile, path)

          if (file.exists()) {
            //read file into a string
            val bytes  = Files.readAllBytes(Paths.get(file.getAbsolutePath))
            val string = new String(bytes)

            //convert string in xml format into its object representation
            val step = XMLUtils.unmarshal(classOf[TestStepStatus], new StreamSource(new StringReader(string)))

            //serialize report inside the object into json
            ResponseConstructor.constructJsonResponse(JacksonUtil.serializeTestReport(step.getReport))
          } else {
            NotFound
          }
        }.andThen { _ =>
          if (sessionFolderInfo.archived) {
            FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
          }
        }
      }
    }
	}

  def exportTestStepReport(sessionId: String, stepReportPath: String): Action[AnyContent] = authorizedAction.async { request =>
    //    34888315-6781-4d74-a677-8f9001a02cb8/4.xml
    val contentType = request.headers.get("Accept").getOrElse(Constants.MimeTypeXML)
    val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
    val reportPath = getReportTempFile(suffix)
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      var path: String = if (stepReportPath.startsWith(sessionId)) {
        // Backwards compatibility.
        val pathParts = StringUtils.split(codec.decode(stepReportPath), "/")
        pathParts(1)
      } else {
        stepReportPath
      }
      path = codec.decode(path)
      reportManager.generateTestStepReport(reportPath, sessionId, path, contentType, ParameterExtractor.extractOptionalUserId(request)).map { reportFile =>
        if (reportFile.isEmpty) {
          NotFound
        } else {
          Ok.sendFile(
            content = reportFile.get.toFile,
            fileName = _ => Some("step_report"+suffix),
            onClose = () => {
              if (reportFile.isDefined) {
                FileUtils.deleteQuietly(reportFile.get.toFile)
              }
            }
          )
        }
      }
    }.recover {
      case e: Exception =>
        if (Files.exists(reportPath)) {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
        throw e
    }
  }

  def exportTestCaseReport(): Action[AnyContent] = authorizedAction.async { request =>
    val session = ParameterExtractor.requiredQueryParameter(request, Parameters.SESSION_ID)
    val contentType = request.headers.get(Constants.AcceptHeader).getOrElse(Constants.MimeTypeXML)
    val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
    val reportPath = getReportTempFile(suffix)
    authorizationManager.canViewTestResultForSession(request, session).flatMap { _ =>
      reportManager.generateTestCaseReport(reportPath, session, contentType, None, ParameterExtractor.extractOptionalUserId(request)).map { reportFile =>
        if (reportFile.isDefined) {
          Ok.sendFile(
            content = reportFile.get.toFile,
            fileName = _ => Some("test_report"+suffix),
            onClose = () => {
              if (reportFile.isDefined) {
                FileUtils.deleteQuietly(reportFile.get.toFile)
              }
            }
          )
        } else {
          NotFound
        }
      }
    }.recover {
      case e: Exception =>
        if (Files.exists(reportPath)) {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
        throw e
    }
  }

  private def exportConformanceOverviewCertificateInternal(settings: Option[ConformanceOverviewCertificateWithMessages], communityId: Long, systemId: Long, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], snapshotId: Option[Long]): Future[Result] = {
    val reportPath = getReportTempFile(".pdf")
    reportManager.generateConformanceOverviewCertificate(reportPath, settings, systemId, domainId, groupId, specificationId, communityId, snapshotId).map { _ =>
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_certificate.pdf"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    }.recover {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        logger.warn("Error while generating conformance certificate preview", e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
    }
  }

  def exportOwnConformanceOverviewCertificateReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewOwnConformanceCertificateReport(request, systemId, snapshotId).flatMap { _ =>
      val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
      systemManager.getCommunityIdOfSystem(systemId).flatMap { communityId =>
        exportConformanceOverviewCertificateInternal(None, communityId, systemId, domainId, groupId, specificationId, snapshotId)
      }
    }
  }

  def exportConformanceOverviewCertificateReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewConformanceCertificateReport(request, communityId, snapshotId).flatMap { _ =>
      val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
      val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
      val jsSettings = ParameterExtractor.optionalBodyParameter(request, Parameters.SETTINGS)
      val settings = jsSettings.map(JsonUtil.parseJsConformanceOverviewCertificateWithMessages(_, communityId))
      exportConformanceOverviewCertificateInternal(settings, communityId, systemId, domainId, groupId, specificationId, snapshotId)
    }
  }

  def exportConformanceOverviewReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId).flatMap { _ =>
      val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)

      val reportPath = getReportTempFile(".pdf")
      val task = for {
        communityId <- {
          if (snapshotId.isDefined) {
            conformanceManager.getConformanceSnapshot(snapshotId.get).map(_.community)
          } else {
            systemManager.getCommunityIdOfSystem(systemId)
          }
        }
        result <- {
          reportManager.generateConformanceOverviewReport(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId).map { _ =>
            Ok.sendFile(
              content = reportPath.toFile,
              fileName = _ => Some(reportPath.toFile.getName),
              onClose = () => {
                FileUtils.deleteQuietly(reportPath.toFile)
              }
            )
          }
        }
      } yield result
      task.recover {
        case e: Exception =>
          FileUtils.deleteQuietly(reportPath.toFile)
          throw e
      }
    }
  }

  def exportOwnConformanceOverviewReportInXML(): Action[AnyContent] = authorizedAction.async { request =>
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    for {
      _ <- {
        if (snapshotId.isDefined) {
          authorizationManager.canViewSystemInSnapshot(request, systemId, snapshotId.get)
        } else {
          authorizationManager.canViewSystem(request, systemId)
        }
      }
      result <- {
        val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
        val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
        val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
        systemManager.getCommunityIdOfSystem(systemId).flatMap { communityId =>
          val reportPath = getReportTempFile(".pdf")
          reportManager.generateConformanceOverviewReportInXML(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId).map { _ =>
            Ok.sendFile(
              content = reportPath.toFile,
              fileName = _ => Some("conformance_report.xml"),
              onClose = () => {
                FileUtils.deleteQuietly(reportPath.toFile)
              }
            )
          }.recover {
            case e: Exception =>
              FileUtils.deleteQuietly(reportPath.toFile)
              throw e
          }
        }
      }
    } yield result
  }

  def exportConformanceOverviewReportInXML(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
      val domainId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      val specificationId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
      val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
      val reportPath = getReportTempFile(".pdf")
      reportManager.generateConformanceOverviewReportInXML(reportPath, systemId, domainId, groupId, specificationId, communityId, snapshotId).map { _ =>
        Ok.sendFile(
          content = reportPath.toFile,
          fileName = _ => Some("conformance_report.xml"),
          onClose = () => {
            FileUtils.deleteQuietly(reportPath.toFile)
          }
        )
      }.recover {
        case e: Exception =>
          FileUtils.deleteQuietly(reportPath.toFile)
          throw e
      }
    }
  }

  def exportOwnConformanceStatementReportInXML(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId).flatMap { _ =>
      val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.TESTS).getOrElse(false)
      systemManager.getCommunityIdOfSystem(systemId).flatMap { communityId =>
        val reportPath = getReportTempFile(".pdf")
        reportManager.generateConformanceStatementReportInXML(reportPath, withTestSteps, actorId, systemId, communityId, snapshotId).map { _ =>
          Ok.sendFile(
            content = reportPath.toFile,
            fileName = _ => Some("conformance_report.xml"),
            onClose = () => {
              FileUtils.deleteQuietly(reportPath.toFile)
            }
          )
        }.recover {
          case e: Exception =>
            FileUtils.deleteQuietly(reportPath.toFile)
            throw e
        }
      }
    }
  }

  def exportConformanceStatementReportInXML(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.TESTS).getOrElse(false)
      val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
      val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
      val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
      val reportPath = getReportTempFile(".xml")
      reportManager.generateConformanceStatementReportInXML(reportPath, withTestSteps, actorId, systemId, communityId, snapshotId).map { _ =>
        Ok.sendFile(
          content = reportPath.toFile,
          fileName = _ => Some("conformance_report.xml"),
          onClose = () => {
            FileUtils.deleteQuietly(reportPath.toFile)
          }
        )
      }.recover {
        case e: Exception =>
          FileUtils.deleteQuietly(reportPath.toFile)
          throw e
      }
    }
  }

  def exportConformanceStatementReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    for {
      _ <- authorizationManager.canViewConformanceStatementReport(request, systemId, snapshotId)
      labels <- communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request))
      communityId <- {
        if (snapshotId.isDefined) {
          conformanceManager.getConformanceSnapshot(snapshotId.get).map(_.community)
        } else {
          systemManager.getCommunityIdOfSystem(systemId)
        }
      }
      result <- {
        val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
        val includeTests = ParameterExtractor.requiredBodyParameter(request, Parameters.TESTS).toBoolean
        val reportPath = getReportTempFile(".pdf")
        reportManager.generateConformanceStatementReport(reportPath, includeTests, actorId, systemId, labels, communityId, snapshotId).map { _ =>
          Ok.sendFile(
            content = reportPath.toFile,
            fileName = _ => Some("conformance_report.pdf"),
            onClose = () => {
              FileUtils.deleteQuietly(reportPath.toFile)
            }
          )
        }.recover {
          case e: Exception =>
            FileUtils.deleteQuietly(reportPath.toFile)
            throw e
        }
      }
    } yield result
  }

  private def exportConformanceCertificateInternal(settings: Option[ConformanceCertificateInfo], communityId: Long, systemId: Long, actorId: Long, snapshotId: Option[Long]): Future[Result] = {
    val reportPath = getReportTempFile(".pdf")
    reportManager.generateConformanceCertificate(reportPath, settings, actorId, systemId, communityId, snapshotId).map { _ =>
      Ok.sendFile(
        content = reportPath.toFile,
        fileName = _ => Some("conformance_certificate.pdf"),
        onClose = () => {
          FileUtils.deleteQuietly(reportPath.toFile)
        }
      )
    }.recover {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        throw e
    }
  }

  def exportDemoConformanceOverviewCertificateReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoCertificateReport(communityId, request, ReportType.ConformanceOverviewCertificate , exportInfo => {
      val certificateSettings = JsonUtil.parseJsConformanceOverviewCertificateWithMessages(exportInfo.jsSettings, communityId)
      val reportIdentifier = ParameterExtractor.optionalLongBodyParameter(exportInfo.paramMap, Parameters.ID)
      val reportLevel = OverviewLevelType.withName(ParameterExtractor.requiredBodyParameter(exportInfo.paramMap, Parameters.LEVEL))
      for {
        keystore <- {
          // Get the keystore (if needed) to use for the signature
          if (certificateSettings.settings.includeSignature) {
            communityManager.getCommunityKeystore(communityId, decryptKeys = true)
          } else {
            Future.successful(None)
          }
        }
        settingsToUse <- {
          if (!exportInfo.reportSettings.customPdfs) {
            // Get the message (if needed) for the specific level
            val customMessage = certificateSettings.messageToUse(reportLevel, reportIdentifier)
            Future.successful {
              Some(certificateSettings.settings.toConformanceCertificateInfo(customMessage, keystore))
            }
          } else {
            Future.successful(None)
          }
        }
        _ <- reportManager.generateDemoConformanceOverviewCertificate(exportInfo.reportPath, exportInfo.reportSettings, exportInfo.stylesheetPath, settingsToUse, communityId, reportLevel)
      } yield ()
    })
  }

  def exportOwnConformanceCertificateReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canViewOwnConformanceCertificateReport(request, systemId, snapshotId).flatMap { _ =>
      systemManager.getCommunityIdOfSystem(systemId).flatMap { communityId =>
        exportConformanceCertificateInternal(None, communityId, systemId, actorId, snapshotId)
      }
    }
  }

  def exportConformanceCertificateReport(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    for {
      _ <- authorizationManager.canViewConformanceCertificateReport(request, communityId, snapshotId)
      settings <- {
        val jsSettings = ParameterExtractor.optionalBodyParameter(request, Parameters.SETTINGS)
        jsSettings.map(x => {
          val parsedSettings = JsonUtil.parseJsConformanceCertificateSettings(x, communityId)
          if (parsedSettings.includeSignature) {
            communityManager.getCommunityKeystore(communityId, decryptKeys = true).map { keystore =>
              Some(parsedSettings.toConformanceCertificateInfo(keystore))
            }
          } else {
            Future.successful {
              Some(parsedSettings.toConformanceCertificateInfo(None))
            }
          }
        }).getOrElse(Future.successful(None))
      }
      result <- exportConformanceCertificateInternal(settings, communityId, systemId, actorId, snapshotId)
    } yield result
  }

  private def exportDemoCertificateReport(communityId: Long, request: RequestWithAttributes[AnyContent], reportType: ReportType, reportGenerator: ExportDemoCertificateInfo => Future[Unit]): Future[Result] = {
    val reportPath = getReportTempFile(".pdf")
    val task = for {
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      reportSettings <- Future.successful(ParameterExtractor.extractCommunityReportSettings(paramMap, communityId))
      jsSettings <- Future.successful(ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS))
      _ <- authorizationManager.canPreviewConformanceCertificateReport(request, communityId)
      stylesheetInfo <- {
        var result: Option[Result] = None
        var stylesheetFile: Option[Path] = None
        if (reportSettings.customPdfs) {
          if (reportSettings.customPdfsWithCustomXml) {
            val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
            if (stylesheetInfo._2.isEmpty) {
              if (stylesheetInfo._1.isDefined) {
                // Stylesheet provided as part of the request
                stylesheetFile = Some(stylesheetInfo._1.get.toPath)
              } else {
                // Already existing stylesheet
                stylesheetFile = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
              }
            } else {
              result = stylesheetInfo._2
            }
          }
        }
        Future.successful((result, stylesheetFile))
      }
      result <- {
        if (stylesheetInfo._1.isEmpty) {
          reportGenerator.apply(ExportDemoCertificateInfo(reportSettings, stylesheetInfo._2, jsSettings, reportPath, paramMap)).map { _ =>
            Ok.sendFile(
              content = reportPath.toFile,
              fileName = _ => Some("conformance_certificate.pdf"),
              onClose = () => {
                FileUtils.deleteQuietly(reportPath.toFile)
              }
            )
          }
        } else {
          Future.successful(stylesheetInfo._1.get)
        }
      }
    } yield result
    task.recover {
      case e: ServiceCallException => handleDemoReportServiceError(e, reportPath)
      case e: Exception => handleDemoReportGeneralError(e, reportPath)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def exportDemoConformanceCertificateReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoCertificateReport(communityId, request, ReportType.ConformanceStatementCertificate, exportInfo => {
      val certificateSettings = JsonUtil.parseJsConformanceCertificateSettings(exportInfo.jsSettings, communityId)
      for {
        keystore <- {
          if (certificateSettings.includeSignature) {
            communityManager.getCommunityKeystore(communityId, decryptKeys = true)
          } else {
            Future.successful(None)
          }
        }
        settingsToUse <- {
          if (!exportInfo.reportSettings.customPdfs) {
            Future.successful {
              Some(certificateSettings.toConformanceCertificateInfo(keystore))
            }
          } else {
            Future.successful(None)
          }
        }
        _ <- reportManager.generateDemoConformanceCertificate(exportInfo.reportPath, exportInfo.reportSettings, exportInfo.stylesheetPath, settingsToUse, communityId)
      } yield ()
    })
  }

  def exportDemoTestStepReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReport(request, communityId, "step_report.pdf", ReportType.TestStepReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoTestStepReport(reportPath, settings, xsltPath).map(_ => ())
    })
  }

  def exportDemoTestStepReportInXML(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReportInXML(request, communityId, "step_report.xml", ReportType.TestStepReport, (reportPath: Path, xsltPath: Option[Path], _: Option[Map[String, Seq[String]]]) => {
      reportManager.generateDemoTestStepReportInXML(reportPath, xsltPath).map(_ => ())
    })
  }

  def exportDemoTestCaseReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReport(request, communityId, "test_report.pdf", ReportType.TestCaseReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoTestCaseReport(reportPath, settings, xsltPath).map(_ => ())
    })
  }

  def exportDemoTestCaseReportInXML(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReportInXML(request, communityId, "test_report.xml", ReportType.TestCaseReport, (reportPath: Path, xsltPath: Option[Path], _: Option[Map[String, Seq[String]]]) => {
      reportManager.generateDemoTestCaseReportInXML(reportPath, xsltPath).map(_ => ())
    })
  }

  def exportDemoConformanceOverviewReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReport(request, communityId, "conformance_report.pdf", ReportType.ConformanceOverviewReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val level = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.LEVEL).map(OverviewLevelType.withName).getOrElse(OverviewLevelType.OrganisationLevel)
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      reportManager.generateDemoConformanceOverviewReport(reportPath, settings, xsltPath, communityId, level).map(_ => ())
    })
  }

  def exportDemoConformanceOverviewReportInXML(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReportInXML(request, communityId, "conformance_report.xml", ReportType.ConformanceOverviewReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val level = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.LEVEL).map(OverviewLevelType.withName).getOrElse(OverviewLevelType.OrganisationLevel)
      reportManager.generateDemoConformanceOverviewReportInXML(reportPath, xsltPath, communityId, level).map(_ => ())
    })
  }

  def exportDemoConformanceStatementReport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReport(request, communityId, "conformance_report.pdf", ReportType.ConformanceStatementReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.TESTS).getOrElse(false)
      reportManager.generateDemoConformanceStatementReport(reportPath, settings, xsltPath, withTestSteps, communityId).map(_ => ())
    })
  }

  def exportDemoConformanceStatementReportInXML(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    exportDemoReportInXML(request, communityId, "conformance_report.xml", ReportType.ConformanceStatementReport, (reportPath: Path, xsltPath: Option[Path], paramMap: Option[Map[String, Seq[String]]]) => {
      val withTestSteps = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.TESTS).getOrElse(false)
      reportManager.generateDemoConformanceStatementReportInXML(reportPath, xsltPath, withTestSteps, communityId).map(_ => ())
    })
  }

  private def exportDemoReport(request: RequestWithAttributes[AnyContent], communityId: Long, reportName: String, reportType: ReportType, handler: (Path, Option[Path], Option[Map[String, Seq[String]]]) => Future[Unit]): Future[Result] = {
    val reportPath = getReportTempFile(".pdf")
    val task = for {
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      _ <- authorizationManager.canManageCommunity(request, communityId)
      stylesheetInfo <- {
        val settings = ParameterExtractor.extractCommunityReportSettings(paramMap, communityId)
        var stylesheetToUse: Option[Path] = None
        var resultToReturn: Option[Result] = None
        val useStyleSheet = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.USE_STYLE_SHEET).toBoolean
        if (useStyleSheet && settings.customPdfsWithCustomXml) {
          val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
          if (stylesheetInfo._2.isEmpty) {
            if (stylesheetInfo._1.isDefined) {
              // Stylesheet provided as part of the request
              stylesheetToUse = Some(stylesheetInfo._1.get.toPath)
            } else {
              // Already existing stylesheet
              stylesheetToUse = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
            }
          } else {
            resultToReturn = stylesheetInfo._2
          }
        }
        Future.successful((resultToReturn, stylesheetToUse))
      }
      result <- {
        if (stylesheetInfo._1.isEmpty) {
          handler.apply(reportPath, stylesheetInfo._2, paramMap).map { _ =>
            Ok.sendFile(
              content = reportPath.toFile,
              fileName = _ => Some(reportName),
              onClose = () => {
                FileUtils.deleteQuietly(reportPath.toFile)
              }
            )
          }
        } else {
          Future.successful(stylesheetInfo._1.get)
        }
      }
    } yield result
    task.recover {
      case e: ServiceCallException => handleDemoReportServiceError(e, reportPath)
      case e: Exception => handleDemoReportGeneralError(e, reportPath)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  private def handleDemoReportServiceError(e: ServiceCallException, reportPath: Path): Result = {
    FileUtils.deleteQuietly(reportPath.toFile)
    logger.warn("Error while generating PDF report", e)
    val json = if (e.cause.isDefined) {
      JsonUtil.jsErrorMessages(reportManager.extractFailureDetails(e), "text/plain")
    } else if (e.responseBody.exists(StringUtils.isNotBlank)) {
      JsonUtil.jsErrorMessages(List(e.responseBody.get), e.responseContentType.getOrElse("text/plain"))
    } else {
      JsonUtil.jsErrorMessages(List(e.message), "text/plain")
    }
    ResponseConstructor.constructJsonResponse(json.toString())
  }

  private def handleDemoReportGeneralError(e: Exception, reportPath: Path): Result = {
    FileUtils.deleteQuietly(reportPath.toFile)
    logger.warn("Error while generating PDF report", e)
    ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
  }

  private def exportDemoReportInXML(request: RequestWithAttributes[AnyContent], communityId: Long, reportName: String, reportType: ReportType, handler: (Path, Option[Path], Option[Map[String, Seq[String]]]) => Future[Unit]): Future[Result] = {
    val reportPath = getReportTempFile(".xml")
    val task = for {
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      _ <- authorizationManager.canManageCommunity(request, communityId)
      stylesheetInfo <- {
        val enabled = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.ENABLE).toBoolean
        var stylesheetToUse: Option[Path] = None
        var resultToReturn: Option[Result] = None
        if (enabled) {
          val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
          if (stylesheetInfo._2.isEmpty) {
            if (stylesheetInfo._1.isDefined) {
              // Stylesheet provided as part of the request
              stylesheetToUse = Some(stylesheetInfo._1.get.toPath)
            } else {
              // Already existing stylesheet
              stylesheetToUse = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
            }
          } else {
            resultToReturn = stylesheetInfo._2
          }
        }
        Future.successful {
          (resultToReturn, stylesheetToUse)
        }
      }
      result <- {
        if (stylesheetInfo._1.isEmpty) {
          handler.apply(reportPath, stylesheetInfo._2, paramMap).map { _ =>
            Ok.sendFile(
              content = reportPath.toFile,
              fileName = _ => Some(reportName),
              onClose = () => {
                FileUtils.deleteQuietly(reportPath.toFile)
              }
            )
          }
        } else {
          Future.successful(stylesheetInfo._1.get)
        }
      }
    } yield result
    task.recover {
      case e: Exception =>
        FileUtils.deleteQuietly(reportPath.toFile)
        logger.warn("Error while generating XML report", e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Preview failed. Please check your configuration and try again.")
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def loadReportSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val reportType = ReportType.apply(ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort)
      reportManager.getReportSettings(communityId, reportType).map { settings =>
        val styleSheetExists = repositoryUtils.getCommunityReportStylesheet(communityId, reportType).isDefined
        ResponseConstructor.constructJsonResponse(JsonUtil.jsReportSettings(settings, styleSheetExists).toString)
      }
    }
  }

  def getReportStylesheet(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).map { _ =>
      val reportType = ReportType.apply(ParameterExtractor.requiredQueryParameter(request, Parameters.TYPE).toShort)
      val stylesheet = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
      if (stylesheet.isDefined) {
        Ok.sendFile(
          content = stylesheet.get.toFile,
          fileName = _ => Some("stylesheet.xslt")
        )
      } else {
        NotFound
      }
    }
  }

  def updateConformanceCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    handleUpdateCertificateSettings(communityId, request, (reportSettings, stylesheet, jsSettings) => {
      val certificateSettings = JsonUtil.parseJsConformanceCertificateSettings(jsSettings, communityId)
      reportManager.updateConformanceCertificateSettings(certificateSettings, reportSettings, stylesheet)
    })
  }

  def updateConformanceOverviewCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    handleUpdateCertificateSettings(communityId, request, (reportSettings, stylesheet, jsSettings) => {
      val certificateSettings = JsonUtil.parseJsConformanceOverviewCertificateWithMessages(jsSettings, communityId)
      reportManager.updateConformanceOverviewCertificateSettings(certificateSettings, reportSettings, stylesheet)
    })
  }

  private def handleUpdateCertificateSettings(communityId: Long, request: RequestWithAttributes[AnyContent], certificateSettingsHandler: (CommunityReportSettings, Option[Option[Path]], String) => Future[Unit]): Future[Result] = {
    val task = for {
      _ <- authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      reportSettings <- Future.successful(ParameterExtractor.extractCommunityReportSettings(paramMap, communityId))
      stylesheetInfo <- {
        var result: Option[Result] = None
        var stylesheetFile: Option[Option[Path]] = None
        if (reportSettings.customPdfsWithCustomXml) {
          val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
          result = stylesheetInfo._2
          if (result.isEmpty) {
            if (stylesheetInfo._1.isDefined) {
              stylesheetFile = Some(stylesheetInfo._1.map(_.toPath))
            }
          }
        } else {
          // Delete stylesheet.
          stylesheetFile = Some(None)
        }
        Future.successful{
          (result, stylesheetFile)
        }
      }
      result <- {
        if (stylesheetInfo._1.isEmpty) {
          val jsSettings = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS)
          certificateSettingsHandler.apply(reportSettings, stylesheetInfo._2, jsSettings).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(stylesheetInfo._1.get)
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

  def updateReportSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val task = for {
      _ <- authorizationManager.canManageCommunity(request, communityId)
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      reportSettings <- Future.successful(ParameterExtractor.extractCommunityReportSettings(paramMap, communityId))
      stylesheetInfo <- {
        val useStyleSheet = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.USE_STYLE_SHEET).toBoolean
        var stylesheetFile: Option[Option[Path]] = None
        var result: Option[Result] = None
        if (useStyleSheet) {
          val stylesheetInfo = ParameterExtractor.extractReportStylesheet(request)
          result = stylesheetInfo._2
          if (result.isEmpty) {
            if (stylesheetInfo._1.isDefined) {
              stylesheetFile = Some(stylesheetInfo._1.map(_.toPath))
            }
          }
        } else {
          // Delete stylesheet
          stylesheetFile = Some(None)
        }
        Future.successful {
          (result, stylesheetFile)
        }
      }
      result <- {
        if (stylesheetInfo._1.isEmpty) {
          reportManager.updateReportSettings(reportSettings, stylesheetInfo._2).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(stylesheetInfo._1.get)
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
    }
  }

	def getTestCaseDefinition(testId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestCase(request, testId).flatMap { _ =>
      testCaseManager.getTestCase(testId).flatMap { tc =>
        if (tc.isDefined) {
          val testCaseId = testId.toLong
          testCaseManager.getDomainOfTestCase(testCaseId).map { domainId =>
            val file = repositoryUtils.getTestSuitesResource(domainId, tc.get.path, None)
            logger.debug("Reading test case ["+testId+"] definition from the file ["+file+"]")
            if (file.exists()) {
              Ok.sendFile(file, inline = true)
            } else {
              NotFound
            }
          }
        } else {
          Future.successful(NotFound)
        }
      }
    }
	}

  def searchTestCases(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewAllTestCases(request).flatMap { _ =>
      val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
      val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
      val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
      val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.TEST_SUITE_IDS)
      testCaseManager.searchTestCases(domainIds, specificationIds, groupIds, actorIds, testSuiteIds).map { testCases =>
        val json = JsonUtil.jsTestCasesList(testCases).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchTestCasesInDomain(): Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canViewDomains(request, Some(List(domainId))).flatMap { _ =>
      val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
      val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
      val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.TEST_SUITE_IDS)

      testCaseManager.searchTestCases(Some(List(domainId)), specificationIds, groupIds, actorIds, testSuiteIds).map { testCases =>
        val json = JsonUtil.jsTestCasesList(testCases).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def exportDomain(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
        exportManager.exportDomain(domainId, exportSettings)
      })
    }
  }

  def exportDomainAndSettings(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemSettings(request).flatMap { _ =>
      exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
        exportManager.exportDomain(domainId, exportSettings)
      })
    }
  }

  def exportCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
        exportManager.exportCommunity(communityId, exportSettings)
      })
    }
  }

  def exportCommunityAndSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemSettings(request).flatMap { _ =>
      exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
        exportManager.exportCommunity(communityId, exportSettings)
      })
    }
  }

  def exportSystemSettings(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemConfigurationValues(request).flatMap { _ =>
      exportInternal(request, includeSettings = true, (exportSettings: ExportSettings) => {
        exportManager.exportSystemSettings(exportSettings)
      })
    }
  }

  def exportDeletions(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteAnyDomain(request).flatMap { _ =>
      exportInternal(request, includeSettings = false, (exportSettings: ExportSettings) => {
        exportManager.exportDeletions(exportSettings.communitiesToDelete.getOrElse(List.empty[String]), exportSettings.domainsToDelete.getOrElse(List.empty[String]))
      })
    }
  }

  private def exportInternal(request: Request[AnyContent], includeSettings: Boolean, fnExportData: ExportSettings => Future[Export]): Future[Result] = {
    // Get export settings to apply.
    val exportSettings = JsonUtil.parseJsExportSettings(requiredBodyParameter(request, Parameters.VALUES), includeSettings)
    fnExportData.apply(exportSettings).map { exportData =>
      exportData.setVersion(Configurations.mainVersionNumber())
      exportData.setTimestamp(XMLDateTimeUtils.getXMLGregorianCalendarDateTime)
      // Define temp file paths.
      val exportPathXml = getReportTempFile(".xml")
      val exportPathZip = getReportTempFile(".zip")
      try {
        // Serialise to temp XML file.
        Using.resource(Files.newOutputStream(exportPathXml)) { xmlOS =>
          XMLUtils.marshalToStream(new JAXBElement(EXPORT_QNAME, classOf[com.gitb.xml.export.Export], exportData), xmlOS)
          xmlOS.flush()
        }
        // Compress
        new ZipArchiver(exportPathXml, exportPathZip, exportSettings.encryptionKey.get.toCharArray).zip()
        // Send
        Ok.sendFile(
          content = exportPathZip.toFile,
          inline = false,
          fileName = _ => Some("data.zip"),
          onClose = () => FileUtils.deleteQuietly(exportPathZip.toFile)
        )
      } catch {
        case e:Exception =>
          FileUtils.deleteQuietly(exportPathZip.toFile)
          throw e
      } finally {
        FileUtils.deleteQuietly(exportPathXml.toFile)
      }
    }
  }

  private def processImport(request: Request[AnyContent], requireDomain: Boolean, requireCommunity: Boolean, requireSettings: Boolean, requireDeletions: Boolean, fnImportData: (Export, ImportSettings) => Future[List[ImportItem]]): Future[Result] = {
    // Get import settings.
    val paramMap = ParameterExtractor.paramMap(request)
    val files = ParameterExtractor.extractFiles(request)
    val importSettings = JsonUtil.parseJsImportSettings(ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SETTINGS))
    if (files.contains(Parameters.FILE)) {
      importPreviewManager.prepareImportPreview(files(Parameters.FILE).file, importSettings, requireDomain, requireCommunity, requireSettings, requireDeletions).flatMap { result =>
        if (result._1.isDefined) {
          // We have an error.
          Future.successful {
            ResponseConstructor.constructErrorResponse(result._1.get._1, result._1.get._2, Some("archive"))
          }
        } else {
          // All ok.
          fnImportData.apply(result._2.get, importSettings).map { importItems =>
            val json = JsonUtil.jsImportPreviewResult(result._3.get, importItems).toString()
            ResponseConstructor.constructJsonResponse(json)
          }.recover {
            case e:Exception =>
              logger.error("An unexpected error occurred while processing the provided archive.", e)
              // Delete the temporary file.
              if (result._4.isDefined) {
                FileUtils.deleteQuietly(result._4.get.toFile)
              }
              ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An error occurred while processing the provided archive.")
          }
        }
      }
    } else {
      Future.successful {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "No archive was provided")
      }
    }
  }

  def uploadSystemSettingsExport(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemSettings(request).flatMap { _ =>
      processImport(request, requireDomain = false, requireCommunity = false, requireSettings = true, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
        importPreviewManager.previewSystemSettingsImport(exportData.getSettings).map { result =>
          List(result._1)
        }
      })
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadDeletionsExport(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteAnyDomain(request).flatMap { _ =>
      processImport(request, requireDomain = false, requireCommunity = false, requireSettings = false, requireDeletions = true, (exportData: Export, settings: ImportSettings) => {
        importPreviewManager.previewDeletionsImport(exportData.getDeletions)
      })
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadCommunityExportTestBedAdmin(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateCommunity(request).flatMap { _ =>
      uploadCommunityExportInternal(request, communityId, canDoAdminOperations = true)
    }
  }

  def uploadCommunityExportCommunityAdmin(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      uploadCommunityExportInternal(request, communityId, canDoAdminOperations = false)
    }
  }

  private def emptyForNegativeId(id: Long): Option[Long] = {
    if (id == -1) {
      None
    } else {
      Some(id)
    }
  }

  private def uploadCommunityExportInternal(request: Request[AnyContent], communityId: Long, canDoAdminOperations: Boolean): Future[Result] = {
    processImport(request, requireDomain = false, requireCommunity = true, requireSettings = false, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
      importPreviewManager.previewCommunityImport(exportData, emptyForNegativeId(communityId), canDoAdminOperations, settings).map { result =>
        val items = new ListBuffer[ImportItem]()
        // First add domain.
        if (result._2.isDefined) {
          items += result._2.get
        }
        // Next add community.
        items += result._1.get
        // Finally add system settings.
        if (result._3.isDefined) {
          items += result._3.get
        }
        items.toList
      }
    }).andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadDomainExportTestBedAdmin(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateDomain(request).flatMap { _ =>
      uploadDomainExportInternal(request, domainId, canDoAdminOperations = true)
    }
  }

  def uploadDomainExportCommunityAdmin(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      uploadDomainExportInternal(request, domainId, canDoAdminOperations = false)
    }
  }

  private def uploadDomainExportInternal(request: Request[AnyContent], domainId: Long, canDoAdminOperations: Boolean): Future[Result] = {
    processImport(request, requireDomain = true, requireCommunity = false, requireSettings = false, requireDeletions = false, (exportData: Export, settings: ImportSettings) => {
      importPreviewManager.previewDomainImport(exportData.getDomains.getDomain.get(0), emptyForNegativeId(domainId), canDoAdminOperations, settings).map { result =>
        List(result)
      }
    }).andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def cancelImportInternal(request: Request[AnyContent]): Future[Result] = {
    Future.successful {
      val pendingImportId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
      val pendingFolder = Paths.get(importPreviewManager.getPendingFolder().getAbsolutePath, pendingImportId)
      // Delete temporary folder (if exists)
      FileUtils.deleteQuietly(pendingFolder.toFile)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def cancelDomainImport(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      cancelImportInternal(request)
    }
  }

  def cancelSystemSettingsImport(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemSettings(request).flatMap { _ =>
      cancelImportInternal(request)
    }
  }

  def cancelCommunityImport(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      cancelImportInternal(request)
    }
  }

  private def confirmImportInternal(request: Request[AnyContent], fnImportData: (Export, ImportSettings, List[ImportItem]) => Future[Unit]): Future[Result] = {
    val pendingImportId = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID)
    val pendingFolder = Paths.get(importPreviewManager.getPendingFolder().getAbsolutePath, pendingImportId)
    Future.successful {
      // Get XML file and deserialize.
      val xmlFile = importPreviewManager.getPendingImportFile(pendingFolder, pendingImportId)
      if (xmlFile.isDefined) {
        // We can skip XSD validation this time as the time was checked previously (at initial upload).
        val exportData: Export = XMLUtils.unmarshal(classOf[com.gitb.xml.export.Export], new StreamSource(Files.newInputStream(xmlFile.get.toPath)))
        val importSettings = JsonUtil.parseJsImportSettings(ParameterExtractor.requiredBodyParameter(request, Parameters.SETTINGS))
        importSettings.dataFilePath = Some(xmlFile.get.toPath)
        val importItems = JsonUtil.parseJsImportItems(ParameterExtractor.requiredBodyParameter(request, Parameters.ITEMS))
        fnImportData.apply(exportData, importSettings, importItems).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      } else {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected failure occurred during the import process.")
        }
      }
    }.flatten.andThen { _ =>
      FileUtils.deleteQuietly(pendingFolder.toFile)
    }
  }

  private def confirmDomainImportInternal(request: Request[AnyContent], domainId: Long, canAddOrDeleteDomain: Boolean): Future[Result] = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeDomainImport(export.getDomains.getDomain.asScala.head, importSettings, importItems, emptyForNegativeId(domainId), canAddOrDeleteDomain)
    })
  }

  private def confirmCommunityImportInternal(request: Request[AnyContent], communityId: Long, canDoAdminOperations: Boolean): Future[Result] = {
    confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
      importCompleteManager.completeCommunityImport(export, importSettings, importItems, emptyForNegativeId(communityId), canDoAdminOperations, Some(ParameterExtractor.extractUserId(request)))
    })
  }

  def confirmDomainImportTestBedAdmin(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateDomain(request).flatMap { _ =>
      confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = true)
    }
  }

  def confirmDomainImportCommunityAdmin(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      confirmDomainImportInternal(request, domainId, canAddOrDeleteDomain = false)
    }
  }

  def confirmCommunityImportTestBedAdmin(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateCommunity(request).flatMap { _ =>
      confirmCommunityImportInternal(request, communityId, canDoAdminOperations = true)
    }
  }

  def confirmCommunityImportCommunityAdmin(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      confirmCommunityImportInternal(request, communityId, canDoAdminOperations = false)
    }
  }

  def confirmSystemSettingsImport: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystemSettings(request).flatMap { _ =>
      confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
        importCompleteManager.completeSystemSettingsImport(export.getSettings, importSettings, importItems, canManageSettings = true, Some(ParameterExtractor.extractUserId(request)))
      })
    }
  }

  def confirmDeletionsImport: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteAnyDomain(request).flatMap { _ =>
      confirmImportInternal(request, (export: Export, importSettings: ImportSettings, importItems: List[ImportItem]) => {
        importCompleteManager.completeDeletionsImport(export.getDeletions, importSettings, importItems)
      })
    }
  }

  def applySandboxData(): Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = authorizedAction.async(parse.multipartFormData) { request =>
    authorizationManager.canApplySandboxDataMulti(request).flatMap { _ =>
      val archivePassword = ParameterExtractor.requiredBodyParameterMulti(request, Parameters.PASSWORD)
      request.body.file(Parameters.FILE) match {
        case Some(archive) =>
          val archiveFile = archive.ref.path.toFile
          importCompleteManager.importSandboxData(archiveFile, archivePassword).flatMap { importResult =>
            if (importResult.processingComplete) {
              // Successful - prevent other imports to take place and return
              repositoryUtils.createDataLockFile()
              // The default theme may have changed.
              systemConfigurationManager.reloadThemeCss().map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            } else {
              // Unsuccessful.
              Future.successful {
                val message = importResult.errorMessage.getOrElse("An error occurred while processing the archive")
                ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, message)
              }
            }
          }.andThen { _ =>
            FileUtils.deleteQuietly(archiveFile)
          }
        case None =>
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
          }
      }
    }
  }

  private def getReportTempFile(suffix: String): Path = {
    repositoryUtils.getReportTempFile(suffix)
  }

}
