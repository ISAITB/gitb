package managers

import config.Configurations
import models.{Constants, SystemConfigurations, SystemConfigurationsWithEnvironment}
import org.mindrot.jbcrypt.BCrypt
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.MimeUtil

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SystemConfigurationManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

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

  def getFaviconPath(): String = {
    val env = sys.env.get(Constants.EnvironmentTheme)
    parseFavicon(env)
  }

  def getLogoPath(): String = {
    val env = sys.env.get(Constants.EnvironmentTheme)
    parseLogo(env)
  }

  def getFooterLogoPath(): String = {
    val env = sys.env.get(Constants.EnvironmentTheme)
    parseFooterLogo(env)
  }

  private def parseLogo(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      Constants.EcLogo
    } else {
      Constants.GitbLogo
    }
  }

  private def parseFooterLogo(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      Constants.GitbLogo
    } else {
      ""
    }
  }

  private def parseFavicon(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      Constants.EcFavicon
    } else {
      Constants.GitbFavicon
    }
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

}
