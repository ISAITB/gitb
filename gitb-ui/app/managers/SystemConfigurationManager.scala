package managers

import config.Configurations
import models.Enums.UserRole
import models._
import models.theme.{Theme, ThemeFiles}
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.collection.heterogeneous.HNil
import utils.{EmailUtil, JsonUtil, MimeUtil, RepositoryUtils}

import java.io.File
import java.sql.Timestamp
import java.util.{Calendar, UUID}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}

@Singleton
class SystemConfigurationManager @Inject() (testResultManager: TestResultManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemConfigurationManager])
  private final val editableSystemConfigurationTypes = Set(
    Constants.SessionAliveTime, Constants.RestApiEnabled, Constants.SelfRegistrationEnabled,
    Constants.DemoAccount, Constants.WelcomeMessage, Constants.AccountRetentionPeriod,
    Constants.EmailSettings
  )

  private var activeThemeId: Option[Long] = None
  private var activeThemeCss: Option[String] = None
  private var activeThemeFavicon: Option[String] = None
  private var defaultEmailSettings: EmailSettings = _

  private def constructLogoPath(themeId: Long, partialLogoPath: String): String = {
    // We go up two levels as URLs are relative to the CSS defining them which here is under "/api/theme/
    val path = new StringBuilder("url('../../")
    if (!isBuiltInThemeResource(partialLogoPath)) {
      path.append("api/theme/resource/").append(themeId).append("/")
    }
    path.append(StringUtils.removeStart(partialLogoPath, "/"))
      .append("')")
      .toString()
  }

  def getThemeResource(themeId: Long, resourceName: String): Option[File] = {
    repositoryUtils.getThemeResource(themeId, resourceName)
  }

  private def getOrSetActiveTheme(): DBIO[(Option[Theme], Option[Theme], Option[Theme])] = {
    for {
      activeTheme <- PersistenceSchema.themes
        .filter(_.active === true)
        .result.headOption
      environmentTheme <- {
        if (activeTheme.isDefined) {
          DBIO.successful(None)
        } else if (sys.env.contains(Constants.EnvironmentTheme)) {
          PersistenceSchema.themes.filter(_.key === sys.env(Constants.EnvironmentTheme)).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      defaultTheme <- {
        if (activeTheme.isDefined || environmentTheme.isDefined) {
          DBIO.successful(None)
        } else {
          PersistenceSchema.themes.filter(_.key === Constants.DefaultTheme).result.headOption
        }
      }
      // In case we have a environment-based or default theme mark it as active.
      _ <- {
        var themeIdToActivate: Option[Long] = None
        if (activeTheme.isEmpty) {
          if (environmentTheme.isDefined) {
            themeIdToActivate = Some(environmentTheme.get.id)
          } else if (defaultTheme.isDefined) {
            themeIdToActivate = Some(defaultTheme.get.id)
          }
        }
        if (themeIdToActivate.isDefined) {
          PersistenceSchema.themes.filter(_.id === themeIdToActivate.get).map(_.active).update(true)
        } else {
          DBIO.successful(())
        }
      }
    } yield (activeTheme, environmentTheme, defaultTheme)
  }

  def reloadThemeCss(): Unit = {
    val themeData = exec(getOrSetActiveTheme().transactionally)
    val themeToUse = if (themeData._1.isDefined) {
      logger.info(s"Loaded theme [${themeData._1.get.key}] marked as active.")
      themeData._1.get
    } else if (themeData._2.isDefined) {
      logger.info(s"Loaded theme [${themeData._2.get.key}] selected via environment variable.")
      themeData._2.get
    } else {
      logger.info("Loaded default theme.")
      themeData._3.get
    }
    val cssContent = ":root {" +
        "  --itb-separator-title-color: " + themeToUse.separatorTitleColor + ";\n" +
        "  --itb-modal-title-color: " + themeToUse.modalTitleColor + ";\n" +
        "  --itb-table-title-color: " + themeToUse.tableTitleColor + ";\n" +
        "  --itb-card-title-color: " + themeToUse.cardTitleColor + ";\n" +
        "  --itb-page-title-color: " + themeToUse.pageTitleColor + ";\n" +
        "  --itb-heading-color: " + themeToUse.headingColor + ";\n" +
        "  --itb-tab-link-color: " + themeToUse.tabLinkColor + ";\n" +
        "  --itb-footer-text-color: " + themeToUse.footerTextColor + ";\n" +
        "  --itb-header-background-color: " + themeToUse.headerBackgroundColor + ";\n" +
        "  --itb-header-border-color: " + themeToUse.headerBorderColor + ";\n" +
        "  --itb-header-separator-color: " + themeToUse.headerSeparatorColor + ";\n" +
        "  --itb-header-logo-path: " + constructLogoPath(themeToUse.id, themeToUse.headerLogoPath) + ";\n" +
        "  --itb-footer-background-color: " + themeToUse.footerBackgroundColor + ";\n" +
        "  --itb-footer-border-color: " + themeToUse.footerBorderColor + ";\n" +
        "  --itb-footer-logo-path: " + constructLogoPath(themeToUse.id, themeToUse.footerLogoPath) + ";\n" +
        "  --itb-footer-logo-display: " + themeToUse.footerLogoDisplay + ";\n" +
        "  --itb-btn-primary-color: " + themeToUse.primaryButtonColor + ";\n" +
        "  --itb-btn-primary-label-color: " + themeToUse.primaryButtonLabelColor + ";\n" +
        "  --itb-btn-primary-hover-color: " + themeToUse.primaryButtonHoverColor + ";\n" +
        "  --itb-btn-primary-active-color: " + themeToUse.primaryButtonActiveColor + ";\n" +
        "  --itb-btn-secondary-color: " + themeToUse.secondaryButtonColor + ";\n" +
        "  --itb-btn-secondary-label-color: " + themeToUse.secondaryButtonLabelColor + ";\n" +
        "  --itb-btn-secondary-hover-color: " + themeToUse.secondaryButtonHoverColor + ";\n" +
        "  --itb-btn-secondary-active-color: " + themeToUse.secondaryButtonActiveColor + ";\n" +
    "}"
    activeThemeCss = Some(cssContent)
    activeThemeFavicon = Some(themeToUse.faviconPath)
    activeThemeId = Some(themeToUse.id)
  }

  def getActiveThemeId(): Long = {
    if (activeThemeId.isEmpty) {
      reloadThemeCss()
    }
    activeThemeId.get
  }

  def getCssForActiveTheme(): String = {
    if (activeThemeCss.isEmpty) {
      reloadThemeCss()
    }
    activeThemeCss.get
  }

  def getFaviconPath(): String = {
    if (activeThemeFavicon.isEmpty) {
      reloadThemeCss()
    }
    activeThemeFavicon.get
  }

  /**
   * Gets system config by name
   */
  def getSystemConfiguration(name: String): Option[SystemConfigurations] = {
    exec(PersistenceSchema.systemConfigurations.filter(_.name === name).result.headOption)
  }

  def getEditableSystemConfigurationValues(onlyPersisted: Boolean = false): List[SystemConfigurationsWithEnvironment] = {
    var persistedConfigs = exec(
      PersistenceSchema.systemConfigurations
        .filter(_.name inSet editableSystemConfigurationTypes)
        .map(x => (x.name, x.parameter))
        .result
    ).map(x => SystemConfigurationsWithEnvironment(SystemConfigurations(x._1, x._2, None), defaultSetting = false, environmentSetting = false)).toList
    if (!onlyPersisted) {
      val restApiEnabledConfig = persistedConfigs.find(config => config.config.name == Constants.RestApiEnabled)
      val demoAccountConfig = persistedConfigs.find(config => config.config.name == Constants.DemoAccount)
      val selfRegistrationConfig = persistedConfigs.find(config => config.config.name == Constants.SelfRegistrationEnabled)
      val welcomeMessageConfig = persistedConfigs.find(config => config.config.name == Constants.WelcomeMessage)
      val emailSettingsConfig = persistedConfigs.find(config => config.config.name == Constants.EmailSettings)
      if (restApiEnabledConfig.isEmpty) {
        persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.RestApiEnabled, Some(Configurations.AUTOMATION_API_ENABLED.toString), None), defaultSetting = true, environmentSetting = sys.env.contains("AUTOMATION_API_ENABLED"))
      }
      if (selfRegistrationConfig.isEmpty) {
        persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.SelfRegistrationEnabled, Some(Configurations.REGISTRATION_ENABLED.toString), None), defaultSetting = true, environmentSetting = sys.env.contains("REGISTRATION_ENABLED"))
      }
      if (demoAccountConfig.isEmpty) {
        if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT != -1) {
          persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.DemoAccount, Some(Configurations.DEMOS_ACCOUNT.toString), None), defaultSetting = true, environmentSetting = sys.env.contains("DEMOS_ENABLED") && sys.env.contains("DEMOS_ACCOUNT"))
        } else {
          persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.DemoAccount, None, None), defaultSetting = true, environmentSetting = sys.env.contains("DEMOS_ENABLED") || sys.env.contains("DEMOS_ACCOUNT"))
        }
      } else if (demoAccountConfig.get.config.parameter.nonEmpty && demoAccountConfig.get.config.parameter.get.toLong != Configurations.DEMOS_ACCOUNT) {
        // Invalid ID configured in the DB.
        persistedConfigs = persistedConfigs.filterNot(config => config.config.name == Constants.DemoAccount)
        persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.DemoAccount, None, None), defaultSetting = true, environmentSetting = sys.env.contains("DEMOS_ENABLED") || sys.env.contains("DEMOS_ACCOUNT"))
      }
      if (welcomeMessageConfig.isEmpty) {
        persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.WelcomeMessage, Some(Configurations.WELCOME_MESSAGE), None), defaultSetting = true, environmentSetting = false)
      }
      if (emailSettingsConfig.isEmpty) {
        persistedConfigs = persistedConfigs :+ SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.EmailSettings, Some(JsonUtil.jsEmailSettings(EmailSettings.fromEnvironment()).toString()), None), defaultSetting = true, environmentSetting = sys.env.contains("EMAIL_ENABLED"))
      }
    }
    persistedConfigs
  }

  def isEditableSystemParameter(name: String): Boolean = {
    editableSystemConfigurationTypes.contains(name)
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None): Option[SystemConfigurationsWithEnvironment] = {
    exec(updateSystemParameterInternal(name, value, applySetting = true).transactionally)
  }

  private def processReceivedEmailSettings(jsonString: String): EmailSettings = {
    var settings = JsonUtil.parseJsEmailSettings(jsonString)
    if (settings.authEnabled.isDefined && settings.authEnabled.get) {
      // We have authentication.
      if (settings.authPassword.isDefined) {
        // Encrypt the SMTP password at rest.
        settings = settings.withPassword(MimeUtil.encryptString(settings.authPassword.get))
      } else if (Configurations.EMAIL_SMTP_AUTH_PASSWORD.isDefined) {
        // A password was not submitted. This means we keep the existing value.
        settings = settings.withPassword(MimeUtil.encryptString(Configurations.EMAIL_SMTP_AUTH_PASSWORD.get))
      }
    }
    settings
  }

  def updateSystemParameterInternal(name: String, providedValue: Option[String] = None, applySetting: Boolean): DBIO[Option[SystemConfigurationsWithEnvironment]] = {
    // Do any pre-processing as needed.
    var parsedEmailSettings: Option[EmailSettings] = None
    val value = if (name == Constants.EmailSettings && providedValue.isDefined) {
      Some(JsonUtil.jsEmailSettings(processReceivedEmailSettings(providedValue.get), maskPassword = false).toString())
    } else {
      providedValue
    }
    // Store in the DB.
    for {
      exists <- PersistenceSchema.systemConfigurations.filter(_.name === name).exists.result
      _ <- {
        if (exists) {
          if ((name == Constants.WelcomeMessage || name == Constants.EmailSettings || name == Constants.AccountRetentionPeriod) && value.isEmpty) {
            PersistenceSchema.systemConfigurations.filter(_.name === name).delete
          } else {
            PersistenceSchema.systemConfigurations.filter(_.name === name).map(_.parameter).update(value)
          }
        } else {
          PersistenceSchema.systemConfigurations += SystemConfigurations(name, value, None)
        }
      }
      returnValue <- if (applySetting) {
        // Now apply also to the current instance.
        name match {
          case Constants.RestApiEnabled =>
            if (value.isDefined) {
              Configurations.AUTOMATION_API_ENABLED = value.get.toBoolean
            }
            DBIO.successful(None)
          case Constants.SelfRegistrationEnabled =>
            if (value.isDefined) {
              Configurations.REGISTRATION_ENABLED = value.get.toBoolean
            }
            DBIO.successful(None)
          case Constants.DemoAccount =>
            if (value.isDefined) {
              Configurations.DEMOS_ENABLED = true
              Configurations.DEMOS_ACCOUNT = value.get.toLong
            } else {
              Configurations.DEMOS_ENABLED = false
              Configurations.DEMOS_ACCOUNT = -1
            }
            DBIO.successful(None)
          case Constants.WelcomeMessage =>
            if (value.isDefined) {
              Configurations.WELCOME_MESSAGE = value.get
              DBIO.successful(None)
            } else {
              Configurations.WELCOME_MESSAGE = Configurations.WELCOME_MESSAGE_DEFAULT
              DBIO.successful(Some(
                SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.WelcomeMessage, Some(Configurations.WELCOME_MESSAGE), None), defaultSetting = true, environmentSetting = false)
              ))
            }
          case Constants.AccountRetentionPeriod =>
            if (value.isDefined) {
              deleteInactiveUserAccountsInternal() andThen DBIO.successful(None)
            } else {
              DBIO.successful(None)
            }
          case Constants.EmailSettings =>
            if (value.isDefined) {
              if (parsedEmailSettings.isEmpty) {
                parsedEmailSettings = Some(JsonUtil.parseJsEmailSettings(value.get))
              }
              parsedEmailSettings.get.toEnvironment()
              testResultManager.schedulePendingTestInteractionNotifications()
              DBIO.successful(Some(
                SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.EmailSettings, Some(JsonUtil.jsEmailSettings(EmailSettings.fromEnvironment()).toString()), None), defaultSetting = false, environmentSetting = false)
              ))
            } else {
              defaultEmailSettings.toEnvironment()
              testResultManager.schedulePendingTestInteractionNotifications()
              DBIO.successful(Some(
                SystemConfigurationsWithEnvironment(SystemConfigurations(Constants.EmailSettings, Some(JsonUtil.jsEmailSettings(EmailSettings.fromEnvironment()).toString()), None), defaultSetting = true, environmentSetting = sys.env.contains("EMAIL_ENABLED"))
              ))
            }
          case _ => DBIO.successful(None)
        }
      } else {
        DBIO.successful(None)
      }
    } yield returnValue
  }

  def updateMasterPassword(previousPassword: Array[Char], newPassword: Array[Char]): Unit = {
    val dbAction: DBIO[_] = for {
      // Update the hashed stored value
      _ <- updateSystemParameterInternal(Constants.MasterPassword, Some(BCrypt.hashpw(String.valueOf(newPassword), BCrypt.gensalt())), applySetting = false)
      // Update domain parameters
      domainParams <- PersistenceSchema.domainParameters.filter(_.kind === "HIDDEN").filter(_.value.isDefined).map(x => (x.id, x.value)).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        domainParams.foreach { param =>
          val q = for { p <- PersistenceSchema.domainParameters if p.id === param._1 } yield p.value
          actions += q.update(Some(MimeUtil.encryptString(MimeUtil.decryptString(param._2.get, previousPassword), newPassword)))
        }
        toDBIO(actions)
      }
      // Update organisation parameters
      orgParams <- PersistenceSchema.organisationParameterValues
                    .join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
                    .filter(_._2.kind === "SECRET")
                    .map(x => (x._1.parameter, x._1.organisation, x._1.value))
                    .result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        orgParams.foreach { param =>
          val q = for { p <- PersistenceSchema.organisationParameterValues if p.parameter === param._1 && p.organisation === param._2 } yield p.value
          actions += q.update(MimeUtil.encryptString(MimeUtil.decryptString(param._3, previousPassword), newPassword))
        }
        toDBIO(actions)
      }
      // Update system parameters
      sysParams <- PersistenceSchema.systemParameterValues
                    .join(PersistenceSchema.systemParameters).on(_.parameter === _.id)
                    .filter(_._2.kind === "SECRET")
                    .map(x => (x._1.parameter, x._1.system, x._1.value))
                    .result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        sysParams.foreach { param =>
          val q = for { p <- PersistenceSchema.systemParameterValues if p.parameter === param._1 && p.system === param._2 } yield p.value
          actions += q.update(MimeUtil.encryptString(MimeUtil.decryptString(param._3, previousPassword), newPassword))
        }
        toDBIO(actions)
      }
      // Update statement parameters
      stmtParams <- PersistenceSchema.configs
                      .join(PersistenceSchema.parameters).on(_.parameter === _.id)
                      .filter(_._2.kind === "SECRET")
                      .map(x => (x._1.parameter, x._1.system, x._1.value))
                      .result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        stmtParams.foreach { param =>
          val q = for { p <- PersistenceSchema.configs if p.parameter === param._1 && p.system === param._2 } yield p.value
          actions += q.update(MimeUtil.encryptString(MimeUtil.decryptString(param._3, previousPassword), newPassword))
        }
        toDBIO(actions)
      }
      // Update community keystore keys
      communityKeystores <- PersistenceSchema.communityKeystores.map(x => (x.community, x.keyPassword, x.keystorePassword)).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        communityKeystores.foreach { keystore =>
          val q = for { c <- PersistenceSchema.communityKeystores if c.id === keystore._1 } yield (c.keyPassword, c.keystorePassword)
          actions += q.update(
            MimeUtil.encryptString(MimeUtil.decryptString(keystore._2, previousPassword), newPassword),
            MimeUtil.encryptString(MimeUtil.decryptString(keystore._3, previousPassword), newPassword)
          )
        }
        toDBIO(actions)
      }
      // Update stored SMTP settings
      emailSettings <- PersistenceSchema.systemConfigurations.filter(_.name === Constants.EmailSettings).result.headOption
      _ <- {
        if (emailSettings.isDefined && emailSettings.get.parameter.isDefined) {
          val existingSettings = JsonUtil.parseJsEmailSettings(emailSettings.get.parameter.get)
          if (existingSettings.authPassword.isDefined) {
            val newEncryptedSmtpPassword = MimeUtil.encryptString(MimeUtil.decryptString(existingSettings.authPassword.get, previousPassword), newPassword)
            val newEmailSettingsToStore = JsonUtil.jsEmailSettings(existingSettings.withPassword(newEncryptedSmtpPassword), maskPassword = false)
            PersistenceSchema.systemConfigurations.filter(_.name === Constants.EmailSettings).map(_.parameter).update(Some(newEmailSettingsToStore.toString()))
          } else {
            DBIO.successful(())
          }
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
    exec(dbAction.transactionally)
  }

  def getThemes(): List[Theme] = {
    exec(PersistenceSchema.themes.sortBy(_.key.asc).result).toList
  }

  def getTheme(themeId: Long): Theme = {
    exec(PersistenceSchema.themes.filter(_.id === themeId).result).head
  }

  def themeExists(themeKey: String, exclude: Option[Long]): Boolean = {
    exec(
      PersistenceSchema.themes
      .filter(_.key === themeKey)
      .filterOpt(exclude)((q, id) => q.id =!= id)
      .exists
      .result
    )
  }

  def createTheme(referenceThemeId: Long, theme: Theme, themeFiles: ThemeFiles): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createThemeInternal(Some(referenceThemeId), theme, themeFiles, onSuccessCalls)
    val reloadNeeded = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    if (reloadNeeded) {
      reloadThemeCss()
    }
  }

  def activateTheme(themeId: Long): Unit = {
    exec((for {
      // Deactivate previously active theme.
      _ <- PersistenceSchema.themes.filter(_.active === true).map(_.active).update(false)
      // Activate theme.
      _ <- PersistenceSchema.themes.filter(_.id === themeId).map(_.active).update(true)
    } yield ()).transactionally)
    reloadThemeCss()
  }

  def createThemeInternal(referenceThemeId: Option[Long], theme: Theme, themeFiles: ThemeFiles, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Boolean] = {
    for {
      // Check to see that no other theme has the same key.
      referencedThemeFiles <- {
        if (referenceThemeId.isDefined && (themeFiles.headerLogo.isEmpty || themeFiles.footerLogo.isEmpty || themeFiles.faviconFile.isEmpty)) {
          PersistenceSchema.themes.filter(_.id === referenceThemeId).map(x => (x.headerLogoPath, x.footerLogoPath, x.faviconPath)).result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Save the theme in the DB.
      savedTheme <- {
        val headerPathToSet = getThemeResourcePathToSave(themeFiles.headerLogo,
          determineReferencePath(referenceThemeId.isDefined, theme.headerLogoPath, referencedThemeFiles.map(_._1)),
          isUpdate = false)
        val footerPathToSet = getThemeResourcePathToSave(themeFiles.footerLogo,
          determineReferencePath(referenceThemeId.isDefined, theme.footerLogoPath, referencedThemeFiles.map(_._2)),
          isUpdate = false)
        val faviconPathToSet = getThemeResourcePathToSave(themeFiles.faviconFile,
          determineReferencePath(referenceThemeId.isDefined, theme.faviconPath, referencedThemeFiles.map(_._3)),
          isUpdate = false)
        val themeToSave = theme.withImagePaths(headerPathToSet, footerPathToSet, faviconPathToSet)
        (PersistenceSchema.insertTheme += themeToSave).map(id => themeToSave.withId(id))
      }
      // Deactivate the other active theme if this was active.
      reloadNeeded <- {
        if (savedTheme.active) {
          PersistenceSchema.themes.filter(_.id =!= savedTheme.id).filter(_.active === true).map(_.active).update(false) andThen getOrSetActiveTheme() andThen DBIO.successful(true)
        } else {
          DBIO.successful(false)
        }
      }
      // Save the theme resources on the file system.
      _ <- {
        saveThemeResourceFile(themeFiles.headerLogo, savedTheme.id, savedTheme.headerLogoPath, referenceThemeId, referencedThemeFiles.map(_._1), onSuccessCalls)
        saveThemeResourceFile(themeFiles.footerLogo, savedTheme.id, savedTheme.footerLogoPath, referenceThemeId, referencedThemeFiles.map(_._2), onSuccessCalls)
        saveThemeResourceFile(themeFiles.faviconFile, savedTheme.id, savedTheme.faviconPath, referenceThemeId, referencedThemeFiles.map(_._3), onSuccessCalls)
        DBIO.successful(())
      }
    } yield reloadNeeded
  }

  private def determineReferencePath(checkReference: Boolean, themePath: String, referencePath: Option[String]): Option[String] = {
    if (checkReference) {
      referencePath
    } else {
      // This is an import.
      Some(themePath)
    }
  }

  private def saveThemeResourceFile(providedFile: Option[NamedFile], savedThemeId: Long, savedPath: String, referenceId: Option[Long], referencePath: Option[String], onSuccessCalls: mutable.ListBuffer[() => _]): Unit = {
    if (providedFile.isDefined) {
      onSuccessCalls += (() => repositoryUtils.saveThemeResource(savedThemeId, savedPath, providedFile.get.file))
    } else if (referenceId.isDefined && referencePath.isDefined && !isBuiltInThemeResource(savedPath)) {
      val referencedFile = repositoryUtils.getThemeResource(referenceId.get, referencePath.get)
      onSuccessCalls += (() => repositoryUtils.saveThemeResource(savedThemeId, savedPath, referencedFile.get))
    }
  }

  private def getThemeResourcePathToSave(providedFile: Option[NamedFile], referenceThemeFilePath: Option[String], isUpdate: Boolean): String = {
    var pathToSet: Option[String] = None
    var fileNameToProcess: Option[String] = None
    if (providedFile.isDefined) {
      fileNameToProcess = Some(providedFile.get.name)
    } else if (isUpdate || referenceThemeFilePath.isDefined && isBuiltInThemeResource(referenceThemeFilePath.get)) {
      // We are either updating a theme and keeping the resource unchanged or creating a theme and using a built-in resource. The path is left unchanged.
      pathToSet = referenceThemeFilePath
    } else {
      // We are creating a theme and reusing the reference theme's custom resource. We will make a copy of the theme resource.
      fileNameToProcess = referenceThemeFilePath
    }
    if (pathToSet.isEmpty) {
      val extension = Option(StringUtils.trimToNull(FilenameUtils.getExtension(fileNameToProcess.get)))
      pathToSet = Some(UUID.randomUUID().toString + extension.map(ext => "." + ext).getOrElse(""))
    }
    pathToSet.get
  }

  def isBuiltInThemeResource(resourcePath: String): Boolean = {
    StringUtils.startsWithIgnoreCase(resourcePath, "/assets/")
  }

  def adaptBuiltInThemeResourcePathForClasspathLookup(resourcePath: String): String = {
    // Built-in resource. This is exposed as "/assets/*" but to look it up on the classpath we use "public/*".
    "public/" + StringUtils.removeStartIgnoreCase(resourcePath, "/assets/")
  }

  def updateTheme(theme: Theme, themeFiles: ThemeFiles): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateThemeInternal(theme, themeFiles, onSuccessCalls)
    val reloadNeeded = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    if (reloadNeeded) {
      reloadThemeCss()
    }
  }

  def updateThemeInternal(theme: Theme, themeFiles: ThemeFiles, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Boolean] = {
    for {
      // Look up the existing data for the theme.
      existingTheme <- PersistenceSchema.themes.filter(_.id === theme.id).filter(_.custom === true).result.headOption
      // Update the DB.
      resourcePathsToUse <- {
        if (existingTheme.isDefined) {
          val headerPathToUse = getThemeResourcePathToSave(themeFiles.headerLogo, Some(theme.headerLogoPath), isUpdate = true)
          val footerPathToUse = getThemeResourcePathToSave(themeFiles.footerLogo, Some(theme.footerLogoPath), isUpdate = true)
          val faviconPathToUse = getThemeResourcePathToSave(themeFiles.faviconFile, Some(theme.faviconPath), isUpdate = true)
          PersistenceSchema.themes.filter(_.id === theme.id).map(x =>
            x.key :: x.description :: x.active :: x.separatorTitleColor :: x.modalTitleColor :: x.tableTitleColor :: x.cardTitleColor ::
            x.pageTitleColor :: x.headingColor :: x.tabLinkColor :: x.footerTextColor :: x.headerBackgroundColor ::
            x.headerBorderColor :: x.headerSeparatorColor :: x.headerLogoPath :: x.footerBackgroundColor ::
            x.footerBorderColor :: x.footerLogoPath :: x.footerLogoDisplay :: x.faviconPath ::
            x.primaryButtonColor :: x.primaryButtonLabelColor :: x.primaryButtonHoverColor :: x.primaryButtonActiveColor ::
            x.secondaryButtonColor :: x.secondaryButtonLabelColor :: x.secondaryButtonHoverColor :: x.secondaryButtonActiveColor :: HNil
          ).update(
            theme.key :: theme.description :: theme.active :: theme.separatorTitleColor :: theme.modalTitleColor :: theme.tableTitleColor :: theme.cardTitleColor ::
            theme.pageTitleColor :: theme.headingColor :: theme.tabLinkColor :: theme.footerTextColor :: theme.headerBackgroundColor ::
            theme.headerBorderColor :: theme.headerSeparatorColor :: headerPathToUse :: theme.footerBackgroundColor ::
            theme.footerBorderColor :: footerPathToUse :: theme.footerLogoDisplay :: faviconPathToUse ::
            theme.primaryButtonColor :: theme.primaryButtonLabelColor :: theme.primaryButtonHoverColor :: theme.primaryButtonActiveColor ::
            theme.secondaryButtonColor :: theme.secondaryButtonLabelColor :: theme.secondaryButtonHoverColor :: theme.secondaryButtonActiveColor :: HNil
          ) andThen DBIO.successful(Some(headerPathToUse, footerPathToUse, faviconPathToUse))
        } else {
          DBIO.successful(None)
        }
      }
      // Ensure we have a single active theme.
      reloadNeeded <- {
        if (existingTheme.isDefined) {
          if (existingTheme.get.active && theme.active) {
            // Update to the currently active theme.
            getOrSetActiveTheme() andThen DBIO.successful(true)
          } else if (existingTheme.get.active && !theme.active) {
            // The currently active theme was deactivated.
            getOrSetActiveTheme() andThen DBIO.successful(true)
          } else if (!existingTheme.get.active && theme.active) {
            // The theme was activated. Deactivate the previously active theme and refresh.
            PersistenceSchema.themes.filter(_.id =!= theme.id).filter(_.active === true).map(_.active).update(false) andThen getOrSetActiveTheme() andThen DBIO.successful(true)
          } else {
            // No changes needed.
            DBIO.successful(false)
          }
        } else {
          DBIO.successful(false)
        }
      }
      // Update the file system resources.
      _ <- {
        if (existingTheme.isDefined && resourcePathsToUse.isDefined) {
          updateThemeResource(existingTheme.get.id, existingTheme.get.headerLogoPath, resourcePathsToUse.get._1, themeFiles.headerLogo, onSuccessCalls)
          updateThemeResource(existingTheme.get.id, existingTheme.get.footerLogoPath, resourcePathsToUse.get._2, themeFiles.footerLogo, onSuccessCalls)
          updateThemeResource(existingTheme.get.id, existingTheme.get.faviconPath, resourcePathsToUse.get._3, themeFiles.faviconFile, onSuccessCalls)
        }
        DBIO.successful(())
      }
    } yield reloadNeeded
  }

  private def updateThemeResource(themeId: Long, existingResourcePath: String, newResourcePath: String, newResourceFile: Option[NamedFile], onSuccessCalls: mutable.ListBuffer[() => _]) = {
    if (newResourceFile.isDefined) {
      onSuccessCalls += (() => repositoryUtils.saveThemeResource(themeId, newResourcePath, newResourceFile.get.file))
      onSuccessCalls += (() => repositoryUtils.deleteThemeResource(themeId, existingResourcePath))
    }
  }

  def deleteTheme(themeId: Long): Boolean = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteThemeInternal(themeId, onSuccessCalls)
    val results = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    if (results._2) {
      reloadThemeCss()
    }
    results._1
  }

  def deleteThemeInternal(themeId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[(Boolean, Boolean)] = {
    for {
      // Ensure that we are allowed to delete the theme (only a custom theme can be deleted).
      existingThemeInfo <- PersistenceSchema.themes
          .filter(_.id === themeId)
          .filter(_.custom === true)
          .map(_.active)
          .result
          .headOption
      // Delete the theme.
      reloadNeeded <- {
        if (existingThemeInfo.isDefined) {
          PersistenceSchema.themes.filter(_.id === themeId).delete andThen {
            if (existingThemeInfo.get) {
              // This was the active theme.
              getOrSetActiveTheme() andThen DBIO.successful(true)
            } else {
              DBIO.successful(false)
            }
          }
        } else {
          DBIO.successful(false)
        }
      }
      // Delete the theme's resources.
      _ <- {
        if (existingThemeInfo.isDefined) {
          onSuccessCalls += (() => repositoryUtils.deleteThemeResources(themeId))
        }
        DBIO.successful(())
      }
    } yield (existingThemeInfo.isDefined, reloadNeeded)
  }

  private def deleteInactiveUserAccountsInternal(): DBIO[Option[Int]] = {
    for {
      retentionPeriod <- PersistenceSchema.systemConfigurations.filter(_.name === Constants.AccountRetentionPeriod).map(_.parameter).result.headOption
      usersDeleted <- {
        if (retentionPeriod.isDefined && retentionPeriod.get.isDefined) {
          // Calculate threshold date.
          val now = Calendar.getInstance()
          now.add(Calendar.DAY_OF_YEAR, -1 * Integer.parseInt(retentionPeriod.get.get))
          val minimumStartTime = new Timestamp(now.getTimeInMillis)
          // We must keep users of organisation without with recent tests.
          val organisationsWithRecentTests = PersistenceSchema.testResults
            .filter(_.organizationId.isDefined)
            .filter(_.startTime > minimumStartTime)
            .map(_.organizationId)
            .distinct
          // We must keep users of organisation without any tests that have been recently updated.
          val organisationsWithoutAnyTestsButWithRecentUpdates = PersistenceSchema.organizations
            .filterNot(_.id in PersistenceSchema.testResults.filter(_.organizationId.isDefined).map(_.organizationId).distinct)
            .filter(_.updateTime > minimumStartTime)
            .map(_.id)
          // Delete the matching users.
          PersistenceSchema.users
            .filterNot(_.organization in organisationsWithoutAnyTestsButWithRecentUpdates)
            .filterNot(_.organization in organisationsWithRecentTests)
            .filter(_.id =!= Configurations.DEMOS_ACCOUNT)
            .filter(_.role inSet Set(UserRole.VendorUser.id.toShort, UserRole.VendorAdmin.id.toShort))
            .delete
            .flatMap(deletedCount => {
              DBIO.successful(Some(deletedCount))
            })
        } else {
          DBIO.successful(None)
        }
      }
    } yield usersDeleted
  }

  def deleteInactiveUserAccounts(): Option[Int] = {
    exec(deleteInactiveUserAccountsInternal().transactionally)
  }

  def testEmailSettings(settings: EmailSettings, toAddress: String): Future[Option[List[String]]] = {
    val settingsToUse = if (settings.authEnabled.isDefined && settings.authEnabled.get && settings.authPassword.isEmpty && Configurations.EMAIL_SMTP_AUTH_PASSWORD.isDefined) {
      // No password was received. This means that the existing one should be reuse.
      settings.withPassword(Configurations.EMAIL_SMTP_AUTH_PASSWORD.get)
    } else {
      settings
    }
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
    val promise = Promise[Option[List[String]]]()
    val future = scala.concurrent.Future {
      val subject = "Test Bed test email"
      var content = "<h2>Test Bed email settings verification</h2>"
      content += "Receiving this email confirms that your Test Bed instance's email settings are correctly configured."
      EmailUtil.sendEmail(settingsToUse, Array[String](toAddress), null, subject, content, null)
      None
    }
    future.onComplete {
      case Success(result) => promise.success(result)
      case Failure(exception) => promise.success(Some(extractFailureDetails(exception)))
    }
    promise.future
  }

  def recordDefaultEmailSettings(): EmailSettings = {
    // This is called before we adapt the settings based on stored values.
    defaultEmailSettings = EmailSettings.fromEnvironment()
    defaultEmailSettings
  }

}
