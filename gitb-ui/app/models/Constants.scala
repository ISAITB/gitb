package models

/**
 * Created by VWYNGAET on 07/11/2016.
 */
object Constants {
  // Organization constants
  val DefaultOrganizationId = 0L
  val DefaultCommunityId = 0L
  val AdminOrganizationName = "Admin Organization"

  // System configuration constants
  val SessionAliveTime = "session_alive_time"
  val MasterPassword = "master_password"

  // env variables
  val EnvironmentTheme = "THEME"
  val DefaultTheme = "gitb"
  val EcTheme = "ec"
  val ImagePath = "assets/images/"
  val EcLogo = ImagePath + "ec.png"
  val GitbLogo = ImagePath + "gitb.png"
  val EcFavicon = "public/images/favicon-ec.gif"
  val GitbFavicon = "public/images/favicon.png"

  val domainConfigurationName = "com.gitb.DOMAIN"
  val organisationConfigurationName = "com.gitb.ORGANISATION"
  val systemConfigurationName = "com.gitb.SYSTEM"

  val organisationConfiguration_shortName = "shortName"
  val organisationConfiguration_fullName = "fullName"
  val systemConfiguration_shortName = "shortName"
  val systemConfiguration_fullName = "fullName"
  val systemConfiguration_version = "version"

  val PlaceholderOrganisation = "$ORGANISATION"
  val PlaceholderSystem = "$SYSTEM"
  val PlaceholderSpecification = "$SPECIFICATION"
  val PlaceholderActor = "$ACTOR"
  val PlaceholderDomain = "$DOMAIN"

  val ProductionMode = "production"
  val DevelopmentMode = "development"
  val SandboxMode = "sandbox"

  val FilterDateFormat = "dd-MM-yyyy HH:mm:ss"

  val VersionNumber = "1.15.0b1"
  val ResourceVersionNumber = "1.15.0b1"
}
