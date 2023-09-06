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
  val RestApiEnabled = "rest_api_enabled"
  val SelfRegistrationEnabled = "self_registration_enabled"
  val MasterPassword = "master_password"
  val DemoAccount = "demo_account"
  val WelcomeMessage = "welcome"

  // env variables
  val EnvironmentTheme = "THEME"
  val DefaultTheme = "gitb"
  val EcTheme = "ec"
  val ImagePath = "assets/images/"
  val EcLogo: String = ImagePath + "ec.png"
  val GitbLogo: String = ImagePath + "gitb.png"
  val EcFavicon = "public/images/favicon-ec.gif"
  val GitbFavicon = "public/images/favicon.png"

  val domainTestVariable = "DOMAIN"
  val organisationTestVariable = "ORGANISATION"
  val systemTestVariable = "SYSTEM"

  val domainConfigurationName: String = "com.gitb."+domainTestVariable
  val organisationConfigurationName: String = "com.gitb."+organisationTestVariable
  val systemConfigurationName: String = "com.gitb."+systemTestVariable

  val organisationConfiguration_shortName = "shortName"
  val organisationConfiguration_fullName = "fullName"
  val systemConfiguration_shortName = "shortName"
  val systemConfiguration_fullName = "fullName"
  val systemConfiguration_version = "version"

  val PlaceholderOrganisation = "$ORGANISATION"
  val PlaceholderSystem = "$SYSTEM"
  val PlaceholderSpecification = "$SPECIFICATION"
  val PlaceholderSpecificationGroup = "$SPECIFICATION_GROUP"
  val PlaceholderSpecificationGroupOption = "$SPECIFICATION_GROUP_OPTION"
  val PlaceholderActor = "$ACTOR"
  val PlaceholderDomain = "$DOMAIN"

  val ProductionMode = "production"
  val DevelopmentMode = "development"
  val SandboxMode = "sandbox"

  val FilterDateFormat = "dd-MM-yyyy HH:mm:ss"
  val AutomationHeader = "ITB_API_KEY"

  val defaultPage = 1L
  val defaultLimit = 10L

  // When ending in "-snapshot", this is considered a non-published release.
  val VersionNumber = "1.21.0-snapshot"
}
