package managers

import config.Configurations
import models.theme.{Theme, ThemeFiles}
import models.{Constants, NamedFile, SystemConfigurations, SystemConfigurationsWithEnvironment}
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils}

import java.io.File
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SystemConfigurationManager @Inject() (repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemConfigurationManager])

  private var activeThemeId: Option[Long] = None
  private var activeThemeCss: Option[String] = None
  private var activeThemeFavicon: Option[String] = None

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
        .filter(_.name inSet Set(
          Constants.RestApiEnabled,
          Constants.SessionAliveTime,
          Constants.SelfRegistrationEnabled,
          Constants.DemoAccount,
          Constants.WelcomeMessage)
        )
        .map(x => (x.name, x.parameter))
        .result
    ).map(x => SystemConfigurationsWithEnvironment(SystemConfigurations(x._1, x._2, None), defaultSetting = false, environmentSetting = false)).toList
    if (!onlyPersisted) {
      val restApiEnabledConfig = persistedConfigs.find(config => config.config.name == Constants.RestApiEnabled)
      val demoAccountConfig = persistedConfigs.find(config => config.config.name == Constants.DemoAccount)
      val selfRegistrationConfig = persistedConfigs.find(config => config.config.name == Constants.SelfRegistrationEnabled)
      val welcomeMessageConfig = persistedConfigs.find(config => config.config.name == Constants.WelcomeMessage)
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
    }
    persistedConfigs
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None): Option[String] = {
    // Persist in the DB.
    if (name == Constants.WelcomeMessage && value.isEmpty) {
      exec(PersistenceSchema.systemConfigurations.filter(_.name === name).delete.transactionally)
    } else {
      exec(updateSystemParameterInternal(name, value).transactionally)
    }
    var returnValue: Option[String] = None
    // Now apply also to the current instance.
    name match {
      case Constants.RestApiEnabled =>
        if (value.isDefined) {
          Configurations.AUTOMATION_API_ENABLED = value.get.toBoolean
        }
      case Constants.SelfRegistrationEnabled =>
        if (value.isDefined) {
          Configurations.REGISTRATION_ENABLED = value.get.toBoolean
        }
      case Constants.DemoAccount =>
        if (value.isDefined) {
          Configurations.DEMOS_ENABLED = true
          Configurations.DEMOS_ACCOUNT = value.get.toLong
        } else {
          Configurations.DEMOS_ENABLED = false
          Configurations.DEMOS_ACCOUNT = -1
        }
      case Constants.WelcomeMessage =>
        if (value.isDefined) {
          Configurations.WELCOME_MESSAGE = value.get
        } else {
          Configurations.WELCOME_MESSAGE = Configurations.WELCOME_MESSAGE_DEFAULT
          returnValue = Some(Configurations.WELCOME_MESSAGE_DEFAULT)
        }
      case _ => // No action needed.
    }
    returnValue
  }

  private def updateSystemParameterInternal(name: String, value: Option[String] = None): DBIO[_] = {
    for {
      exists <- PersistenceSchema.systemConfigurations.filter(_.name === name).exists.result
      _ <- if (exists) {
        PersistenceSchema.systemConfigurations.filter(_.name === name).map(_.parameter).update(value)
      } else {
        PersistenceSchema.systemConfigurations += SystemConfigurations(name, value, None)
      }
    } yield ()
  }

  def updateMasterPassword(previousPassword: Array[Char], newPassword: Array[Char]): Unit = {
    val dbAction: DBIO[_] = for {
      // Update the hashed stored value
      _ <- updateSystemParameterInternal(Constants.MasterPassword, Some(BCrypt.hashpw(String.valueOf(newPassword), BCrypt.gensalt())))
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
      // Update conformance certificate keys
      certificateSettings <- PersistenceSchema.conformanceCertificates.map(x => (x.id, x.keyPassword, x.keystorePassword)).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        certificateSettings.foreach { setting =>
          if (setting._2.isDefined || setting._3.isDefined) {
            var keyPassToSet: Option[String] = None
            if (setting._2.isDefined) {
              keyPassToSet = Some(MimeUtil.encryptString(MimeUtil.decryptString(setting._2.get, previousPassword), newPassword))
            }
            var keystorePassToSet: Option[String] = None
            if (setting._3.isDefined) {
              keystorePassToSet = Some(MimeUtil.encryptString(MimeUtil.decryptString(setting._3.get, previousPassword), newPassword))
            }
            val q = for { c <- PersistenceSchema.conformanceCertificates if c.id === setting._1 } yield (c.keyPassword, c.keystorePassword)
            actions += q.update(keyPassToSet, keystorePassToSet)
          }
        }
        toDBIO(actions)
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
          PersistenceSchema.themes.filter(_.id === theme.id).map(x => (
            x.key, x.description, x.active, x.separatorTitleColor, x.modalTitleColor, x.tableTitleColor, x.cardTitleColor,
            x.pageTitleColor, x.headingColor, x.tabLinkColor, x.footerTextColor, x.headerBackgroundColor,
            x.headerBorderColor, x.headerSeparatorColor, x.headerLogoPath, x.footerBackgroundColor,
            x.footerBorderColor, x.footerLogoPath, x.footerLogoDisplay, x.faviconPath
          )).update(
            theme.key, theme.description, theme.active, theme.separatorTitleColor, theme.modalTitleColor, theme.tableTitleColor, theme.cardTitleColor,
            theme.pageTitleColor, theme.headingColor, theme.tabLinkColor, theme.footerTextColor, theme.headerBackgroundColor,
            theme.headerBorderColor, theme.headerSeparatorColor, headerPathToUse, theme.footerBackgroundColor,
            theme.footerBorderColor, footerPathToUse, theme.footerLogoDisplay, faviconPathToUse
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

}
