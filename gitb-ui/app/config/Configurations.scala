package config

import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Play

object Configurations {

  // Database parameters
  var DB_DRIVER_CLASS:String = ""
  var DB_JDBC_URL:String = ""
  var DB_USER:String = ""
  var DB_PASSWORD = ""

  var DB_ROOT_URL:String = ""
  var DB_NAME:String = ""
/*
  // c3p0 connection pooling settings
  var C3P0_ACQUIRE_INCREMENT:Int = 0
  var C3P0_CONNECTION_TIMEOUT = 0
  var C3P0_EXCESS_TIMEOUT = 0
  var C3P0_MAX_POOLSIZE = 0
  var C3P0_MIN_POOLSIZE = 0
  var C3P0_UNRETURNED_TIMEOUT = 0
  var C3P0_HELPER_THREADS = 0
  */

  // Redis parameters
  var REDIS_HOST:String = ""
  var REDIS_PORT = 0

  //General configurations
  var SERVER_REQUEST_TIMEOUT_IN_SECONDS=0
  var TOKEN_LIFETIME_IN_DAYS = 0
  var TOKEN_LIFETIME_IN_SECONDS = 0
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

  var SMTP_PROPERTIES = new Properties()

  def loadConfigurations() = {
    //Load configuration file
    val conf:Config = ConfigFactory.load()

    //Parse DB Parameters
    DB_DRIVER_CLASS = conf.getString("db.default.driver")
    DB_JDBC_URL     = conf.getString("db.default.url")
    DB_USER         = conf.getString("db.default.user")
    DB_PASSWORD     = conf.getString("db.default.password")
    DB_ROOT_URL     = conf.getString("db.default.rooturl")
    DB_NAME         = conf.getString("db.default.name")
/*
    //Parse C3P0 Parameters
    C3P0_ACQUIRE_INCREMENT  = conf.getInt("c3p0.acquire_increment")
    C3P0_CONNECTION_TIMEOUT = conf.getInt("c3p0.connection_timeout")
    C3P0_EXCESS_TIMEOUT     = conf.getInt("c3p0.excess_timeout")
    C3P0_MAX_POOLSIZE       = conf.getInt("c3p0.max_poolsize")
    C3P0_MIN_POOLSIZE       = conf.getInt("c3p0.min_poolsize")
    C3P0_UNRETURNED_TIMEOUT = conf.getInt("c3p0.unreturned_timeout")
    C3P0_HELPER_THREADS     = conf.getInt("c3p0.helper_threads")
*/
    //Parse Redis Parameters
    REDIS_HOST = conf.getString("redis.host")
    REDIS_PORT = conf.getInt("redis.port")

    //General Parameters
    TOKEN_LIFETIME_IN_DAYS = conf.getInt("token.lifetime.days")
    TOKEN_LIFETIME_IN_SECONDS = TOKEN_LIFETIME_IN_DAYS * 24 * 60 * 60
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
  }

  def fromEnv(propertyName: String, default: String): String = {
    sys.env.getOrElse(propertyName, default)
  }

}
