package managers

import models.{Constants, SystemConfiguration}
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.MimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer

@Singleton
class SystemConfigurationManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("SystemConfigurationManager")

  /**
   * Gets system config by name
   */
  def getSystemConfiguration(name: String): SystemConfiguration = {
    val sc = exec(PersistenceSchema.systemConfigurations.filter(_.name === name).result.head)
    val config = new SystemConfiguration(sc)
    config
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None) = {
    exec(updateSystemParameterInternal(name, value).transactionally)
  }

  private def updateSystemParameterInternal(name: String, value: Option[String] = None): DBIO[_] = {
    val q = for {c <- PersistenceSchema.systemConfigurations if c.name === name} yield c.parameter
    q.update(value)
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
