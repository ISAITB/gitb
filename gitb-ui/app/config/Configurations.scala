package config

import com.gitb.utils.HmacUtils
import com.typesafe.config.{Config, ConfigFactory}
import models.Constants

import java.util.Properties
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.matching.Regex

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
  var EMAIL_TO: Array[String] = _
  var EMAIL_SMTP_HOST = ""
  var EMAIL_SMTP_PORT: Int = -1
  var EMAIL_SMTP_SSL_ENABLED = false
  var EMAIL_SMTP_SSL_PROTOCOLS: Option[String] = None
  var EMAIL_SMTP_STARTTLS_ENABLED = false
  var EMAIL_SMTP_AUTH_ENABLED = true
  var EMAIL_SMTP_AUTH_USERNAME = ""
  var EMAIL_SMTP_AUTH_PASSWORD = ""
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

  var EMAIL_ATTACHMENTS_MAX_SIZE: Int = -1
  var EMAIL_ATTACHMENTS_MAX_COUNT: Int = -1
  var EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR = ""
  var EMAIL_ATTACHMENTS_ALLOWED_TYPES: Set[String] = _

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

  // 5120 KB default (5 MBs)
  var SAVED_FILE_MAX_SIZE: Long = 5120

  var SMTP_PROPERTIES = new Properties()

  var INPUT_SANITIZER__ENABLED = true
  var INPUT_SANITIZER__METHODS_TO_CHECK_STR:String = _
  var INPUT_SANITIZER__METHODS_TO_CHECK:Set[String] = _
  var INPUT_SANITIZER__DEFAULT_BLACKLIST_EXPRESSION:Regex = _
  var INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS:Map[String, Regex] = _
  var INPUT_SANITIZER__PARAMETERS_TO_SKIP:Set[String] = _
  var INPUT_SANITIZER__PARAMETERS_AS_JSON:Set[String] = _

  var DATA_ARCHIVE_KEY = ""
  var DATA_WEB_INIT_ENABLED = false

  var TEST_SESSION_ARCHIVE_THRESHOLD = 30
  var PASSWORD_COMPLEXITY_RULE_REGEX:Regex = _

  var TESTBED_MODE:String = ""

  var API_ROOT = ""

  def loadConfigurations(): Unit = {
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
      SERVER_REQUEST_TIMEOUT_IN_SECONDS = fromEnv("SERVER_REQUEST_TIMEOUT_IN_SECONDS", conf.getString("server.request.timeout.seconds")).toInt
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
        EMAIL_SMTP_SSL_ENABLED = fromEnv("EMAIL_SMTP_SSL_ENABLED", conf.getString("email.smtp.ssl.enabled")).toBoolean
        if (EMAIL_SMTP_SSL_ENABLED) {
          SMTP_PROPERTIES.setProperty("mail.smtp.ssl.enable", "true")
          val sslProtocolsValue = fromEnv("EMAIL_SMTP_SSL_PROTOCOLS", conf.getString("email.smtp.ssl.protocols"))
          if (!sslProtocolsValue.isBlank) {
            EMAIL_SMTP_SSL_PROTOCOLS = Some(sslProtocolsValue.trim)
            SMTP_PROPERTIES.setProperty("mail.smtp.ssl.protocols", EMAIL_SMTP_SSL_PROTOCOLS.get)
          }
        }
        EMAIL_SMTP_STARTTLS_ENABLED = fromEnv("EMAIL_SMTP_STARTTLS_ENABLED", conf.getString("email.smtp.starttls.enabled")).toBoolean
        if (EMAIL_SMTP_STARTTLS_ENABLED) {
          SMTP_PROPERTIES.setProperty("mail.smtp.starttls.enable", "true")
        }
        SMTP_PROPERTIES.setProperty("mail.smtp.host", EMAIL_SMTP_HOST)
        SMTP_PROPERTIES.setProperty("mail.smtp.port", EMAIL_SMTP_PORT.toString)
      }

      SURVEY_ENABLED = fromEnv("SURVEY_ENABLED", conf.getString("survey.enabled")).toBoolean
      SURVEY_ADDRESS = fromEnv("SURVEY_ADDRESS", conf.getString("survey.address"))
      MORE_INFO_ENABLED = fromEnv("MORE_INFO_ENABLED", conf.getString("moreinfo.enabled")).toBoolean
      MORE_INFO_ADDRESS = fromEnv("MORE_INFO_ADDRESS", conf.getString("moreinfo.address"))
      RELEASE_INFO_ENABLED = fromEnv("RELEASE_INFO_ENABLED", conf.getString("releaseinfo.enabled")).toBoolean
      RELEASE_INFO_ADDRESS = fromEnv("RELEASE_INFO_ADDRESS", conf.getString("releaseinfo.address"))

      USERGUIDE_OU = fromEnv("USERGUIDE_OU", conf.getString("userguide.ou"))
      USERGUIDE_OA = fromEnv("USERGUIDE_OA", conf.getString("userguide.oa"))
      USERGUIDE_CA = fromEnv("USERGUIDE_CA", conf.getString("userguide.ca"))
      USERGUIDE_TA = fromEnv("USERGUIDE_TA", conf.getString("userguide.ta"))

      EMAIL_ATTACHMENTS_MAX_SIZE = fromEnv("EMAIL_ATTACHMENTS_MAX_SIZE", conf.getString("email.attachments.maxSize")).toInt
      EMAIL_ATTACHMENTS_MAX_COUNT = fromEnv("EMAIL_ATTACHMENTS_MAX_COUNT", conf.getString("email.attachments.maxCount")).toInt
      EMAIL_ATTACHMENTS_ALLOWED_TYPES_STR = fromEnv("EMAIL_ATTACHMENTS_ALLOWED_TYPES", conf.getString("email.attachments.allowedTypes"))
      var tempSet = new scala.collection.mutable.HashSet[String]()
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
      val hmacKey = System.getenv.getOrDefault("HMAC_KEY", "devKey")
      val hmacKeyWindow = System.getenv.getOrDefault("HMAC_WINDOW", "10000")
      HmacUtils.configure(hmacKey, hmacKeyWindow.toLong)

      AUTHENTICATION_COOKIE_PATH = fromEnv("AUTHENTICATION_COOKIE_PATH", conf.getString("authentication.cookie.path"))
      AUTHENTICATION_SSO_ENABLED = fromEnv("AUTHENTICATION_SSO_ENABLED", conf.getString("authentication.sso.enabled")).toBoolean
      AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = fromEnv("AUTHENTICATION_SSO_IN_MIGRATION_PERIOD", conf.getString("authentication.sso.inMigrationPeriod")).toBoolean
      AUTHENTICATION_SSO_LOGIN_URL = fromEnv("AUTHENTICATION_SSO_LOGIN_URL", conf.getString("authentication.sso.url.login"))
      AUTHENTICATION_SSO_CALLBACK_URL = fromEnv("AUTHENTICATION_SSO_CALLBACK_URL", conf.getString("authentication.sso.url.callback"))
      AUTHENTICATION_SSO_CAS_VERSION = fromEnv("AUTHENTICATION_SSO_CAS_VERSION", conf.getString("authentication.sso.casVersion")).toShort

      AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS = fromEnv("AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS", conf.getString("authentication.sso.customParameters.userDetails"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL", conf.getString("authentication.sso.userAttributes.email"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME", conf.getString("authentication.sso.userAttributes.firstName"))
      AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME = fromEnv("AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME", conf.getString("authentication.sso.userAttributes.lastName"))
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
      INPUT_SANITIZER__METHODS_TO_CHECK_STR = fromEnv("INPUT_SANITIZER__METHODS_TO_CHECK", conf.getString("inputSanitizer.methodsToCheck"))
      tempSet = new scala.collection.mutable.HashSet[String]()
      INPUT_SANITIZER__METHODS_TO_CHECK_STR.split(",").map(_.trim).foreach{ method =>
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

      DATA_ARCHIVE_KEY = fromEnv("DATA_ARCHIVE_KEY", "")
      DATA_WEB_INIT_ENABLED = fromEnv("DATA_WEB_INIT_ENABLED", "false").toBoolean

      // Mode - START
      /*
        Use of default values for secrets should only be allowed for a development instance.
       */
      if (DATA_ARCHIVE_KEY.nonEmpty || DATA_WEB_INIT_ENABLED) {
        TESTBED_MODE = Constants.SandboxMode
      } else {
        TESTBED_MODE = fromEnv("TESTBED_MODE", Constants.DevelopmentMode)
        // Test that no default values are being used.
        if (TESTBED_MODE == Constants.ProductionMode) {
          val appSecret = fromEnv("APPLICATION_SECRET", "value_used_during_development_to_be_replaced_in_production")
          val dbPassword = fromEnv("DB_DEFAULT_PASSWORD", "gitb")
          val masterPassword = String.valueOf(MASTER_PASSWORD)
          if (appSecret == "value_used_during_development_to_be_replaced_in_production" || appSecret == "CHANGE_ME" ||
            masterPassword == "value_used_during_development_to_be_replaced_in_production" || masterPassword == "CHANGE_ME" ||
            dbPassword == "gitb" || dbPassword == "CHANGE_ME" ||
            hmacKey == "devKey" || hmacKey == "CHANGE_ME"
          ) {
            throw new IllegalStateException("Your application is running in production mode with default values set for sensitive configuration properties. Switch to development mode by setting on gitb-ui the TESTBED_MODE environment variable to \""+Constants.DevelopmentMode+"\" or replace these settings accordingly. For more details refer to the test bed's production installation guide.")
          }
        }
      }
      // Mode - END
      TEST_SESSION_ARCHIVE_THRESHOLD = fromEnv("TEST_SESSION_ARCHIVE_THRESHOLD", conf.getString("testsession.archive.threshold")).toInt
      API_ROOT = conf.getString("apiPrefix")
      PASSWORD_COMPLEXITY_RULE_REGEX = new Regex(conf.getString("passwordComplexityRule"))
      _IS_LOADED = true
    }
  }

  private def fromEnv(propertyName: String, default: String): String = {
    sys.env.getOrElse(propertyName, default)
  }

}
