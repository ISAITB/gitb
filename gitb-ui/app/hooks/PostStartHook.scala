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

package hooks

import com.gitb.utils.HmacUtils
import config.Configurations
import config.Configurations.{BUILD_TIMESTAMP, DB_PASSWORD, MASTER_PASSWORD}
import jakarta.xml.ws.Endpoint
import jaxws.TestbedService
import managers._
import managers.export.ImportCompleteManager
import models.Constants
import models.Enums.UserRole
import org.apache.commons.io.FileUtils
import org.apache.commons.io.comparator.NameFileComparator
import org.apache.commons.lang3.StringUtils
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.pekko.actor.{ActorSystem, Cancellable}
import org.slf4j.LoggerFactory
import play.api.{Configuration, Environment}
import utils._

import java.io.{File, FileFilter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.LocalDate
import java.util
import java.util.Properties
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Using

@Singleton
class PostStartHook @Inject() (authenticationManager: AuthenticationManager,
                               actorSystem: ActorSystem,
                               systemConfigurationManager: SystemConfigurationManager,
                               testResultManager: TestResultManager,
                               testExecutionManager: TestExecutionManager,
                               importCompleteManager: ImportCompleteManager,
                               repositoryUtils: RepositoryUtils,
                               environment: Environment,
                               userManager: UserManager,
                               config: Configuration)
                              (implicit ec: ExecutionContext) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  onStart()

  def onStart(): Unit = {
    if (!Configurations.STARTUP_FAILURE) {
      Await.result(
        (
          for {
            _ <- Future.successful {
              logger.info("Starting Application")
              System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
              BUILD_TIMESTAMP = getBuildTimestamp()
            }
            _ <- checkOperationMode()
            _ <- initialiseTestbedClient()
            _ <- checkMasterPassword()
            _ <- loadDataExports()
            _ <- adaptSystemConfiguration()
            _ <- destroyIdleSessions()
            _ <- deleteInactiveUserAccounts()
            _ <- cleanupPendingTestSuiteUploads()
            _ <- cleanupTempFiles()
            _ <- archiveOldTestSessions()
            _ <- prepareRestApiDocumentation()
            _ <- prepareTheme()
            _ <- setupAdministratorOneTimePassword()
            _ <- setupInteractionNotifications()
            _ <- setupSsoMigrationModeIfNeeded()
            _ <- removeOverrideConfiguration()
          } yield ()
        ).recover {
          case e: Exception =>
            Configurations.STARTUP_FAILURE = true
            throw e
        }.andThen { _ =>
          completeStartup()
        }, Duration.Inf)
    } else {
      completeStartup()
    }
  }

  private def completeStartup(): Unit = {
    if (Configurations.STARTUP_FAILURE) {
      logger.error("Application failed to start")
    } else {
      var banner = ""
      Using.resource(Thread.currentThread().getContextClassLoader.getResourceAsStream("banner.txt")) { stream =>
        banner = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
      }
      logger.info("Web context root is [{}], public context root is [{}] and public home link is [{}]", Configurations.WEB_CONTEXT_ROOT, Configurations.PUBLIC_CONTEXT_ROOT, Configurations.TESTBED_HOME_LINK)
      logger.info("Started ITB frontend (itb-ui) in {} mode - release {} ({})\n{}", Configurations.TESTBED_MODE, Constants.VersionNumber, Configurations.BUILD_TIMESTAMP, banner)
    }
  }

  private def checkOperationMode(): Future[Unit] = {
    Future.successful {
      val modeSetting = sys.env.get("TESTBED_MODE")
      if (modeSetting.isDefined && modeSetting.get == Constants.DevelopmentMode) {
        // Explicitly set to development mode.
        Configurations.TESTBED_MODE = Constants.DevelopmentMode
      } else {
        val secretsNotForProduction = secretsSetForDevelopment()
        if (modeSetting.isDefined && modeSetting.get == Constants.ProductionMode) {
          if (secretsNotForProduction) {
            // Production mode with dev-level secrets - Fail the start-up.
            throw new IllegalStateException("Your application is running in production mode with default values set for sensitive configuration properties. Switch to development mode by setting on gitb-ui the TESTBED_MODE environment variable to \""+Constants.DevelopmentMode+"\" or replace these settings accordingly. For more details refer to the test bed's production installation guide.")
          } else {
            // All ok.
            Configurations.TESTBED_MODE = Constants.ProductionMode
          }
        } else {
          // Sandbox or other non-production mode.
          Configurations.TESTBED_MODE = Constants.DevelopmentMode
          if (secretsNotForProduction) {
            // Log a warning.
            logger.warn("Your application is running with default values set for sensitive configuration properties. Replace these settings accordingly and switch to production mode by setting on gitb-ui the TESTBED_MODE environment variable to \""+Constants.ProductionMode+"\". For more details refer to the test bed's production installation guide.")
          }
        }
      }
    }
  }

  private def secretsSetForDevelopment(): Boolean = {
    val appSecret = config.get[String]("play.http.secret.key")
    val dbPassword = DB_PASSWORD
    val hmacKey = HmacUtils.getKey
    val masterPassword = String.valueOf(MASTER_PASSWORD)
    appSecret == "value_used_during_development_to_be_replaced_in_production" || appSecret == "CHANGE_ME" ||
      masterPassword == "value_used_during_development_to_be_replaced_in_production" || masterPassword == "CHANGE_ME" ||
      dbPassword == "gitb" || dbPassword == "CHANGE_ME" ||
      hmacKey == "devKey" || hmacKey == "CHANGE_ME"
  }

  private def adaptSystemConfiguration(): Future[Unit] = {
    // Record the default settings (from the config, fixed values and the environment).
    val defaultEmailSettings = systemConfigurationManager.recordDefaultEmailSettings()
    // Load persisted configuration parameters.
    systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true).flatMap { persistedConfigs =>
      // Check against environment settings.
      val restApiEnabledConfig = persistedConfigs.find(config => config.config.name == Constants.RestApiEnabled).map(_.config)
      for {
        // REST API.
        _ <- {
          if (restApiEnabledConfig.nonEmpty && restApiEnabledConfig.get.parameter.nonEmpty) {
            Configurations.AUTOMATION_API_ENABLED = restApiEnabledConfig.get.parameter.get.toBoolean
          }
          val restApiAdminKey = persistedConfigs.find(config => config.config.name == Constants.RestApiAdminKey).map(_.config)
          if (restApiAdminKey.flatMap(_.parameter).isEmpty) {
            val initialApiKeyValue = Configurations.AUTOMATION_API_MASTER_KEY.getOrElse(CryptoUtil.generateApiKey())
            systemConfigurationManager.updateSystemParameter(Constants.RestApiAdminKey, Some(initialApiKeyValue))
          } else {
            Future.successful(())
          }
        }
        // Self-registration.
        _ <- {
          val selfRegistrationConfig = persistedConfigs.find(config => config.config.name == Constants.SelfRegistrationEnabled).map(_.config)
          if (selfRegistrationConfig.nonEmpty && selfRegistrationConfig.get.parameter.nonEmpty) {
            Configurations.REGISTRATION_ENABLED = selfRegistrationConfig.get.parameter.get.toBoolean
          }
          Future.successful(())
        }
        // Demo account.
        _ <- {
          val demoAccountConfig = persistedConfigs.find(config => config.config.name == Constants.DemoAccount).map(_.config)
          Configurations.DEMOS_ENABLED = demoAccountConfig.nonEmpty && demoAccountConfig.get.parameter.nonEmpty
          if (Configurations.DEMOS_ENABLED) {
            Configurations.DEMOS_ACCOUNT = demoAccountConfig.get.parameter.get.toLong
          } else {
            Configurations.DEMOS_ACCOUNT = -1
          }
          // Make sure that the configured demo account is an existing basic user.
          if (Configurations.DEMOS_ACCOUNT != -1) {
            userManager.getUserById(Configurations.DEMOS_ACCOUNT).map { demoUser =>
              if (demoUser.isEmpty || demoUser.get.role != UserRole.VendorUser.id.toShort) {
                logger.warn("Configured demo account ID [{}] does not match a non-administrator user and will be ignored.", Configurations.DEMOS_ACCOUNT)
                Configurations.DEMOS_ENABLED = false
                Configurations.DEMOS_ACCOUNT = -1
              }
            }
          } else {
            Future.successful(())
          }
        }
        // Welcome message.
        _ <- {
          val welcomeMessageConfig = persistedConfigs.find(config => config.config.name == Constants.WelcomeMessage).map(_.config)
          if (welcomeMessageConfig.nonEmpty && welcomeMessageConfig.get.parameter.nonEmpty) {
            Configurations.WELCOME_MESSAGE = welcomeMessageConfig.get.parameter.get
          } else {
            Configurations.WELCOME_MESSAGE = Configurations.WELCOME_MESSAGE_DEFAULT
          }
          Future.successful(())
        }
        // Email settings.
        _ <- {
          val emailSettings = persistedConfigs.find(config => config.config.name == Constants.EmailSettings).map(_.config)
          if (emailSettings.nonEmpty && emailSettings.get.parameter.nonEmpty) {
            JsonUtil.parseJsEmailSettings(emailSettings.get.parameter.get).toEnvironment(Some(defaultEmailSettings))
          }
          Future.successful(())
        }
      } yield ()
    }
  }

  private def getBuildTimestamp(): String = {
    var timestamp = ""
    Using.resource(environment.classLoader.getResourceAsStream("core-module.properties")) { stream =>
      val props = new Properties()
      props.load(stream)
      timestamp = props.getOrDefault("gitb.buildTimestamp", "").asInstanceOf[String]
    }
    timestamp
  }

  private def checkMasterPassword(): Future[Unit] = {
    for {
      existingHash <- systemConfigurationManager.getSystemConfiguration(Constants.MasterPassword)
      existingValue <- {
        if (existingHash.isEmpty || existingHash.get.parameter.isEmpty) {
          // Store master password.
          val existingValue = Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD)))
          systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, existingValue).map { _ =>
            existingValue
          }
        } else {
          Future.successful(existingHash.get.parameter)
        }
      }
      _ <- {
        if (CryptoUtil.checkPassword(String.valueOf(Configurations.MASTER_PASSWORD), existingValue.get)) {
          // Set master password matches stored value.
          if (Configurations.MASTER_PASSWORD_TO_REPLACE.isDefined) {
            Configurations.MASTER_PASSWORD_TO_REPLACE = None
            logger.warn("You have configured property MASTER_PASSWORD_TO_REPLACE, however the configured MASTER_PASSWORD " +
              "matches the one currently in place. Property MASTER_PASSWORD_TO_REPLACE will be ignored. To silence this warning " +
              "remove property MASTER_PASSWORD_TO_REPLACE.")
          }
          if (Configurations.MASTER_PASSWORD_FORCE) {
            logger.warn("You have set property MASTER_PASSWORD_FORCE to true, however the configured MASTER_PASSWORD " +
              "matches the one currently in place. To silence this warning remove property MASTER_PASSWORD_FORCE or set it to false.")
          }
          Future.successful(())
        } else {
          // Set master password does not match stored value.
          if (Configurations.MASTER_PASSWORD_TO_REPLACE.isDefined) {
            if (CryptoUtil.checkPassword(String.valueOf(Configurations.MASTER_PASSWORD_TO_REPLACE.get), existingValue.get)) {
              systemConfigurationManager.updateMasterPassword(Configurations.MASTER_PASSWORD_TO_REPLACE.get, Configurations.MASTER_PASSWORD).map { _ =>
                logger.info("The master password has been successfully updated and existing secrets have been re-encrypted using it. " +
                  "In the application's next startup remove property MASTER_PASSWORD_TO_REPLACE to avoid startup warnings.")
              }
            } else {
              if (Configurations.MASTER_PASSWORD_FORCE) {
                systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD)))).map { _ =>
                  logger.warn("The configured MASTER_PASSWORD does not match the one currently in place but the previous one to " +
                    "replace, provided via MASTER_PASSWORD_TO_REPLACE, does not match it either. As property MASTER_PASSWORD_FORCE " +
                    "is set to true the new MASTER_PASSWORD will be used from now on, however any existing secret values will be " +
                    "rendered invalid and lost as they cannot be re-encrypted.")
                }
              } else {
                throw new IllegalStateException("The configured MASTER_PASSWORD does not match the one currently in place but " +
                  "the previous one to replace, provided via MASTER_PASSWORD_TO_REPLACE, does not match it either. As property " +
                  "MASTER_PASSWORD_FORCE is not set to true this is considered an invalid state resulting in a startup failure. " +
                  "To avoid this you should provide the correct previous password via property MASTER_PASSWORD_TO_REPLACE in the " +
                  "next startup to allow existing secret values to be decrypted and re-encrypted with the new password. If you do " +
                  "not know the previous password or want to proceed without updating existing secrets you can force the startup by " +
                  "setting MASTER_PASSWORD_FORCE to true when you restart. Doing so however will result in the loss of any existing " +
                  "secret values.")
              }
            }
          } else {
            if (Configurations.MASTER_PASSWORD_FORCE) {
              systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD)))).map { _ =>
                logger.warn("The configured MASTER_PASSWORD does not match the one currently in place but you did not specify " +
                  "the previous password via property MASTER_PASSWORD_TO_REPLACE. As property MASTER_PASSWORD_FORCE is set to true " +
                  "the new MASTER_PASSWORD will be used from now on, however any existing secret values will be rendered invalid and lost " +
                  "as they could not be re-encrypted.")
              }
            } else {
              throw new IllegalStateException("The configured MASTER_PASSWORD does not match the one currently in place but " +
                "you did not specify the previous password via property MASTER_PASSWORD_TO_REPLACE. As property " +
                "MASTER_PASSWORD_FORCE is not set to true this is considered an invalid state resulting in a startup failure. " +
                "To avoid this you should provide the previous password via property MASTER_PASSWORD_TO_REPLACE in the next startup " +
                "to allow existing secret values to be decrypted and re-encrypted with the new password. If you do not know the " +
                "previous password or want to proceed without updating existing secrets you can force the startup by setting " +
                "MASTER_PASSWORD_FORCE to true when you restart. Doing so however will result in the loss of any existing secret " +
                "values.")
            }
          }
        }
      }
    } yield ()
  }

  private def setupAdministratorOneTimePassword(): Future[Unit] = {
    val defaultUsername = "admin@itb"
    authenticationManager.replaceDefaultAdminPasswordIfNeeded(defaultUsername).map { newDefaultAdminAccountPassword =>
      if (newDefaultAdminAccountPassword.isDefined) {
        logger.info(
          "The default administrator account has a onetime password to be replaced on first login.\n\n\n" +
            "###############################################################################\n\n" +
            s"The one-time password for the default administrator account [$defaultUsername] is:\n\n"+
            s"${newDefaultAdminAccountPassword.get}\n\n"+
            "###############################################################################\n\n"
        )
      }
    }
  }

  private def setupInteractionNotifications(): Future[Unit] = {
    Future.successful {
      testResultManager.schedulePendingTestInteractionNotifications()
    }
  }

  private def setupSsoMigrationModeIfNeeded(): Future[Unit] = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED && !Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD) {
      userManager.migratedUsersExist().map { exist =>
        if (!exist) {
          // We force migration mode because otherwise the ITB instance is unusable without being manually set to migration mode.
          logger.info("SSO is enabled without any SSO-enabled accounts. Forcing SSO migration mode until an initial account is migrated.")
          Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = true
        }
      }
    } else {
      Future.successful(())
    }
  }

  private def initialiseTestbedClient(): Future[Unit] = {
    Future.successful {
      val endpoint = Endpoint.create(new TestbedService(actorSystem)).asInstanceOf[EndpointImpl]
      if (Configurations.TESTBED_CLIENT_URL_INTERNAL != Configurations.TESTBED_CLIENT_URL) {
        endpoint.setPublishedEndpointUrl(Configurations.TESTBED_CLIENT_URL)
      }
      endpoint.publish(Configurations.TESTBED_CLIENT_URL_INTERNAL)
      TestbedService.endpoint = endpoint
    }
  }

  /**
    * Scheduled job that kills idle sessions
    */
  private def destroyIdleSessions(): Future[Cancellable] = {
    Future.successful {
      actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, 30.minutes) {
        () => {
          systemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime).map { config =>
            if (config.isDefined && config.get.parameter.isDefined) {
              testResultManager.getRunningTestResults.map { sessions =>
                val terminationTasks = sessions.map { session =>
                  val difference = TimeUtil.getTimeDifferenceInSeconds(session.startTime)
                  if (difference >= config.get.parameter.get.toInt) {
                    val sessionId = session.sessionId
                    testExecutionManager.endSession(sessionId).map { _ =>
                      logger.info("Terminated idle session [" + sessionId + "]")
                    }
                  } else {
                    // Nothing to do
                    Future.successful(())
                  }
                }
                Future.sequence(terminationTasks).recover {
                  case e: Exception =>
                    logger.warn("Failure while terminating idle sessions", e)
                    throw e
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Scheduled job that deletes inactive user accounts.
   */
  private def deleteInactiveUserAccounts(): Future[Cancellable] = {
    Future.successful {
      actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, 1.day) {
        () => {
          systemConfigurationManager.deleteInactiveUserAccounts().map { deletedAccounts =>
            if (deletedAccounts.isDefined) {
              logger.info("Deleted {} inactive user account(s)", deletedAccounts.get)
            } else {
              logger.debug("Skipped the deletion of inactive user accounts")
            }
          }
        }
      }
    }
  }

  private def cleanupPendingTestSuiteUploads(): Future[Cancellable] = {
    Future.successful {
      actorSystem.scheduler.scheduleWithFixedDelay(1.hours, 1.hours) {
        () => {
          deleteSubfolders(repositoryUtils.getPendingFolder(), 3600000) // 1 hour
        }
      }
    }
  }

  private def cleanupTempFiles(): Future[Cancellable] = {
    Future.successful {
      // Make sure the root temp folders exist
      Files.createDirectories(repositoryUtils.getTempReportFolder().toPath)
      Files.createDirectories(repositoryUtils.getTempArchivedSessionWorkspaceFolder().toPath)
      // Schedule the cleanup job.
      actorSystem.scheduler.scheduleAtFixedRate(0.minutes, 5.minutes) {
        () => {
          deleteSubfolders(repositoryUtils.getTempReportFolder(), 300000) // 5 minutes
          deleteSubfolders(repositoryUtils.getTempArchivedSessionWorkspaceFolder(), 300000) // 5 minutes
        }
      }
    }
  }

  private def deleteSubfolders(rootFolder: File, gracePeriodMillis: Long): Unit = {
    if (rootFolder.exists() && rootFolder.isDirectory) {
      for (file <- rootFolder.listFiles()) {
        if (file.lastModified() + gracePeriodMillis < System.currentTimeMillis) {
          try {
            FileUtils.deleteQuietly(file)
          } catch {
            case e:Exception =>
              logger.warn("Unable to delete temp folder [" + file.getAbsolutePath + "]", e)
          }
        }
      }
    }
  }

  private def loadDataExports(): Future[Unit] = {
    val dataIn = repositoryUtils.getDataInFolder()
    if (dataIn.exists() && dataIn.isDirectory && dataIn.canRead) {
      val containedFiles = dataIn.listFiles()
      // Make sure the processing order is consistent and matches the file name alphabetical ordering.
      util.Arrays.sort(containedFiles, NameFileComparator.NAME_INSENSITIVE_COMPARATOR)
      if (containedFiles != null && containedFiles.nonEmpty) {
        val archiveKey = Configurations.DATA_ARCHIVE_KEY
        if (archiveKey.isBlank) {
          logger.warn("No key was provided to open provided data archives. Skipping data import.")
          Future.successful(())
        } else {
          // We use foldLeft to ensure that the items are processed in sequence producing futures that execute before proceeding to the next one.
          containedFiles.filter(_.getName.toLowerCase.endsWith(".zip")).foldLeft(Future.successful(())) { (previousFuture, currentArchive) =>
            previousFuture.flatMap { _ =>
              importCompleteManager.importSandboxData(currentArchive, archiveKey).map(_ => ())
            }
          }
        }
      } else {
        Future.successful(())
      }
    } else {
      Future.successful(())
    }
  }

  private def archiveOldTestSessions(): Future[Cancellable] = {
    Future.successful {
      actorSystem.scheduler.scheduleAtFixedRate(0.minutes, 20.hours) {
        () => {
          val now = LocalDate.now()
          val archivalThreshold = now.minusDays(Configurations.TEST_SESSION_ARCHIVE_THRESHOLD)
          val statusUpdatesFolder = repositoryUtils.getStatusUpdatesFolder()
          if (statusUpdatesFolder.exists() && statusUpdatesFolder.isDirectory) {
            val yearFolders = statusUpdatesFolder.listFiles(new FileFilter {
              override def accept(pathname: File): Boolean = {
                pathname.isDirectory && isNumeric(pathname.getName)
              }
            })
            if (yearFolders != null) {
              yearFolders.foreach { yearFolder =>
                val monthFoldersToArchive = yearFolder.listFiles(new FileFilter {
                  override def accept(pathname: File): Boolean = {
                    if (pathname.isDirectory) {
                      try {
                        val year = Integer.parseInt(yearFolder.getName)
                        val month = Integer.parseInt(pathname.getName)
                        val folderDate = LocalDate.of(year, month, 1)
                        folderDate.isBefore(archivalThreshold) && (now.getYear != year || month < now.getMonthValue)
                      } catch {
                        case _: NumberFormatException =>
                          // In case we have unexpected folders that don't match what we expect
                          false
                      }
                    } else {
                      false
                    }
                  }
                })
                if (monthFoldersToArchive != null) {
                  monthFoldersToArchive.foreach { monthFolder =>
                    // Create the zip archive.
                    val zipArchive = Path.of(yearFolder.getAbsolutePath, monthFolder.getName+".zip")
                    Files.deleteIfExists(zipArchive)
                    new ZipArchiver(monthFolder.toPath, zipArchive).zip()
                    // All ok - delete the folder.
                    FileUtils.deleteDirectory(monthFolder)
                    logger.info("Archived test session folder for year ["+yearFolder.getName+"] and month ["+monthFolder.getName+"]")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private def prepareRestApiDocumentation(): Future[Unit] = {
    Future.successful {
      val apiDocsFile = repositoryUtils.getRestApiDocsDocumentation()
      if (apiDocsFile.exists()) {
        FileUtils.deleteQuietly(apiDocsFile)
      }
      var apiUrl = Configurations.TESTBED_HOME_LINK
      if (apiUrl == "/") {
        apiUrl = s"http://localhost:${sys.env.getOrElse("http.port", 9000)}${Configurations.API_ROOT}/rest"
      } else {
        if (!apiUrl.endsWith("/")) {
          apiUrl += "/"
        }
        apiUrl += Configurations.API_PREFIX+"/rest"
      }
      try {
        Using.resource(Thread.currentThread().getContextClassLoader.getResourceAsStream("api/openapi.json")) { stream =>
          var template = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
          template = StringUtils.replaceEach(template,
            Array("${version}", "${contactEmail}", "${userGuideAddress}", "${apiUrl}"),
            Array(Constants.VersionNumber, Configurations.EMAIL_TO.getOrElse(Array.empty).headOption.getOrElse(Configurations.EMAIL_DEFAULT.getOrElse("-")), Configurations.USERGUIDE_OU, apiUrl)
          )
          Files.createDirectories(apiDocsFile.getParentFile.toPath)
          Files.writeString(apiDocsFile.toPath, template, StandardCharsets.UTF_8)
        }
        logger.info("Prepared REST API documentation")
      } catch {
        case e: Exception =>
          logger.error("Failed to generate REST API documentation", e)
          throw e
      }
    }
  }

  private def isNumeric(name: String): Boolean = {
    try {
      Integer.parseInt(name)
      true
    } catch {
      case _: NumberFormatException =>
        false
    }
  }

  private def prepareTheme(): Future[String] = {
    // Calling this method will parse and cache the active theme.
    systemConfigurationManager.getCssForActiveTheme()
  }

  private def removeOverrideConfiguration(): Future[Unit] = {
    Future.successful {
      if (Configurations.TESTBED_MODE == Constants.ProductionMode) {
        /*
         * If we are running in production we may have secrets defined in a separate configuration file
         * that is generated by the Docker entrypoint script. At this point we have fully loaded our
         * configuration so we can safely delete this file (upon restart it will again be re-created for
         * the duration of the bootstrap process).
         */
        Files.deleteIfExists(Path.of(sys.env.getOrElse("OVERRIDE_CONFIG_PATH", "/usr/local/gitb-ui/conf/overrides.conf")))
      }
    }
  }

}
