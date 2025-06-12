package config

import com.gitb.utils.HmacUtils
import com.typesafe.config.{Config, ConfigFactory}
import ecas.AuthenticationLevel
import models.Constants
import org.apache.commons.lang3.StringUtils

import java.util.Locale
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.matching.Regex

object Configurations {

  private var _IS_LOADED = false
  var STARTUP_FAILURE = false

  // Database parameters
  var DB_JDBC_URL:String = ""
  var DB_USER:String = ""
  var DB_PASSWORD = ""

  var DB_ROOT_URL:String = ""
  var DB_NAME:String = ""
  var DB_MIGRATION_TABLE:String = ""
  var DB_LATEST_DB_BASELINE_SCRIPT: Option[String] = None

  // Redis parameters
  var REDIS_HOST:String = ""
  var REDIS_PORT = 0

  // General configurations
  var AUTHENTICATION_SESSION_MAX_IDLE_TIME = 0
  var AUTHENTICATION_SESSION_MAX_TOTAL_TIME = 0
  var TOKEN_LENGTH = 0
  var TESTBED_SERVICE_URL = ""
  var TESTBED_CLIENT_URL = ""
  var TESTBED_CLIENT_URL_INTERNAL = ""
	var TEST_CASE_REPOSITORY_PATH = ""

  var EMAIL_ENABLED = false
  var EMAIL_FROM: Option[String] = None
  var EMAIL_TO: Option[Array[String]] = None
  var EMAIL_DEFAULT: Option[String] = None
  var EMAIL_SMTP_HOST: Option[String] = None
  var EMAIL_SMTP_PORT: Option[Int] = None
  var EMAIL_SMTP_SSL_ENABLED: Option[Boolean] = None
  var EMAIL_SMTP_SSL_PROTOCOLS: Option[Array[String]] = None
  var EMAIL_SMTP_STARTTLS_ENABLED: Option[Boolean] = None
  var EMAIL_CONTACT_FORM_ENABLED: Option[Boolean] = None
  var EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX: Option[Boolean] = None
  var EMAIL_SMTP_AUTH_ENABLED: Option[Boolean] = None
  var EMAIL_SMTP_AUTH_USERNAME: Option[String] = None
  var EMAIL_SMTP_AUTH_PASSWORD: Option[String] = None
  var EMAIL_ATTACHMENTS_MAX_SIZE: Int = -1
  var EMAIL_ATTACHMENTS_MAX_COUNT: Int = -1
  var EMAIL_ATTACHMENTS_ALLOWED_TYPES: Set[String] = _
  var EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER: Int = -1

  var SURVEY_ENABLED = false
  var SURVEY_ADDRESS = ""
  var MORE_INFO_ENABLED = false
  var MORE_INFO_ADDRESS = ""
  var RELEASE_INFO_ENABLED = false
  var RELEASE_INFO_ADDRESS = ""

  var USERGUIDE_OU = ""
  var USERGUIDE_OA = ""
  var USERGUIDE_TA = ""
  var USERGUIDE_CA = ""

  var GUIDES_EULOGIN_USE = ""
  var GUIDES_EULOGIN_MIGRATION = ""

  var ANTIVIRUS_SERVER_ENABLED = false
  var ANTIVIRUS_SERVER_HOST = ""
  var ANTIVIRUS_SERVER_PORT: Int = -1
  var ANTIVIRUS_SERVER_TIMEOUT = 0

  var PROXY_SERVER_ENABLED = false
  var PROXY_SERVER_HOST = ""
  var PROXY_SERVER_PORT: Int = -1
  var PROXY_SERVER_AUTH_ENABLED = false
  var PROXY_SERVER_AUTH_USERNAME = ""
  var PROXY_SERVER_AUTH_PASSWORD = ""

  var TSA_SERVER_ENABLED = false
  var TSA_SERVER_URL = ""

  var VALIDATION_TDL_EXTERNAL_ENABLED = false
  var VALIDATION_TDL_EXTERNAL_URL = ""

  var MASTER_PASSWORD: Array[Char] = _
  var MASTER_PASSWORD_TO_REPLACE: Option[Array[Char]] = None
  var MASTER_PASSWORD_FORCE = false

  var AUTHENTICATION_SSO_ENABLED = false
  var AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = false
  var AUTHENTICATION_SSO_LOGIN_URL = ""
  var AUTHENTICATION_SSO_PREFIX_URL: Option[String] = None
  var AUTHENTICATION_SSO_AUTHENTICATION_LEVEL: Option[AuthenticationLevel] = None
  var AUTHENTICATION_SSO_AUTHENTICATION_LEVEL_PARAMETER = "authenticationLevel"
  var AUTHENTICATION_SSO_CALLBACK_URL = ""
  var AUTHENTICATION_SSO_CAS_VERSION: Short = 2
  var AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS: String = "userDetails"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL: String = "email"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME: String = "firstName"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME: String = "lastName"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__AUTHENTICATION_LEVEL: String = "authenticationLevel"
  var AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX: String = "laxValidate"

  var DEMOS_ENABLED = false
  var DEMOS_ACCOUNT:Long = -1

  var REGISTRATION_ENABLED = true
  var TESTBED_HOME_LINK: String = "/"

  // 5120 KB default (5 MBs)
  var SAVED_FILE_MAX_SIZE: Long = 5120

  var CONFORMANCE_STATEMENT_REPORT_MAX_TEST_CASES: Int = 100

  var INPUT_SANITIZER__ENABLED = true
  var INPUT_SANITIZER__METHODS_TO_CHECK:Set[String] = _
  var INPUT_SANITIZER__DEFAULT_BLACKLIST_EXPRESSION:Regex = _
  var INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS:Map[String, Regex] = _
  var INPUT_SANITIZER__PARAMETERS_TO_SKIP:Set[String] = _
  var INPUT_SANITIZER__PARAMETERS_AS_JSON:Set[String] = _

  var DATA_ARCHIVE_KEY = ""
  var DATA_WEB_INIT_ENABLED = false

  var TEST_SESSION_ARCHIVE_THRESHOLD = 30
  var TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD = 500L
  var PASSWORD_COMPLEXITY_RULE_REGEX:Regex = _

  var TESTBED_MODE:String = ""

  var WEB_CONTEXT_ROOT = ""
  var WEB_CONTEXT_ROOT_WITH_SLASH = ""
  var PUBLIC_CONTEXT_ROOT = ""
  var PUBLIC_CONTEXT_ROOT_WITH_SLASH = ""
  var API_PREFIX = ""
  var API_ROOT = ""
  var AUTOMATION_API_ENABLED = false
  var AUTOMATION_API_MASTER_KEY: Option[String] = None
  var BUILD_TIMESTAMP = ""

  val WELCOME_MESSAGE_DEFAULT = "<h4>The Interoperability Test Bed is a platform for self-service conformance testing against semantic and technical specifications.</h4>"
  var WELCOME_MESSAGE: String = WELCOME_MESSAGE_DEFAULT
  var SESSION_COOKIE_SECURE: Boolean = false

  def versionInfo(): String = {
    if (Constants.VersionNumber.toLowerCase.endsWith("snapshot")) {
      Constants.VersionNumber + " (" + Configurations.BUILD_TIMESTAMP + ")"
    } else {
      Constants.VersionNumber
    }
  }

  def mainVersionNumber(): String = {
    StringUtils.removeEnd(Constants.VersionNumber.toLowerCase(Locale.getDefault), "-snapshot")
  }

  def loadConfigurations(): Unit = {
    if (!_IS_LOADED) {
      // Load configuration file
      val conf:Config = ConfigFactory.load()
      // Context paths - start
      WEB_CONTEXT_ROOT = fromEnv("WEB_CONTEXT_ROOT", conf.getString("play.http.context"))
      WEB_CONTEXT_ROOT_WITH_SLASH = StringUtils.appendIfMissing(WEB_CONTEXT_ROOT, "/")
      API_PREFIX = conf.getString("apiPrefix")
      API_ROOT = WEB_CONTEXT_ROOT_WITH_SLASH + API_PREFIX
      PUBLIC_CONTEXT_ROOT = fromEnv("AUTHENTICATION_COOKIE_PATH", WEB_CONTEXT_ROOT)
      PUBLIC_CONTEXT_ROOT_WITH_SLASH = StringUtils.appendIfMissing(PUBLIC_CONTEXT_ROOT, "/")
      // Context paths - end
      //Parse DB Parameters
      DB_JDBC_URL     = conf.getString("db.default.url")
      DB_USER         = conf.getString("db.default.user")
      DB_PASSWORD     = conf.getString("db.default.password")
      DB_ROOT_URL     = conf.getString("db.default.rooturl")
      DB_NAME         = conf.getString("db.default.name")
      DB_MIGRATION_TABLE = conf.getString("db.default.migration.table")
      DB_LATEST_DB_BASELINE_SCRIPT = Some(conf.getString("latestDbBaselineScript"))

      //Parse Redis Parameters
      REDIS_HOST = conf.getString("redis.host")
      REDIS_PORT = conf.getInt("redis.port")

      //General Parameters
      AUTHENTICATION_SESSION_MAX_IDLE_TIME = fromEnv("AUTHENTICATION_SESSION_MAX_IDLE_TIME", conf.getString("authentication.session.maxIdleTime")).toInt
      AUTHENTICATION_SESSION_MAX_TOTAL_TIME = fromEnv("AUTHENTICATION_SESSION_MAX_TOTAL_TIME", conf.getString("authentication.session.maxTotalTime")).toInt
      TOKEN_LENGTH = conf.getInt("token.length")
      TESTBED_SERVICE_URL = conf.getString("testbed.service.url")
      TESTBED_CLIENT_URL  = conf.getString("testbed.client.url")
      TESTBED_CLIENT_URL_INTERNAL = fromEnv("TESTBED_CLIENT_URL_INTERNAL", TESTBED_CLIENT_URL)

      TEST_CASE_REPOSITORY_PATH = conf.getString("testcase.repository.path")

      EMAIL_ENABLED = fromEnv("EMAIL_ENABLED", conf.getString("email.enabled")).toBoolean
      EMAIL_FROM = Option(fromEnv("EMAIL_FROM", null))
      EMAIL_TO = Option(fromEnv("EMAIL_TO", null)).map(_.split(","))
      EMAIL_DEFAULT = Option(fromEnv("EMAIL_DEFAULT", "DIGIT-ITB@ec.europa.eu"))
      EMAIL_SMTP_HOST = Option(fromEnv("EMAIL_SMTP_HOST", null))
      EMAIL_SMTP_PORT = Option(fromEnv("EMAIL_SMTP_PORT", null)).map(_.toInt)
      EMAIL_SMTP_AUTH_ENABLED = Option(fromEnv("EMAIL_SMTP_AUTH_ENABLED", conf.getString("email.smtp.auth.enabled")).toBoolean)
      EMAIL_SMTP_AUTH_USERNAME = Option(fromEnv("EMAIL_SMTP_AUTH_USERNAME", null))
      EMAIL_SMTP_AUTH_PASSWORD = Option(fromEnv("EMAIL_SMTP_AUTH_PASSWORD", conf.getString("email.smtp.auth.password")))
      // Collect as Properties object
      EMAIL_SMTP_SSL_ENABLED = Option(fromEnv("EMAIL_SMTP_SSL_ENABLED", conf.getString("email.smtp.ssl.enabled")).toBoolean)
      val sslProtocolsValue = fromEnv("EMAIL_SMTP_SSL_PROTOCOLS", conf.getString("email.smtp.ssl.protocols")).split(",")
      if (sslProtocolsValue.nonEmpty) {
        EMAIL_SMTP_SSL_PROTOCOLS = Some(sslProtocolsValue)
      }
      EMAIL_SMTP_STARTTLS_ENABLED = Option(fromEnv("EMAIL_SMTP_STARTTLS_ENABLED", conf.getString("email.smtp.starttls.enabled")).toBoolean)
      EMAIL_CONTACT_FORM_ENABLED = Some(fromEnv("EMAIL_CONTACT_FORM_ENABLED", "true").toBoolean)
      EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX = Some(fromEnv("EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX", "true").toBoolean)
      EMAIL_ATTACHMENTS_MAX_SIZE = fromEnv("EMAIL_ATTACHMENTS_MAX_SIZE", conf.getString("email.attachments.maxSize")).toInt
      EMAIL_ATTACHMENTS_MAX_COUNT = fromEnv("EMAIL_ATTACHMENTS_MAX_COUNT", conf.getString("email.attachments.maxCount")).toInt
      val typesStr = fromEnv("EMAIL_ATTACHMENTS_ALLOWED_TYPES", conf.getString("email.attachments.allowedTypes"))
      var tempSet = new scala.collection.mutable.HashSet[String]()
      typesStr.split(",").map(_.trim).foreach{ mimeType =>
        tempSet += mimeType
      }
      EMAIL_ATTACHMENTS_ALLOWED_TYPES = tempSet.toSet
      EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER = fromEnv("EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER", "30").toInt

      SURVEY_ENABLED = fromEnv("SURVEY_ENABLED", conf.getString("survey.enabled")).toBoolean
      SURVEY_ADDRESS = fromEnv("SURVEY_ADDRESS", conf.getString("survey.address"))
      MORE_INFO_ENABLED = fromEnv("MORE_INFO_ENABLED", conf.getString("moreinfo.enabled")).toBoolean
      MORE_INFO_ADDRESS = fromEnv("MORE_INFO_ADDRESS", conf.getString("moreinfo.address"))
      RELEASE_INFO_ENABLED = fromEnv("RELEASE_INFO_ENABLED", conf.getString("releaseinfo.enabled")).toBoolean
      RELEASE_INFO_ADDRESS = fromEnv("RELEASE_INFO_ADDRESS", conf.getString("releaseinfo.address"))

      USERGUIDE_OU = replaceUserGuideRelease(fromEnv("USERGUIDE_OU", conf.getString("userguide.ou")))
      USERGUIDE_OA = replaceUserGuideRelease(fromEnv("USERGUIDE_OA", conf.getString("userguide.oa")))
      USERGUIDE_CA = replaceUserGuideRelease(fromEnv("USERGUIDE_CA", conf.getString("userguide.ca")))
      USERGUIDE_TA = replaceUserGuideRelease(fromEnv("USERGUIDE_TA", conf.getString("userguide.ta")))

      ANTIVIRUS_SERVER_ENABLED = fromEnv("ANTIVIRUS_SERVER_ENABLED", conf.getString("antivirus.enabled")).toBoolean
      if (ANTIVIRUS_SERVER_ENABLED) {
        ANTIVIRUS_SERVER_HOST = fromEnv("ANTIVIRUS_SERVER_HOST", conf.getString("antivirus.host"))
        ANTIVIRUS_SERVER_PORT = fromEnv("ANTIVIRUS_SERVER_PORT", conf.getString("antivirus.port")).toInt
        ANTIVIRUS_SERVER_TIMEOUT = fromEnv("ANTIVIRUS_SERVER_TIMEOUT", conf.getString("antivirus.timeout")).toInt
      }

      MASTER_PASSWORD = fromEnv("MASTER_PASSWORD", conf.getString("masterPassword")).toCharArray
      val replacement = fromEnv("MASTER_PASSWORD_TO_REPLACE", "")
      if (replacement.nonEmpty) {
        MASTER_PASSWORD_TO_REPLACE = Some(replacement.toCharArray)
      }
      MASTER_PASSWORD_FORCE = fromEnv("MASTER_PASSWORD_FORCE", "false").toBoolean

      PROXY_SERVER_ENABLED = fromEnv("PROXY_SERVER_ENABLED", conf.getString("proxy.enabled")).toBoolean
      if (PROXY_SERVER_ENABLED) {
        PROXY_SERVER_HOST = fromEnv("PROXY_SERVER_HOST", conf.getString("proxy.host"))
        PROXY_SERVER_PORT = fromEnv("PROXY_SERVER_PORT", conf.getString("proxy.port")).toInt
        PROXY_SERVER_AUTH_ENABLED = fromEnv("PROXY_SERVER_AUTH_ENABLED", conf.getString("proxy.auth.enabled")).toBoolean
        if (PROXY_SERVER_AUTH_ENABLED) {
          PROXY_SERVER_AUTH_USERNAME = fromEnv("PROXY_SERVER_AUTH_USERNAME", conf.getString("proxy.auth.user"))
          PROXY_SERVER_AUTH_PASSWORD = fromEnv("PROXY_SERVER_AUTH_PASSWORD", conf.getString("proxy.auth.password"))
        }
      }

      TSA_SERVER_ENABLED = fromEnv("TSA_SERVER_ENABLED", conf.getString("signature.tsa.enabled")).toBoolean
      if (TSA_SERVER_ENABLED) {
        TSA_SERVER_URL = fromEnv("TSA_SERVER_URL", conf.getString("signature.tsa.url"))
      }

      VALIDATION_TDL_EXTERNAL_ENABLED = fromEnv("VALIDATION_TDL_EXTERNAL_ENABLED", conf.getString("validation.tdl.external.enabled")).toBoolean
      if (VALIDATION_TDL_EXTERNAL_ENABLED) {
        VALIDATION_TDL_EXTERNAL_URL = fromEnv("VALIDATION_TDL_EXTERNAL_URL", conf.getString("validation.tdl.external.url"))
      }

      // Configure HMAC processing
      val hmacKey = fromEnv("HMAC_KEY", conf.getString("hmac.key"))
      val hmacKeyWindow = fromEnv("HMAC_WINDOW", conf.getString("hmac.window"))
      HmacUtils.configure(hmacKey, hmacKeyWindow.toLong)

      AUTHENTICATION_SSO_ENABLED = fromEnv("AUTHENTICATION_SSO_ENABLED", conf.getString("authentication.sso.enabled")).toBoolean
      AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = fromEnv("AUTHENTICATION_SSO_IN_MIGRATION_PERIOD", conf.getString("authentication.sso.inMigrationPeriod")).toBoolean
      AUTHENTICATION_SSO_LOGIN_URL = fromEnv("AUTHENTICATION_SSO_LOGIN_URL", conf.getString("authentication.sso.url.login"))
      AUTHENTICATION_SSO_PREFIX_URL = Option(fromEnv("AUTHENTICATION_SSO_PREFIX_URL", "")).filter(StringUtils.isNotBlank)
      AUTHENTICATION_SSO_AUTHENTICATION_LEVEL = Option(fromEnv("AUTHENTICATION_SSO_AUTHENTICATION_LEVEL", conf.getString("authentication.sso.authenticationLevel"))).filter(StringUtils.isNotBlank).map(AuthenticationLevel.fromName)
      AUTHENTICATION_SSO_AUTHENTICATION_LEVEL_PARAMETER = fromEnv("AUTHENTICATION_SSO_AUTHENTICATION_LEVEL_PARAMETER", conf.getString("authentication.sso.authenticationLevelParameter"))
      AUTHENTICATION_SSO_CALLBACK_URL = fromEnv("AUTHENTICATION_SSO_CALLBACK_URL", conf.getString("authentication.sso.url.callback"))
      AUTHENTICATION_SSO_CAS_VERSION = fromEnv("AUTHENTICATION_SSO_CAS_VERSION", conf.getString("authentication.sso.casVersion")).toShort

      AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS = fromEnv("AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS", conf.getString("authentication.sso.customParameters.userDetails"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL", conf.getString("authentication.sso.userAttributes.email"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME", conf.getString("authentication.sso.userAttributes.firstName"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME", conf.getString("authentication.sso.userAttributes.lastName"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__AUTHENTICATION_LEVEL = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__AUTHENTICATION_LEVEL", conf.getString("authentication.sso.userAttributes.authenticationLevel"))
      AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX = fromEnv("AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX", conf.getString("authentication.sso.ticketValidationUrlSuffix"))

      DEMOS_ENABLED = fromEnv("DEMOS_ENABLED", conf.getString("demos.enabled")).toBoolean
      DEMOS_ACCOUNT = fromEnv("DEMOS_ACCOUNT", conf.getString("demos.account")).toLong

      REGISTRATION_ENABLED = fromEnv("REGISTRATION_ENABLED", conf.getString("registration.enabled")).toBoolean
      TESTBED_HOME_LINK = fromEnv("TESTBED_HOME_LINK", TESTBED_HOME_LINK)

      SAVED_FILE_MAX_SIZE = fromEnv("SAVED_FILE_MAX_SIZE", SAVED_FILE_MAX_SIZE.toString).toLong

      GUIDES_EULOGIN_USE = fromEnv("GUIDES_EULOGIN_USE", conf.getString("guides.eulogin.use"))
      GUIDES_EULOGIN_MIGRATION = fromEnv("GUIDES_EULOGIN_MIGRATION", conf.getString("guides.eulogin.migration"))

      // Input sanitiser - START
      INPUT_SANITIZER__ENABLED = fromEnv("INPUT_SANITIZER__ENABLED", conf.getString("inputSanitizer.enabled")).toBoolean
      val sanitizerMethodsToCheck = fromEnv("INPUT_SANITIZER__METHODS_TO_CHECK", conf.getString("inputSanitizer.methodsToCheck"))
      tempSet = new scala.collection.mutable.HashSet[String]()
      sanitizerMethodsToCheck.split(",").map(_.trim).foreach{ method =>
        tempSet += method
      }
      INPUT_SANITIZER__METHODS_TO_CHECK = tempSet.toSet
      INPUT_SANITIZER__DEFAULT_BLACKLIST_EXPRESSION = new Regex(fromEnv("INPUT_SANITIZER__DEFAULT_BLACKLIST_EXPRESSION", conf.getString("inputSanitizer.defaultBlacklistExpression")))
      val sanitizerExpressionsConfig = conf.getObjectList("inputSanitizer.parameterWhitelistExpressions")
      if (sanitizerExpressionsConfig == null) {
        INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS = Map()
      } else {
        val tempMap: scala.collection.mutable.Map[String, Regex] = scala.collection.mutable.Map()
        sanitizerExpressionsConfig.forEach { entry =>
          entry.entrySet().forEach { mapping =>
            tempMap(mapping.getKey) = new Regex(mapping.getValue.unwrapped.asInstanceOf[String])
          }
        }
        INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS = tempMap.iterator.toMap
      }
      val sanitizerSkipped = conf.getStringList("inputSanitizer.parametersToSkip")
      if (sanitizerSkipped == null) {
        INPUT_SANITIZER__PARAMETERS_TO_SKIP = Set()
      } else {
        INPUT_SANITIZER__PARAMETERS_TO_SKIP = sanitizerSkipped.asScala.toSet
      }
      val sanitizerJsonParameters = conf.getStringList("inputSanitizer.parametersAsJson")
      if (sanitizerJsonParameters == null) {
        INPUT_SANITIZER__PARAMETERS_AS_JSON = Set()
      } else {
        INPUT_SANITIZER__PARAMETERS_AS_JSON = sanitizerJsonParameters.asScala.toSet
      }
      // Input sanitiser - END

      DATA_ARCHIVE_KEY = fromEnv("DATA_ARCHIVE_KEY", conf.getString("dataArchive.key"))
      DATA_WEB_INIT_ENABLED = fromEnv("DATA_WEB_INIT_ENABLED", "false").toBoolean
      TEST_SESSION_ARCHIVE_THRESHOLD = fromEnv("TEST_SESSION_ARCHIVE_THRESHOLD", conf.getString("testsession.archive.threshold")).toInt
      TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD = fromEnv("TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD", conf.getString("testsession.embeddedReportData.threshold")).toLong
      PASSWORD_COMPLEXITY_RULE_REGEX = new Regex(conf.getString("passwordComplexityRule"))
      AUTOMATION_API_ENABLED = fromEnv("AUTOMATION_API_ENABLED", "false").toBoolean
      // Master API key
      val masterApiKey = fromEnv("AUTOMATION_API_MASTER_KEY", conf.getString("masterApiKey"))
      if (StringUtils.isNotBlank(masterApiKey)) {
        AUTOMATION_API_MASTER_KEY = Some(masterApiKey.trim)
      }
      // Session cookie
      SESSION_COOKIE_SECURE = fromEnv("SESSION_SECURE", conf.getString("play.http.session.secure")).toBoolean
      // Max test cases to include in detailed conformance statement reports.
      CONFORMANCE_STATEMENT_REPORT_MAX_TEST_CASES = fromEnv("CONFORMANCE_STATEMENT_REPORT_MAX_TEST_CASES", "100").toInt
      _IS_LOADED = true
    }
  }

  private def fromEnv(propertyName: String, default: String): String = {
    sys.env.getOrElse(propertyName, default)
  }

  def restApiSwaggerLink(): Option[String] = {
    if (Configurations.AUTOMATION_API_ENABLED) {
      Some(Configurations.API_PREFIX + "/rest/swagger")
    } else {
      None
    }
  }

  def restApiJsonLink(): Option[String] = {
    if (Configurations.AUTOMATION_API_ENABLED) {
      Some(Configurations.API_PREFIX + "/rest")
    } else {
      None
    }
  }

  private def replaceUserGuideRelease(link: String): String = {
    val versionNumberForDocs = if (Constants.VersionNumber.toLowerCase().contains("snapshot")) {
      "latest"
    } else {
      Constants.VersionNumber
    }
    StringUtils.replace(link, "{RELEASE}", versionNumberForDocs)
  }

}
