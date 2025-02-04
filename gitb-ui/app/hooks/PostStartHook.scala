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
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory
import play.api.{Configuration, Environment}
import utils.{CryptoUtil, JsonUtil, RepositoryUtils, TimeUtil, ZipArchiver}

import java.io.{File, FileFilter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.LocalDate
import java.util
import java.util.Properties
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Using

@Singleton
class PostStartHook @Inject() (implicit ec: ExecutionContext,
                               authenticationManager: AuthenticationManager,
                               actorSystem: ActorSystem,
                               systemConfigurationManager: SystemConfigurationManager,
                               testResultManager: TestResultManager,
                               testExecutionManager: TestExecutionManager,
                               importCompleteManager: ImportCompleteManager,
                               repositoryUtils: RepositoryUtils,
                               environment: Environment,
                               userManager: UserManager,
                               config: Configuration) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  onStart()

  def onStart(): Unit = {
    try {
      if (!Configurations.STARTUP_FAILURE) {
        logger.info("Starting Application")
        System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
        BUILD_TIMESTAMP = getBuildTimestamp()
        checkOperationMode()
        initialiseTestbedClient()
        checkMasterPassword()
        loadDataExports()
        adaptSystemConfiguration()
        destroyIdleSessions()
        deleteInactiveUserAccounts()
        cleanupPendingTestSuiteUploads()
        cleanupTempFiles()
        archiveOldTestSessions()
        prepareRestApiDocumentation()
        prepareTheme()
        setupAdministratorOneTimePassword()
        setupInteractionNotifications()
        removeOverrideConfiguration()
      }
    } catch {
      case e: Exception =>
        Configurations.STARTUP_FAILURE = true
        throw e
    } finally {
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
  }

  private def checkOperationMode(): Unit = {
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

  private def adaptSystemConfiguration(): Unit = {
    // Record the default settings (from the config, fixed values and the environment).
    val defaultEmailSettings = systemConfigurationManager.recordDefaultEmailSettings()
    // Load persisted configuration parameters.
    val persistedConfigs = systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true)
    // Check against environment settings.
    val restApiEnabledConfig = persistedConfigs.find(config => config.config.name == Constants.RestApiEnabled).map(_.config)
    // REST API.
    if (restApiEnabledConfig.nonEmpty && restApiEnabledConfig.get.parameter.nonEmpty) {
      Configurations.AUTOMATION_API_ENABLED = restApiEnabledConfig.get.parameter.get.toBoolean
    }
    val restApiAdminKey = persistedConfigs.find(config => config.config.name == Constants.RestApiAdminKey).map(_.config)
    if (restApiAdminKey.flatMap(_.parameter).isEmpty) {
      val initialApiKeyValue = Configurations.AUTOMATION_API_MASTER_KEY.getOrElse(CryptoUtil.generateApiKey())
      systemConfigurationManager.updateSystemParameter(Constants.RestApiAdminKey, Some(initialApiKeyValue))
    }
    // Self-registration.
    val selfRegistrationConfig = persistedConfigs.find(config => config.config.name == Constants.SelfRegistrationEnabled).map(_.config)
    if (selfRegistrationConfig.nonEmpty && selfRegistrationConfig.get.parameter.nonEmpty) {
      Configurations.REGISTRATION_ENABLED = selfRegistrationConfig.get.parameter.get.toBoolean
    }
    // Demo account.
    val demoAccountConfig = persistedConfigs.find(config => config.config.name == Constants.DemoAccount).map(_.config)
    Configurations.DEMOS_ENABLED = demoAccountConfig.nonEmpty && demoAccountConfig.get.parameter.nonEmpty
    if (Configurations.DEMOS_ENABLED) {
      Configurations.DEMOS_ACCOUNT = demoAccountConfig.get.parameter.get.toLong
    } else {
      Configurations.DEMOS_ACCOUNT = -1
    }
    // Make sure that the configured demo account is an existing basic user.
    if (Configurations.DEMOS_ACCOUNT != -1) {
      val demoUser = userManager.getUserById(Configurations.DEMOS_ACCOUNT)
      if (demoUser.isEmpty || demoUser.get.role != UserRole.VendorUser.id.toShort) {
        logger.warn("Configured demo account ID [{}] does not match a non-administrator user and will be ignored.", Configurations.DEMOS_ACCOUNT)
        Configurations.DEMOS_ENABLED = false
        Configurations.DEMOS_ACCOUNT = -1
      }
    }
    // Welcome message.
    val welcomeMessageConfig = persistedConfigs.find(config => config.config.name == Constants.WelcomeMessage).map(_.config)
    if (welcomeMessageConfig.nonEmpty && welcomeMessageConfig.get.parameter.nonEmpty) {
      Configurations.WELCOME_MESSAGE = welcomeMessageConfig.get.parameter.get
    } else {
      Configurations.WELCOME_MESSAGE = Configurations.WELCOME_MESSAGE_DEFAULT
    }
    // Email settings.
    val emailSettings = persistedConfigs.find(config => config.config.name == Constants.EmailSettings).map(_.config)
    if (emailSettings.nonEmpty && emailSettings.get.parameter.nonEmpty) {
      JsonUtil.parseJsEmailSettings(emailSettings.get.parameter.get).toEnvironment(Some(defaultEmailSettings))
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

  private def checkMasterPassword(): Unit = {
    val existingHash = systemConfigurationManager.getSystemConfiguration(Constants.MasterPassword)
    var existingValue: Option[String] = None
    if (existingHash.isEmpty || existingHash.get.parameter.isEmpty) {
      // Store master password.
      existingValue = Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD)))
      systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, existingValue)
    } else {
      existingValue = existingHash.get.parameter
    }
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
    } else {
      // Set master password does not match stored value.
      if (Configurations.MASTER_PASSWORD_TO_REPLACE.isDefined) {
        if (CryptoUtil.checkPassword(String.valueOf(Configurations.MASTER_PASSWORD_TO_REPLACE.get), existingValue.get)) {
          systemConfigurationManager.updateMasterPassword(Configurations.MASTER_PASSWORD_TO_REPLACE.get, Configurations.MASTER_PASSWORD)
          logger.info("The master password has been successfully updated and existing secrets have been re-encrypted using it. " +
            "In the application's next startup remove property MASTER_PASSWORD_TO_REPLACE to avoid startup warnings.")
        } else {
          if (Configurations.MASTER_PASSWORD_FORCE) {
            systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD))))
            logger.warn("The configured MASTER_PASSWORD does not match the one currently in place but the previous one to " +
              "replace, provided via MASTER_PASSWORD_TO_REPLACE, does not match it either. As property MASTER_PASSWORD_FORCE " +
              "is set to true the new MASTER_PASSWORD will be used from now on, however any existing secret values will be " +
              "rendered invalid and lost as they cannot be re-encrypted.")
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
          systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(CryptoUtil.hashPassword(String.valueOf(Configurations.MASTER_PASSWORD))))
          logger.warn("The configured MASTER_PASSWORD does not match the one currently in place but you did not specify " +
            "the previous password via property MASTER_PASSWORD_TO_REPLACE. As property MASTER_PASSWORD_FORCE is set to true " +
            "the new MASTER_PASSWORD will be used from now on, however any existing secret values will be rendered invalid and lost " +
            "as they could not be re-encrypted.")
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

  private def setupAdministratorOneTimePassword(): Unit = {
    val defaultUsername = "admin@itb"
    val newDefaultAdminAccountPassword = authenticationManager.replaceDefaultAdminPasswordIfNeeded(defaultUsername)
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

  private def setupInteractionNotifications(): Unit = {
    testResultManager.schedulePendingTestInteractionNotifications()
  }

  private def initialiseTestbedClient(): Unit = {
    val endpoint = Endpoint.create(new TestbedService(actorSystem)).asInstanceOf[EndpointImpl]
    if (Configurations.TESTBED_CLIENT_URL_INTERNAL != Configurations.TESTBED_CLIENT_URL) {
      endpoint.setPublishedEndpointUrl(Configurations.TESTBED_CLIENT_URL)
    }
    endpoint.publish(Configurations.TESTBED_CLIENT_URL_INTERNAL)
    TestbedService.endpoint = endpoint
  }

  /**
    * Scheduled job that kills idle sessions
    */
  private def destroyIdleSessions() = {
    actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, 30.minutes) {
      () => {
        val config = systemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime)
        if (config.isDefined && config.get.parameter.isDefined) {
          val list = testResultManager.getRunningTestResults
          list.foreach { result =>
            val difference = TimeUtil.getTimeDifferenceInSeconds(result.startTime)
            if (difference >= config.get.parameter.get.toInt) {
              val sessionId = result.sessionId
              testExecutionManager.endSession(sessionId)
              logger.info("Terminated idle session [" + sessionId + "]")
            }
          }
        }
      }
    }
  }

  /**
   * Scheduled job that deletes inactive user accounts.
   */
  private def deleteInactiveUserAccounts() = {
    actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, 1.day) {
      () => {
        val deletedAccounts = systemConfigurationManager.deleteInactiveUserAccounts()
        if (deletedAccounts.isDefined) {
          logger.info("Deleted {} inactive user account(s)", deletedAccounts.get)
        } else {
          logger.debug("Skipped the deletion of inactive user accounts")
        }
      }
    }
  }

  private def cleanupPendingTestSuiteUploads() = {
    actorSystem.scheduler.scheduleWithFixedDelay(1.hours, 1.hours) {
      () => {
        deleteSubfolders(repositoryUtils.getPendingFolder(), 3600000) // 1 hour
      }
    }
  }

  private def cleanupTempFiles() = {
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

  private def loadDataExports(): Unit = {
    val dataIn = repositoryUtils.getDataInFolder()
    if (dataIn.exists() && dataIn.isDirectory && dataIn.canRead) {
      val containedFiles = dataIn.listFiles()
      // Make sure the processing order is consistent and matches the file name alphabetical ordering.
      util.Arrays.sort(containedFiles, NameFileComparator.NAME_INSENSITIVE_COMPARATOR)
      if (containedFiles != null && containedFiles.nonEmpty) {
        val archiveKey = Configurations.DATA_ARCHIVE_KEY
        if (archiveKey.isBlank) {
          logger.warn("No key was provided to open provided data archives. Skipping data import.")
        } else {
          containedFiles.foreach { file =>
            if (file.getName.toLowerCase.endsWith(".zip")) {
              importCompleteManager.importSandboxData(file, archiveKey)
            }
          }
        }
      }
    }
  }

  private def archiveOldTestSessions() = {
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

  private def prepareRestApiDocumentation(): Unit = {
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

  private def isNumeric(name: String): Boolean = {
    try {
      Integer.parseInt(name)
      true
    } catch {
      case _: NumberFormatException =>
        false
    }
  }

  private def prepareTheme(): Unit = {
    // Calling this method will parse and cache the active theme.
    systemConfigurationManager.getCssForActiveTheme()
  }

  private def removeOverrideConfiguration(): Unit = {
    if (Configurations.TESTBED_MODE == Constants.ProductionMode) {
      /*
       * If we are running in production we may have secrets defined in a separate configuration file
       * that is generated by the Docker entrypoint script. At this point we have fully loaded our
       * configuration so we can safely delete this  file (upon restart it will again be re-created for
       * the duration of the bootstrap process).
       */
      Files.deleteIfExists(Path.of(sys.env.getOrElse("OVERRIDE_CONFIG_PATH", "/usr/local/gitb-ui/conf/overrides.conf")))
    }
  }

}
