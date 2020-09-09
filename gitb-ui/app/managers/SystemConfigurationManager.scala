package managers

import javax.inject.{Inject, Singleton}
import models.{Constants, SystemConfiguration}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

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
    val q = for {c <- PersistenceSchema.systemConfigurations if c.name === name} yield (c.parameter)
    exec(q.update(value).transactionally)
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

}
