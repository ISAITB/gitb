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

package controllers

import com.gitb.utils.HmacUtils
import com.nimbusds.jose.crypto.{ECDSAVerifier, RSASSAVerifier}
import com.nimbusds.jose.jwk.{ECKey, JWKSet, RSAKey}
import com.nimbusds.jose.{JWSObjectJSON, JWSVerifier}
import config.Configurations
import controllers.HealthCheckService.HealthCheckType.HealthCheckType
import controllers.HealthCheckService._
import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, SystemConfigurationManager, TestbedBackendClient}
import models.Enums.ServiceHealthStatusType.ServiceHealthStatusType
import models.Enums.{ReleaseMessageSeverity, ServiceHealthStatusType}
import models.health.ReleaseMessage
import models.{Constants, EmailSettings, ServiceHealthInfo}
import org.apache.commons.lang3.{StringUtils, Strings}
import org.apache.pekko.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import utils.signature.ValidationTimeStamp
import utils.{ClamAVClient, JsonUtil}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.atomic.AtomicReference
import java.util.{Objects, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Using}

object HealthCheckService {

  case class TestEngineCheckResult(success: Boolean, message: String, error: Option[String] = None) {

    def errorToUse(): String = {
      error.flatMap(x => if (StringUtils.isBlank(x)) None else Some(x)).getOrElse("No error to report.")
    }

  }

  case class ResponseInfo(status: Int, payload: String) {}
  private case class CachedHealthStatus(calculationTime: Option[Instant], status: ServiceHealthStatusType.ServiceHealthStatusType)

  object HealthCheckType extends Enumeration(1) {
    type HealthCheckType = Value
    val AntivirusService, EmailService, TrustedTimestampService, UserInterfaceCommunication, TestEngineCallbacks, TestEngineCommunication, SoftwareVersion = Value
  }

  private val currentStatusMap = new AtomicReference[Map[HealthCheckType, CachedHealthStatus]](
    Map(
      HealthCheckType.AntivirusService -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.EmailService -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.TrustedTimestampService -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.UserInterfaceCommunication -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.TestEngineCallbacks -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.TestEngineCommunication -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown),
      HealthCheckType.SoftwareVersion -> CachedHealthStatus(None, ServiceHealthStatusType.Unknown)
    )
  )

  private val lastOverallStatusCalculationTime = new AtomicReference[Option[Instant]](None)

}

class HealthCheckService @Inject()(authorizedAction: AuthorizedAction,
                                   cc: ControllerComponents,
                                   authorizationManager: AuthorizationManager,
                                   systemConfigurationManager: SystemConfigurationManager,
                                   ws: WSClient,
                                   testbedClient: TestbedBackendClient)
                                  (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val LOGGER = LoggerFactory.getLogger(classOf[HealthCheckService])

  private def updateHealthStatus(checkType: HealthCheckType, status: ServiceHealthStatusType): Unit = {
    var done = false
    while (!done) {
      val current = currentStatusMap.get()
      val updatedMap = current + (checkType -> CachedHealthStatus(Some(Instant.now()), status))
      done = currentStatusMap.compareAndSet(current, updatedMap)
    }
  }

  private def updateOverallHealthStatus(): Unit = {
    var done = false
    while (!done) {
      val current = lastOverallStatusCalculationTime.get()
      done = lastOverallStatusCalculationTime.compareAndSet(current, Some(Instant.now()))
    }
  }

  private def determineCachedOverallStatus(): ServiceHealthStatusType = {
    determineStatus(currentStatusMap.get().values.map(_.status))
  }

  private def determineStatus(statusList: Iterable[ServiceHealthStatusType]): ServiceHealthStatusType = {
    var hasError = false
    var hasWarning = false
    var hasInfo = false
    var hasOk = false
    statusList.foreach {
      case ServiceHealthStatusType.Error => hasError = true
      case ServiceHealthStatusType.Warning => hasWarning = true
      case ServiceHealthStatusType.Info => hasInfo = true
      case ServiceHealthStatusType.Ok => hasOk = true
      case _ => // Nothing needed
    }
    if (hasError) {
      ServiceHealthStatusType.Error
    } else if (hasWarning) {
      ServiceHealthStatusType.Warning
    } else if (hasInfo) {
      ServiceHealthStatusType.Info
    } else if (hasOk) {
      ServiceHealthStatusType.Ok
    } else {
      ServiceHealthStatusType.Unknown
    }
  }

  def runPostLoginChecks(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      val cachedStatus = lastOverallStatusCalculationTime.get()
      // Instant.now().minus(1, ChronoUnit.HOURS))
      val task = if (cachedStatus.exists(calculationTime => calculationTime.isAfter(Instant.now().minus(1, ChronoUnit.HOURS)))) {
        // Within 1 hour of cached status update time - use cached status
        LOGGER.debug("Returning cached")
        Future.successful(determineCachedOverallStatus())
      } else {
        LOGGER.debug("Calculating status")
        // Recalculate status
        val checks = List(
          checkAntivirusServiceInternal(),
          checkTrustedTimestampServiceInternal(),
          checkEmailServiceInternal(),
          checkSoftwareVersionInternal(),
          checkTestEngineCallbacksInternal(),
          checkTestEngineCommunicationInternal()
        )
        Future.sequence(checks).map { _ =>
          determineCachedOverallStatus()
        }.recover {
          case e: Exception =>
            LOGGER.warn("Unexpected failure while running post-login health checks", e)
            ServiceHealthStatusType.Unknown
        }.andThen {
          case Success(_) => updateOverallHealthStatus()
        }
      }
      task.map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthStatus(healthInfo).toString)
      }
    }
  }

  def checkAntivirusService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkAntivirusServiceInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkAntivirusServiceInternal(): Future[ServiceHealthInfo] = {
    Future.successful {
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        try {
          val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
          Using.resource(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8))) { stream =>
            virusScanner.scan(stream)
            ServiceHealthInfo(ServiceHealthStatusType.Ok,
              "The antivirus scanning service is enabled and working correctly.",
              readClasspathResource("health/antivirus/ok.md").formatted(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            )
          }
        } catch {
          case e: Exception =>
            val errors = errorsToString(systemConfigurationManager.extractFailureDetails(e))
            ServiceHealthInfo(ServiceHealthStatusType.Error, "The antivirus scanning service is not working correctly.",
              readClasspathResource("health/antivirus/error.md").formatted(errors, Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            )
        }
      } else {
        if (Configurations.TESTBED_MODE == Constants.DevelopmentMode) {
          ServiceHealthInfo(ServiceHealthStatusType.Info,
            "The antivirus scanning service is disabled.",
            readClasspathResource("health/antivirus/info.md")
          )
        } else {
          ServiceHealthInfo(ServiceHealthStatusType.Warning,
            "The antivirus scanning service is disabled.",
            readClasspathResource("health/antivirus/warning.md")
          )
        }
      }
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.AntivirusService, value.status)
    }
  }

  def checkEmailService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkEmailServiceInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkEmailServiceInternal(): Future[ServiceHealthInfo] = {
    if (Configurations.EMAIL_ENABLED) {
      systemConfigurationManager.testEmailSettings(EmailSettings.fromEnvironment(), UUID.randomUUID().toString+"@itb.ec.europa.eu").map { errors =>
        if (errors.isEmpty) {
          // Ok
          val contactFormText = if (Configurations.EMAIL_CONTACT_FORM_ENABLED.getOrElse(false)) {
            ""
          } else {
            "Note that the **email contact form** is currently disabled. You could consider enabling this to allow email-based support for users from within the Test Bed."
          }
          ServiceHealthInfo(ServiceHealthStatusType.Ok,
            "The email (SMTP) service is enabled and working correctly.",
            readClasspathResource("health/email/ok.md").formatted(contactFormText, Configurations.EMAIL_SMTP_HOST.get, Configurations.EMAIL_SMTP_PORT.get)
          )
        } else {
          // Error
          val contactFormText = if (Configurations.EMAIL_CONTACT_FORM_ENABLED.getOrElse(false)) {
            "enabled"
          } else {
            "disabled"
          }
          ServiceHealthInfo(ServiceHealthStatusType.Error,
            "The email (SMTP) service is not working correctly.",
            readClasspathResource("health/email/error.md").formatted(errorsToString(errors.get), contactFormText)
          )
        }
      }
    } else {
      Future.successful {
        if (Configurations.TESTBED_MODE == Constants.DevelopmentMode) {
          // Info
          ServiceHealthInfo(ServiceHealthStatusType.Info,
            "The email (SMTP) service is disabled.",
            readClasspathResource("health/email/info_dev.md")
          )
        } else {
          // Info
          ServiceHealthInfo(ServiceHealthStatusType.Info,
            "The email (SMTP) service is disabled.",
            readClasspathResource("health/email/info_prod.md")
          )
        }
      }
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.EmailService, value.status)
    }
  }

  def checkTrustedTimestampService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkTrustedTimestampServiceInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkTrustedTimestampServiceInternal(): Future[ServiceHealthInfo] = {
    Future.successful{
      if (Configurations.TSA_SERVER_ENABLED) {
        try {
          val tsaClient = new ValidationTimeStamp(Configurations.TSA_SERVER_URL)
          Using.resource(new ByteArrayInputStream("TEST".getBytes(StandardCharsets.UTF_8))) { stream =>
            val signatureBytes = tsaClient.getTimeStampToken(stream)
            if (signatureBytes == null || signatureBytes.isEmpty) {
              throw new IllegalStateException("The TSA server returned an empty timestamp")
            }
          }
          ServiceHealthInfo(ServiceHealthStatusType.Ok,
            "The trusted timestamp service is enabled and working correctly.",
            readClasspathResource("health/tsa/ok.md").formatted(Configurations.TSA_SERVER_URL)
          )
        } catch {
          case e: Exception =>
            val errors = errorsToString(systemConfigurationManager.extractFailureDetails(e))
            ServiceHealthInfo(ServiceHealthStatusType.Error,
              "The trusted timestamp service is not working correctly.",
              readClasspathResource("health/tsa/error.md").formatted(errors, Configurations.TSA_SERVER_URL)
            )
        }
      } else {
        ServiceHealthInfo(ServiceHealthStatusType.Info,
          "The trusted timestamp service is disabled.",
          readClasspathResource("health/tsa/info.md")
        )
      }
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.TrustedTimestampService, value.status)
    }
  }

  def checkUserInterfaceCommunicationErrorDetails(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo = ServiceHealthInfo(ServiceHealthStatusType.Error,
        "User interface communications are not working correctly.",
        readClasspathResource("health/ws/error.md").formatted(Configurations.TESTBED_HOME_LINK, Configurations.PUBLIC_CONTEXT_ROOT, Configurations.WEB_CONTEXT_ROOT)
      )
      updateHealthStatus(HealthCheckType.UserInterfaceCommunication, healthInfo.status)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
    }
  }

  def checkUserInterfaceCommunicationSuccessDetails(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo = ServiceHealthInfo(ServiceHealthStatusType.Ok,
        "User interface communications are working correctly.",
        readClasspathResource("health/ws/ok.md")
      )
      updateHealthStatus(HealthCheckType.UserInterfaceCommunication, healthInfo.status)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
    }
  }

  def checkUserInterfaceCommunication: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { _ =>
    Future {
      Right(
        Flow[JsValue]
          .take(1)
          .map { clientMessage =>
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Websocket received: {}", clientMessage)
            }
            val parsedMessage = (clientMessage \ "msg").asOpt[String]
            parsedMessage.map {
              // Expecting the text "test"
              case "test" => Json.obj("msg" -> "OK")
              case _ => Json.obj("msg" -> "NOK")
            }.getOrElse(Json.obj("msg" -> "NOK"))
          }
          .concat(org.apache.pekko.stream.scaladsl.Source.single(Json.obj("msg" -> "Closing...")))
          .watchTermination() { (_, termination) =>
            termination.foreach(_ => LOGGER.debug("WebSocket closed"))
            Flow[JsValue]
          }
      )
    }
  }

  def checkTestEngineCallbacks(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkTestEngineCallbacksInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkTestEngineCallbacksInternal(): Future[ServiceHealthInfo] = {
    testbedClient.healthCheck("callbacks").map { callbackUrl =>
      if (callbackUrl.startsWith("http://localhost") || callbackUrl.startsWith("https://localhost")) {
        ServiceHealthInfo(ServiceHealthStatusType.Info,
          "Test engine callbacks are limited to localhost calls.",
          readClasspathResource("health/callbacks/info.md").formatted(callbackUrl)
        )
      } else {
        ServiceHealthInfo(ServiceHealthStatusType.Ok,
          "Test engine callbacks appear to be working correctly.",
          readClasspathResource("health/callbacks/ok.md").formatted(callbackUrl)
        )
      }
    }.recover {
      case e: Exception =>
        val errors = errorsToString(systemConfigurationManager.extractFailureDetails(e))
        ServiceHealthInfo(ServiceHealthStatusType.Error,
          "Test engine callbacks are not working correctly.",
          readClasspathResource("health/callbacks/error.md").formatted(errors)
        )
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.TestEngineCallbacks, value.status)
    }
  }

  def checkTestEngineCommunication(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkTestEngineCommunicationInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkTestEngineCommunicationInternal(): Future[ServiceHealthInfo] = {
    /*
     * Things to check:
     * - Mismatch between WEB_CONTENT_ROOT (gitb-ui) and REPOSITORY_ROOT_URL (gitb-srv)
     * - The HMAC_KEY in gitb-ui and gitb-srv does not match.
     * - Networking issue between gitb-ui and gitb-srv.
     * - Networking issue between gitb-srv and gitb-ui.
     */
    testbedClient.healthCheck("engine").map { result =>
      val testEngineResult = JsonUtil.parseJsTestEngineHealthCheckResponse(result)
      // If we are here it means that gitb-ui got a response from gitb-srv.
      val uiToSrv = TestEngineCheckResult(success = true, "success")
      // Test gitb-srv to gitb-ui (callbacks)
      val srvToUiCallback = testEngineResult.items.find(result => result.name == "tbs").map { x =>
        TestEngineCheckResult(x.status, if (x.status) "success" else "failure", x.message)
      }.getOrElse(TestEngineCheckResult(success = false, "unknown"))
      // Test gitb-srv to gitb-ui (lookups)
      val srvToUiLookup = testEngineResult.items.find(result => result.name == "repo").map { x =>
        TestEngineCheckResult(x.status, if (x.status) "success" else "failure", x.message)
      }.getOrElse(TestEngineCheckResult(success = false, "unknown"))
      if (uiToSrv.success && srvToUiCallback.success && srvToUiLookup.success) {
        ServiceHealthInfo(ServiceHealthStatusType.Ok,
          "Test engine communications are working correctly.",
          readClasspathResource("health/engine/ok.md")
        )
      } else {
        ServiceHealthInfo(ServiceHealthStatusType.Error,
          "Test engine communications are not working correctly.",
          readClasspathResource("health/engine/error.md").formatted(
            uiToSrv.message, uiToSrv.errorToUse(),
            srvToUiCallback.message, srvToUiCallback.errorToUse(),
            srvToUiLookup.message, srvToUiLookup.errorToUse(),
            Configurations.WEB_CONTEXT_ROOT, testEngineResult.repositoryUrl,
            hmacKeysMatchMessage(testEngineResult.hmacHash)
          )
        )
      }
    }.recover {
      case e: Exception =>
        val uiToSrv = TestEngineCheckResult(success = false, "failure", Some(errorsToString(systemConfigurationManager.extractFailureDetails(e))))
        val srvToUi = TestEngineCheckResult(success = false, "unknown")
        ServiceHealthInfo(ServiceHealthStatusType.Error,
          "Test engine communications are not working correctly.",
          readClasspathResource("health/engine/error.md").formatted(
            uiToSrv.message, uiToSrv.errorToUse(),
            srvToUi.message, srvToUi.errorToUse(),
            srvToUi.message, srvToUi.errorToUse(),
            Configurations.WEB_CONTEXT_ROOT, "N/A",
            "can't be checked"
          )
        )
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.TestEngineCommunication, value.status)
    }
  }

  def checkSoftwareVersion(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      checkSoftwareVersionInternal().map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  private def checkSoftwareVersionInternal(): Future[ServiceHealthInfo] = {
    val task = if (Configurations.SOFTWARE_VERSION_CHECK_ENABLED) {
      for {
        jwsContent <- ws.url(Configurations.SOFTWARE_VERSION_CHECK_INFO_URL)
          .addHttpHeaders("Content-Type" -> Constants.MimeTypeJSON)
          .withFollowRedirects(true)
          .get()
          .map(x => ResponseInfo(x.status, x.body))
        jwksContent <- ws.url(Configurations.SOFTWARE_VERSION_CHECK_JWKS_URL)
          .addHttpHeaders("Content-Type" -> Constants.MimeTypeJSON)
          .withFollowRedirects(true)
          .get()
          .map(x => ResponseInfo(x.status, x.body))
        healthInfo <- {
          val healthInfo = if (jwsContent.status >= 400 || jwksContent.status >= 400) {
            // Some error in the lookup occurred.
            val details = new StringBuilder()
            if (jwsContent.status >= 400) {
              details.append("Status retrieval: GET to %s resulted in status %s\n".formatted(Configurations.SOFTWARE_VERSION_CHECK_INFO_URL, jwsContent.status))
            }
            if (jwksContent.status >= 400) {
              details.append("Key retrieval: GET to %s resulted in status %s\n".formatted(Configurations.SOFTWARE_VERSION_CHECK_JWKS_URL, jwksContent.status))
            }
            details.deleteCharAt(details.length() - 1)
            ServiceHealthInfo(ServiceHealthStatusType.Warning,
              "The software version information could not be retrieved.",
              readClasspathResource("health/software/warning_lookup.md").formatted(details)
            )
          } else {
            // Load the JWS
            val jwsObject = JWSObjectJSON.parse(jwsContent.payload);
            // Load the JWKS
            val jwkSet = JWKSet.parse(jwksContent.payload)
            // Verify signature(s)
            val verified = jwsObject.getSignatures().stream().allMatch(signature => {
              val verifier: JWSVerifier = Objects.requireNonNull(jwkSet.getKeyByKeyId(signature.getHeader().getKeyID()), "Signature key not found in key set") match {
                case rsaKey: RSAKey => new RSASSAVerifier(rsaKey.toRSAPublicKey())
                case ecKey: ECKey => new ECDSAVerifier(ecKey.toECPublicKey())
                case _ => throw new IllegalStateException("Unsupported JWS key type")
              }
              signature.verify(verifier)
            })
            if (verified) {
              // Extract software information
              val softwareInfo = JsonUtil.parseJsSoftwareVersionInfo(Json.parse(jwsObject.getPayload().toString()))
              val releaseNoteContent = softwareInfo.latest.releaseNotes.map(x => " (see [release notes](%s))".formatted(x)).getOrElse("")
              val releaseDateContent = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ZonedDateTime.ofInstant(softwareInfo.latest.releaseDate, ZoneOffset.UTC))
              // Parse current release's timestamp
              val currentBuildTime = LocalDateTime.parse(Configurations.BUILD_TIMESTAMP, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC)
              if (currentBuildTime.isBefore(softwareInfo.latest.releaseDate)) {
                // A more recent stable release exists.
                val currentBaseVersion = Configurations.mainVersionNumber()
                // Get the reports (if any) that apply to the current version.
                val reports = softwareInfo.reports.flatMap { reports =>
                  reports.find { releaseMessages =>
                    releaseMessages.release.releaseNumber == currentBaseVersion
                  }.map(_.messages)
                }
                if (reports.exists(_.nonEmpty)) {
                  ServiceHealthInfo(ServiceHealthStatusType.Warning,
                    "There are known issues affecting the Test Bed release you are using.",
                    readClasspathResource("health/software/warning_reports.md").formatted(Constants.VersionNumber, buildReleaseReportList(reports.get), softwareInfo.latest.releaseNumber, releaseDateContent, releaseNoteContent)
                  )
                } else {
                  ServiceHealthInfo(ServiceHealthStatusType.Info,
                    "A new Test Bed release is available.",
                    readClasspathResource("health/software/info.md").formatted(softwareInfo.latest.releaseNumber, releaseDateContent, releaseNoteContent)
                  )
                }
              } else {
                ServiceHealthInfo(ServiceHealthStatusType.Ok,
                  "The Test Bed software is up to date.",
                  readClasspathResource("health/software/ok.md").formatted(softwareInfo.latest.releaseNumber, releaseDateContent, releaseNoteContent)
                )
              }
            } else {
              ServiceHealthInfo(ServiceHealthStatusType.Warning,
                "The software version information could not be verified.",
                readClasspathResource("health/software/warning_integrity.md")
              )
            }
          }
          Future.successful(healthInfo)
        }
      } yield healthInfo
    } else {
      Future.successful {
        ServiceHealthInfo(ServiceHealthStatusType.Info,
          "The software version check is disabled.",
          readClasspathResource("health/software/info_disabled.md")
        )
      }
    }
    task.recover {
      case e: Exception =>
        LOGGER.warn("Unexpected error while checking the software version status", e)
        val errors = errorsToString(systemConfigurationManager.extractFailureDetails(e))
        ServiceHealthInfo(ServiceHealthStatusType.Warning,
          "The software update information could not be retrieved.",
          readClasspathResource("health/software/warning_failure.md").formatted(errors)
        )
    }.andThen {
      case Success(value) => updateHealthStatus(HealthCheckType.SoftwareVersion, value.status)
    }
  }

  private def buildReleaseReportList(reports: Iterable[ReleaseMessage]): String = {
    val msg = new StringBuilder()
    reports.foreach { report =>
      msg.append("- **[%s severity]** %s%s\n".formatted(
          report.messageSeverity.getOrElse(ReleaseMessageSeverity.Low),
          Strings.CS.removeEnd(report.message, "."),
          report.moreInformation.map(x => " (see [details](%s)).".formatted(x)).getOrElse(".")
        )
      )
    }
    msg.toString()
  }

  private def hmacKeysMatchMessage(receivedHash: String): String = {
    if (HmacUtils.getHashedKey == receivedHash) {
      "match"
    } else {
      "do not match"
    }
  }

  private def errorsToString(errorLines: List[String]): String = {
    val buffer = new StringBuilder()
    var lineNumber = -1
    errorLines.foreach { line =>
      lineNumber += 1
      if (buffer.nonEmpty) {
        val padding = "   " * (lineNumber-1)
        buffer.append('\n').append(padding).append("└─ ")
      }
      buffer.append(line)
    }
    buffer.toString()
  }

  private def readClasspathResource(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    if (stream == null) throw new RuntimeException(s"Resource not found: $path")
    scala.io.Source.fromInputStream(stream).mkString
  }

}
