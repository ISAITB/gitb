package config

import java.util.Properties

import com.gitb.utils.HmacUtils
import com.typesafe.config.{Config, ConfigFactory}

object Configurations {

  var _IS_LOADED = false

  // Database parameters
  var DB_DRIVER_CLASS:String = ""
  var DB_JDBC_URL:String = ""
  var DB_USER:String = ""
  var DB_PASSWORD = ""

  var DB_ROOT_URL:String = ""
  var DB_NAME:String = ""

  // Redis parameters
  var REDIS_HOST:String = ""
  var REDIS_PORT = 0

  //General configurations
  var SERVER_REQUEST_TIMEOUT_IN_SECONDS=0
  var AUTHENTICATION_SESSION_MAX_IDLE_TIME = 0
  var AUTHENTICATION_SESSION_MAX_TOTAL_TIME = 0
  var TOKEN_LENGTH = 0
  var TESTBED_SERVICE_URL = ""
  var TESTBED_CLIENT_URL = ""
	var TEST_CASE_REPOSITORY_PATH = ""

  var EMAIL_ENABLED = false
  var EMAIL_FROM = ""
  var EMAIL_TO: Array[String] = null
  var EMAIL_SMTP_HOST = ""
  var EMAIL_SMTP_PORT = -1
  var EMAIL_SMTP_AUTH_ENABLED = true
  var EMAIL_SMTP_AUTH_USERNAME = ""
  var EMAIL_SMTP_AUTH_PASSWORD = ""
  var SURVEY_ENABLED = false
  var SURVEY_ADDRESS = ""

  var USERGUIDE_OU = ""
  var USERGUIDE_OA = ""
  var USERGUIDE_TA = ""
  var USERGUIDE_CA = ""

  var GUIDES_EULOGIN_USE = ""
  var GUIDES_EULOGIN_MIGRATION = ""

  var EMAIL_ATTACHMENTS_MAX_SIZE = -1
  var EMAIL_ATTACHMENTS_MAX_COUNT = -1
  var EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR = ""
  var EMAIL_ATTACHMENTS_ALLOWED_TYPES: Set[String] = null

  var ANTIVIRUS_SERVER_ENABLED = false
  var ANTIVIRUS_SERVER_HOST = ""
  var ANTIVIRUS_SERVER_PORT = -1
  var ANTIVIRUS_SERVER_TIMEOUT = 0

  var PROXY_SERVER_ENABLED = false
  var PROXY_SERVER_HOST = ""
  var PROXY_SERVER_PORT = -1
  var PROXY_SERVER_AUTH_ENABLED = false
  var PROXY_SERVER_AUTH_USERNAME = ""
  var PROXY_SERVER_AUTH_PASSWORD = ""

  var TSA_SERVER_ENABLED = false
  var TSA_SERVER_URL = ""

  var VALIDATION_TDL_EXTERNAL_ENABLED = false
  var VALIDATION_TDL_EXTERNAL_URL = ""

  var MASTER_PASSWORD: Array[Char] = null

  var AUTHENTICATION_COOKIE_PATH = ""
  var AUTHENTICATION_SSO_ENABLED = false
  var AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = false
  var AUTHENTICATION_SSO_LOGIN_URL = ""
  var AUTHENTICATION_SSO_CALLBACK_URL = ""
  var AUTHENTICATION_SSO_CAS_VERSION: Short = 2
  var AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS: String = "userDetails"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL: String = "email"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME: String = "firstName"
  var AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME: String = "lastName"
  var AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX: String = "laxValidate"

  var DEMOS_ENABLED = false
  var DEMOS_ACCOUNT:Long = -1

  var REGISTRATION_ENABLED = true
  var TESTBED_HOME_LINK: String = "/"

  // 1024 KB default
  var SAVED_FILE_MAX_SIZE: Long = 1024

  var SMTP_PROPERTIES = new Properties()

  def loadConfigurations() = {
    if (!_IS_LOADED) {
      //Load configuration file
      val conf:Config = ConfigFactory.load()

      //Parse DB Parameters
      DB_DRIVER_CLASS = conf.getString("db.default.driver")
      DB_JDBC_URL     = conf.getString("db.default.url")
      DB_USER         = conf.getString("db.default.user")
      DB_PASSWORD     = conf.getString("db.default.password")
      DB_ROOT_URL     = conf.getString("db.default.rooturl")
      DB_NAME         = conf.getString("db.default.name")

      //Parse Redis Parameters
      REDIS_HOST = conf.getString("redis.host")
      REDIS_PORT = conf.getInt("redis.port")

      //General Parameters
      AUTHENTICATION_SESSION_MAX_IDLE_TIME = fromEnv("AUTHENTICATION_SESSION_MAX_IDLE_TIME", conf.getString("authentication.session.maxIdleTime")).toInt
      AUTHENTICATION_SESSION_MAX_TOTAL_TIME = fromEnv("AUTHENTICATION_SESSION_MAX_TOTAL_TIME", conf.getString("authentication.session.maxTotalTime")).toInt
      TOKEN_LENGTH = conf.getInt("token.length")
      SERVER_REQUEST_TIMEOUT_IN_SECONDS = conf.getInt("server.request.timeout.seconds")
      TESTBED_SERVICE_URL = conf.getString("testbed.service.url")
      TESTBED_CLIENT_URL  = conf.getString("testbed.client.url")

      TEST_CASE_REPOSITORY_PATH = conf.getString("testcase.repository.path")

      EMAIL_ENABLED = fromEnv("EMAIL_ENABLED", conf.getString("email.enabled")).toBoolean
      if (EMAIL_ENABLED) {
        EMAIL_FROM = fromEnv("EMAIL_FROM", conf.getString("email.from"))
        EMAIL_TO = fromEnv("EMAIL_TO", conf.getString("email.to")).split(",")
        EMAIL_SMTP_HOST = fromEnv("EMAIL_SMTP_HOST", conf.getString("email.smtp.host"))
        EMAIL_SMTP_PORT = fromEnv("EMAIL_SMTP_PORT", conf.getString("email.smtp.port")).toInt
        EMAIL_SMTP_AUTH_ENABLED = fromEnv("EMAIL_SMTP_AUTH_ENABLED", conf.getString("email.smtp.auth.enabled")).toBoolean
        EMAIL_SMTP_AUTH_USERNAME = fromEnv("EMAIL_SMTP_AUTH_USERNAME", conf.getString("email.smtp.auth.username"))
        EMAIL_SMTP_AUTH_PASSWORD = fromEnv("EMAIL_SMTP_AUTH_PASSWORD", conf.getString("email.smtp.auth.password"))
        // Collect as Properties object
        if (EMAIL_SMTP_AUTH_ENABLED) {
          SMTP_PROPERTIES.setProperty("mail.smtp.auth", "true")
        }
        SMTP_PROPERTIES.setProperty("mail.smtp.host", EMAIL_SMTP_HOST)
        SMTP_PROPERTIES.setProperty("mail.smtp.port", EMAIL_SMTP_PORT.toString)
      }

      SURVEY_ENABLED = fromEnv("SURVEY_ENABLED", conf.getString("survey.enabled")).toBoolean
      SURVEY_ADDRESS = fromEnv("SURVEY_ADDRESS", conf.getString("survey.address"))

      USERGUIDE_OU = fromEnv("USERGUIDE_OU", conf.getString("userguide.ou"))
      USERGUIDE_OA = fromEnv("USERGUIDE_OA", conf.getString("userguide.oa"))
      USERGUIDE_CA = fromEnv("USERGUIDE_CA", conf.getString("userguide.ca"))
      USERGUIDE_TA = fromEnv("USERGUIDE_TA", conf.getString("userguide.ta"))

      EMAIL_ATTACHMENTS_MAX_SIZE = fromEnv("EMAIL_ATTACHMENTS_MAX_SIZE", conf.getString("email.attachments.maxSize")).toInt
      EMAIL_ATTACHMENTS_MAX_COUNT = fromEnv("EMAIL_ATTACHMENTS_MAX_COUNT", conf.getString("email.attachments.maxCount")).toInt
      EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR = fromEnv("EMAIL_ATTACHMENTS_ALLOWED_TYPES", conf.getString("email.attachments.allowedTypes"))
      val tempSet = new scala.collection.mutable.HashSet[String]()
      EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR.split(",").map(_.trim).foreach{ mimeType =>
        tempSet += mimeType
      }
      EMAIL_ATTACHMENTS_ALLOWED_TYPES = tempSet.toSet

      ANTIVIRUS_SERVER_ENABLED = fromEnv("ANTIVIRUS_SERVER_ENABLED", conf.getString("antivirus.enabled")).toBoolean
      if (ANTIVIRUS_SERVER_ENABLED) {
        ANTIVIRUS_SERVER_HOST = fromEnv("ANTIVIRUS_SERVER_HOST", conf.getString("antivirus.host"))
        ANTIVIRUS_SERVER_PORT = fromEnv("ANTIVIRUS_SERVER_PORT", conf.getString("antivirus.port")).toInt
        ANTIVIRUS_SERVER_TIMEOUT = fromEnv("ANTIVIRUS_SERVER_TIMEOUT", conf.getString("antivirus.timeout")).toInt
      }

      MASTER_PASSWORD = fromEnv("MASTER_PASSWORD", conf.getString("masterPassword")).toCharArray

      PROXY_SERVER_ENABLED = fromEnv("PROXY_SERVER_ENABLED", conf.getString("proxy.enabled")).toBoolean
      if (PROXY_SERVER_ENABLED) {
        PROXY_SERVER_HOST = fromEnv("PROXY_SERVER_HOST", conf.getString("proxy.host")).toString
        PROXY_SERVER_PORT = fromEnv("PROXY_SERVER_PORT", conf.getString("proxy.port")).toInt
        PROXY_SERVER_AUTH_ENABLED = fromEnv("PROXY_SERVER_AUTH_ENABLED", conf.getString("proxy.auth.enabled")).toBoolean
        if (PROXY_SERVER_AUTH_ENABLED) {
          PROXY_SERVER_AUTH_USERNAME = fromEnv("PROXY_SERVER_AUTH_USERNAME", conf.getString("proxy.auth.user")).toString
          PROXY_SERVER_AUTH_PASSWORD = fromEnv("PROXY_SERVER_AUTH_PASSWORD", conf.getString("proxy.auth.password")).toString
        }
      }

      TSA_SERVER_ENABLED = fromEnv("TSA_SERVER_ENABLED", conf.getString("signature.tsa.enabled")).toBoolean
      if (TSA_SERVER_ENABLED) {
        TSA_SERVER_URL = fromEnv("TSA_SERVER_URL", conf.getString("signature.tsa.url")).toString
      }

      VALIDATION_TDL_EXTERNAL_ENABLED = fromEnv("VALIDATION_TDL_EXTERNAL_ENABLED", conf.getString("validation.tdl.external.enabled")).toBoolean
      if (VALIDATION_TDL_EXTERNAL_ENABLED) {
        VALIDATION_TDL_EXTERNAL_URL = fromEnv("VALIDATION_TDL_EXTERNAL_URL", conf.getString("validation.tdl.external.url")).toString
      }

      // Configure HMAC processing
      val hmacKey = System.getenv.getOrDefault("HMAC_KEY", "devKey")
      val hmacKeyWindow = System.getenv.getOrDefault("HMAC_WINDOW", "10000")
      HmacUtils.configure(hmacKey, hmacKeyWindow.toLong)

      AUTHENTICATION_COOKIE_PATH = fromEnv("AUTHENTICATION_COOKIE_PATH", conf.getString("authentication.cookie.path")).toString
      AUTHENTICATION_SSO_ENABLED = fromEnv("AUTHENTICATION_SSO_ENABLED", conf.getString("authentication.sso.enabled")).toBoolean
      AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = fromEnv("AUTHENTICATION_SSO_IN_MIGRATION_PERIOD", conf.getString("authentication.sso.inMigrationPeriod")).toBoolean
      AUTHENTICATION_SSO_LOGIN_URL = fromEnv("AUTHENTICATION_SSO_LOGIN_URL", conf.getString("authentication.sso.url.login")).toString
      AUTHENTICATION_SSO_CALLBACK_URL = fromEnv("AUTHENTICATION_SSO_CALLBACK_URL", conf.getString("authentication.sso.url.callback")).toString
      AUTHENTICATION_SSO_CAS_VERSION = fromEnv("AUTHENTICATION_SSO_CAS_VERSION", conf.getString("authentication.sso.casVersion")).toShort

      AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS = fromEnv("AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS", conf.getString("authentication.sso.customParameters.userDetails")).toString
      AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL", conf.getString("authentication.sso.userAttributes.email")).toString
      AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME", conf.getString("authentication.sso.userAttributes.firstName")).toString
      AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME", conf.getString("authentication.sso.userAttributes.lastName")).toString
      AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX = fromEnv("AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX", conf.getString("authentication.sso.ticketValidationUrlSuffix")).toString

      DEMOS_ENABLED = fromEnv("DEMOS_ENABLED", conf.getString("demos.enabled")).toBoolean
      DEMOS_ACCOUNT = fromEnv("DEMOS_ACCOUNT", conf.getString("demos.account")).toLong

      REGISTRATION_ENABLED = fromEnv("REGISTRATION_ENABLED", conf.getString("registration.enabled")).toBoolean
      TESTBED_HOME_LINK = fromEnv("TESTBED_HOME_LINK", TESTBED_HOME_LINK).toString

      SAVED_FILE_MAX_SIZE = fromEnv("SAVED_FILE_MAX_SIZE", SAVED_FILE_MAX_SIZE.toString).toLong

      GUIDES_EULOGIN_USE = fromEnv("GUIDES_EULOGIN_USE", conf.getString("guides.eulogin.use")).toString
      GUIDES_EULOGIN_MIGRATION = fromEnv("GUIDES_EULOGIN_MIGRATION", conf.getString("guides.eulogin.migration")).toString

      _IS_LOADED = true
    }
  }

  private def fromEnv(propertyName: String, default: String): String = {
    sys.env.getOrElse(propertyName, default)
  }

}
