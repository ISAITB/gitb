package hooks

import akka.actor.ActorSystem
import config.Configurations
import config.Configurations.BUILD_TIMESTAMP
import jakarta.xml.ws.Endpoint
import jaxws.TestbedService
import managers._
import managers.export.ImportCompleteManager
import models.Constants
import models.Enums.UserRole
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import play.api.Environment
import utils.{RepositoryUtils, TimeUtil, ZipArchiver}

import java.io.{File, FileFilter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.LocalDate
import java.util.Properties
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Using

@Singleton
class PostStartHook @Inject() (implicit ec: ExecutionContext, actorSystem: ActorSystem, systemConfigurationManager: SystemConfigurationManager, testResultManager: TestResultManager, testExecutionManager: TestExecutionManager, importCompleteManager: ImportCompleteManager, repositoryUtils: RepositoryUtils, environment: Environment, userManager: UserManager) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  onStart()

  def onStart(): Unit = {
    logger.info("Starting Application")
    System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
    BUILD_TIMESTAMP = getBuildTimestamp()
    initialiseTestbedClient()
    adaptSystemConfiguration()
    checkMasterPassword()
    destroyIdleSessions()
    cleanupPendingTestSuiteUploads()
    cleanupTempFiles()
    loadDataExports()
    archiveOldTestSessions()
    prepareRestApiDocumentation()
    prepareTheme()
    logger.info("Application has started in "+Configurations.TESTBED_MODE+" mode - release "+Constants.VersionNumber + " built at "+Configurations.BUILD_TIMESTAMP)
  }

  private def adaptSystemConfiguration(): Unit = {
    // Load persisted configuration parameters.
    val persistedConfigs = systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true)
    // Check against environment settings.
    val restApiEnabledConfig = persistedConfigs.find(config => config.config.name == Constants.RestApiEnabled).map(_.config)
    // REST API.
    if (restApiEnabledConfig.nonEmpty && restApiEnabledConfig.get.parameter.nonEmpty) {
      Configurations.AUTOMATION_API_ENABLED = restApiEnabledConfig.get.parameter.get.toBoolean
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
  }

  private def getBuildTimestamp(): String = {
    var timestamp = ""
    Using(environment.classLoader.getResourceAsStream("core-module.properties")) { stream =>
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
      existingValue = Some(BCrypt.hashpw(String.valueOf(Configurations.MASTER_PASSWORD), BCrypt.gensalt()))
      systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, existingValue)
    } else {
      existingValue = existingHash.get.parameter
    }
    if (BCrypt.checkpw(String.valueOf(Configurations.MASTER_PASSWORD), existingValue.get)) {
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
        if (BCrypt.checkpw(String.valueOf(Configurations.MASTER_PASSWORD_TO_REPLACE.get), existingValue.get)) {
          systemConfigurationManager.updateMasterPassword(Configurations.MASTER_PASSWORD_TO_REPLACE.get, Configurations.MASTER_PASSWORD)
          logger.info("The master password has been successfully updated and existing secrets have been re-encrypted using it. " +
            "In the application's next startup remove property MASTER_PASSWORD_TO_REPLACE to avoid startup warnings.")
        } else {
          if (Configurations.MASTER_PASSWORD_FORCE) {
            systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(BCrypt.hashpw(String.valueOf(Configurations.MASTER_PASSWORD), BCrypt.gensalt())))
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
          systemConfigurationManager.updateSystemParameter(Constants.MasterPassword, Some(BCrypt.hashpw(String.valueOf(Configurations.MASTER_PASSWORD), BCrypt.gensalt())))
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

  private def initialiseTestbedClient(): Unit = {
    TestbedService.endpoint = Endpoint.publish(Configurations.TESTBED_CLIENT_URL, new TestbedService(actorSystem))
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

  private def cleanupPendingTestSuiteUploads() = {
    actorSystem.scheduler.scheduleWithFixedDelay(1.hours, 1.hours) {
      () => {
        deleteSubfolders(repositoryUtils.getPendingFolder(), 3600000) // 1 hour
      }
    }
  }

  private def cleanupTempFiles() = {
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
      apiUrl = s"http://localhost:${sys.env.getOrElse("http.port", 9000)}/${Configurations.API_ROOT}/rest"
    } else {
      if (!apiUrl.endsWith("/")) {
        apiUrl += "/"
      }
      apiUrl += Configurations.API_ROOT+"/rest"
    }
    try {
      Using(Thread.currentThread().getContextClassLoader.getResourceAsStream("api/openapi.json")) { stream =>
        var template = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
        template = StringUtils.replaceEach(template,
          Array("${version}", "${contactEmail}", "${userGuideAddress}", "${apiUrl}"),
          Array(Constants.VersionNumber, Configurations.EMAIL_TO.getOrElse(Array.empty).headOption.getOrElse("-"), Configurations.USERGUIDE_OU, apiUrl)
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

}
