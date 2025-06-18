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
import config.Configurations
import controllers.HealthCheckService.TestEngineCheckResult
import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, SystemConfigurationManager, TestbedBackendClient}
import models.Enums.ServiceHealthStatusType
import models.{Constants, EmailSettings, ServiceHealthInfo}
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.signature.ValidationTimeStamp
import utils.{ClamAVClient, JsonUtil}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

object HealthCheckService {

  case class TestEngineCheckResult(success: Boolean, message: String, error: Option[String] = None) {

    def errorToUse(): String = {
      error.flatMap(x => if (StringUtils.isBlank(x)) None else Some(x)).getOrElse("No error to report.")
    }

  }

}

class HealthCheckService @Inject()(authorizedAction: AuthorizedAction,
                                   cc: ControllerComponents,
                                   authorizationManager: AuthorizationManager,
                                   systemConfigurationManager: SystemConfigurationManager,
                                   testbedClient: TestbedBackendClient)
                                  (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val LOGGER = LoggerFactory.getLogger(classOf[HealthCheckService])

  def checkAntivirusService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo: ServiceHealthInfo = if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
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
      ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
    }
  }

  def checkEmailService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
      val healthInfoTask: Future[ServiceHealthInfo] = if (Configurations.EMAIL_ENABLED) {
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
      }
      healthInfoTask.map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  def checkTrustedTimestampService(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo = if (Configurations.TSA_SERVER_ENABLED) {
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
      ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
    }
  }

  def checkUserInterfaceCommunicationErrorDetails(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo = ServiceHealthInfo(ServiceHealthStatusType.Error,
        "User interface communications are not working correctly.",
        readClasspathResource("health/ws/error.md").formatted(Configurations.TESTBED_HOME_LINK, Configurations.PUBLIC_CONTEXT_ROOT, Configurations.WEB_CONTEXT_ROOT)
      )
      ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
    }
  }

  def checkUserInterfaceCommunicationSuccessDetails(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).map { _ =>
      val healthInfo = ServiceHealthInfo(ServiceHealthStatusType.Ok,
        "User interface communications are working correctly.",
        readClasspathResource("health/ws/ok.md")
      )
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
      testbedClient.healthCheck("callbacks").map { callbackUrl =>
        ServiceHealthInfo(ServiceHealthStatusType.Info,
          "Test engine callbacks appear to be working correctly.",
          readClasspathResource("health/callbacks/info.md").formatted(callbackUrl)
        )
      }.recover {
        case e: Exception =>
          val errors = errorsToString(systemConfigurationManager.extractFailureDetails(e))
          ServiceHealthInfo(ServiceHealthStatusType.Error,
            "Test engine callbacks are not working correctly.",
            readClasspathResource("health/callbacks/error.md").formatted(errors)
          )
      }.map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
  }

  def checkTestEngineCommunication(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCheckCoreServiceHealth(request).flatMap { _ =>
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
      }.map { healthInfo =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsServiceHealthInfo(healthInfo).toString)
      }
    }
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
