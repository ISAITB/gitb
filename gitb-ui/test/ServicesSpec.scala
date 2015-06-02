import config.Configurations
import exceptions.ErrorCodes
import java.util.concurrent.TimeUnit
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import persistence.cache.Redis
import persistence.db.PersistenceLayer
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration.FiniteDuration

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ServicesSpec extends Specification{

  val testDbName:String    = "gitb_test"
  val testConfig = Map(
    "db.default.driver" -> "com.mysql.jdbc.Driver",
    "db.default.url" -> ("jdbc:mysql://localhost:3306/"+testDbName+"?characterEncoding=UTF-8&useUnicode=true"),
    "db.default.user" -> Configurations.DB_USER,
    "db.default.password" -> Configurations.DB_PASSWORD)

  var vendorSname       = "srdc"
  var vendorFname       = "SRDC"
  var adminName         = "Mr.Admin"
  var adminEmail        = "admin@srdc.com.tr"
  var adminPassword     = "admin"
  var adminAccessToken  = ""
  var adminRefreshToken = ""

  var user1Name         = "Mr.User1"
  var user1Email        = "user1@srdc.com.tr"
  var user1Password     = "user1"
  var user1AccessToken  = ""
  var user1RefreshToken = ""

  var user2Name         = "Mr.User2"
  var user2Email        = "user2@srdc.com.tr"
  var user2Password     = "user2"
  var user2AccessToken  = ""
  var user2RefreshToken = ""

  "Services" should {

    "create a new database for testing" in new WithApplication{
      PersistenceLayer.dropDatabaseIfExists(testDbName)
      PersistenceLayer.createDatabaseIfNotExists(testDbName)

      //select an unused db for testing
      val redisClient = Redis.getClient()
      redisClient.select(1)
      Redis.releaseClient(redisClient)
    }

    "create a new vendor with an admin" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {

        //send a request with a missing parameter
        val check = route(FakeRequest(GET, "/check/email")).get
        status(check) must equalTo(BAD_REQUEST)
        (contentAsJson(check) \ "error_code").as[Int] must equalTo(ErrorCodes.MISSING_PARAMS)

        //check adminEmail before registering the vendor and admin
        val check1 = route(FakeRequest(GET, ("/check/email?email=" + adminEmail))).get
        status(check1) must equalTo(OK)
        (contentAsJson(check1) \ "available").as[Boolean] must equalTo(true)

        //register the vendor and admin
        val register = route(FakeRequest(POST, "/vendor/register").
          withFormUrlEncodedBody(
            "vendor_sname" -> vendorSname ,
            "vendor_fname" -> vendorFname,
            "user_name"    -> adminName,
            "user_email"   -> adminEmail,
            "password"     -> adminPassword
          )).get
        status(register) must equalTo(OK)

        //check adminEmail again
        val check2 = route(FakeRequest(GET, ("/check/email?email=" + adminEmail))).get
        status(check2) must equalTo(OK)
        (contentAsJson(check2) \ "available").as[Boolean] must equalTo(false)
      }
    }

    "reject access to user with wrong credentials or bad request" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        //login with missing grant_type
        val loginAttempt1 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "email"    -> adminEmail,
            "password" -> adminPassword
          )).get
        status(loginAttempt1) must equalTo(BAD_REQUEST)
        (contentAsJson(loginAttempt1) \ "error_code").as[Int] must equalTo(ErrorCodes.MISSING_PARAMS)

        //login with missing email
        val loginAttempt2 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "password"   -> adminPassword
          )).get
        status(loginAttempt2) must equalTo(BAD_REQUEST)
        (contentAsJson(loginAttempt2) \ "error_code").as[Int] must equalTo(ErrorCodes.MISSING_PARAMS)

        //login with missing password
        val loginAttempt3 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> adminEmail
          )).get
        status(loginAttempt3) must equalTo(BAD_REQUEST)
        (contentAsJson(loginAttempt3) \ "error_code").as[Int] must equalTo(ErrorCodes.MISSING_PARAMS)

        //login with wrong grant type
        val loginAttempt4 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "something",
            "email"      -> adminEmail,
            "password"   -> adminPassword
          )).get
        status(loginAttempt4) must equalTo(BAD_REQUEST)
        (contentAsJson(loginAttempt4) \ "error_code").as[Int] must equalTo(ErrorCodes.INVALID_REQUEST)

        //login with wrong password
        val loginAttempt5 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> adminEmail,
            "password"   -> "wrongPassword"
          )).get
        status(loginAttempt5) must equalTo(UNAUTHORIZED)
        (contentAsJson(loginAttempt5) \ "error_code").as[Int] must equalTo(ErrorCodes.INVALID_CREDENTIALS)
      }
    }

    "allow access to user with his credentials" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        val login = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> adminEmail,
            "password"   -> adminPassword
          )).get
        status(login) must equalTo(OK)
        adminAccessToken  = (contentAsJson(login) \ "access_token").as[String]
        adminRefreshToken = (contentAsJson(login) \ "refresh_token").as[String]
        (contentAsJson(login) \ "access_token").as[String]  must not equalTo("")
        (contentAsJson(login) \ "refresh_token").as[String] must not equalTo("")
        (contentAsJson(login) \ "expires_in").as[Int] must equalTo(Configurations.TOKEN_LIFETIME_IN_SECONDS)
        (contentAsJson(login) \ "token_type").as[String] must equalTo("Bearer")
      }
    }

    "refresh oauth tokens" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        //try to refresh with missing refresh_token parameter
        val refresh1 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "refresh_token"
          )).get
        status(refresh1) must equalTo(BAD_REQUEST)
        (contentAsJson(refresh1) \ "error_code").as[Int] must equalTo(ErrorCodes.MISSING_PARAMS)

        //refresh with valid parameters
        val refresh2 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "refresh_token",
            "refresh_token" -> adminRefreshToken
          )).get
        status(refresh2) must equalTo(OK)
        adminAccessToken  = (contentAsJson(refresh2) \ "access_token").as[String]
        adminRefreshToken = (contentAsJson(refresh2) \ "refresh_token").as[String]
        (contentAsJson(refresh2) \ "access_token").as[String]  must not equalTo("")
        (contentAsJson(refresh2) \ "refresh_token").as[String] must not equalTo("")
        (contentAsJson(refresh2) \ "expires_in").as[Int] must equalTo(Configurations.TOKEN_LIFETIME_IN_SECONDS)
        (contentAsJson(refresh2) \ "token_type").as[String] must equalTo("Bearer")
      }
    }

    "return user profile" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        val profile = route(FakeRequest(GET, "/user/profile")
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile) must equalTo(OK)
        (contentAsJson(profile) \ "name").as[String] must equalTo(adminName)
        (contentAsJson(profile) \ "email").as[String] must equalTo(adminEmail)
      }
    }

    "update user profile" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        //update name
        val newName = "Mr. Admin"
        val profile1 = route(FakeRequest(POST, "/user/profile")
          .withFormUrlEncodedBody(
            "user_name" -> newName
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile1) must equalTo(OK)
        adminName = newName

        //update password
        val newPassword = "newpassword"
        val profile2 = route(FakeRequest(POST, "/user/profile")
          .withFormUrlEncodedBody(
            "password" -> newPassword,
            "old_password" -> adminPassword
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile2) must equalTo(OK)
        adminPassword = newPassword

        //check updated name
        val profile = route(FakeRequest(GET, "/user/profile")
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile) must equalTo(OK)
        (contentAsJson(profile) \ "name").as[String] must equalTo(adminName)

        //login with new password
        val login = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> adminEmail,
            "password"   -> adminPassword
          )).get
        status(login) must equalTo(OK)
        adminAccessToken = (contentAsJson(login) \ "access_token").as[String]
        (contentAsJson(login) \ "access_token").as[String]  must not equalTo("")
      }
    }

    "return vendor profile" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        val profile = route(FakeRequest(GET, "/vendor/profile")
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile) must equalTo(OK)
        (contentAsJson(profile) \ "sname").as[String] must equalTo(vendorSname)
        (contentAsJson(profile) \ "fname").as[String] must equalTo(vendorFname)
      }
    }

    "update vendor profile" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        val profile1 = route(FakeRequest(POST, "/vendor/profile")
          .withFormUrlEncodedBody(/*empty body*/)
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(profile1) must equalTo(OK)

        val newFname = "SRDC Yazılım Araştırma ve Gel. ve Dan. Tic. Ltd. Şti."
        val profile2 = route(FakeRequest(POST, "/vendor/profile")
          .withFormUrlEncodedBody(
            "vendor_fname" -> newFname
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        vendorFname = newFname
        status(profile2) must equalTo(OK)
      }
    }

    "register new users" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        //get user count
        val users1 = route(FakeRequest(GET, "/vendor/users")
        .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(users1) must equalTo(OK)
        contentAsJson(users1).as[List[Map[String, Option[String]]]].length must equalTo(1)

        //register user1
        val register1 = route(FakeRequest(POST, "/user/register")
        .withFormUrlEncodedBody(
          "user_name"  -> user1Name,
          "user_email" -> user1Email,
          "password"   -> user1Password
        )
        .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(register1) must equalTo(OK)

        //get user count
        val users2 = route(FakeRequest(GET, "/vendor/users")
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(users2) must equalTo(OK)
        contentAsJson(users2).as[List[Map[String, Option[String]]]].length must equalTo(2)

        //register user2
        val register2 = route(FakeRequest(POST, "/user/register")
          .withFormUrlEncodedBody(
            "user_name"  -> user2Name,
            "user_email" -> user2Email,
            "password"   -> user2Password
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(register2) must equalTo(OK)

        //get user count
        val users3 = route(FakeRequest(GET, "/vendor/users")
          .withHeaders(AUTHORIZATION -> ("Bearer " + adminAccessToken))).get
        status(users3) must equalTo(OK)
        contentAsJson(users3).as[List[Map[String, Option[String]]]].length must equalTo(3)

        //new users should be able to login with their credentials
        val login1 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> user1Email,
            "password"   -> user1Password
          )).get
        user1AccessToken  = (contentAsJson(login1) \ "access_token").as[String]
        user1RefreshToken = (contentAsJson(login1) \ "refresh_token").as[String]
        status(login1) must equalTo(OK)

        val login2 = route(FakeRequest(POST, "/oauth/access_token")
          .withFormUrlEncodedBody(
            "grant_type" -> "password",
            "email"      -> user2Email,
            "password"   -> user2Password
          )).get
        user2AccessToken  = (contentAsJson(login2) \ "access_token").as[String]
        user2RefreshToken = (contentAsJson(login2) \ "refresh_token").as[String]
        status(login2) must equalTo(OK)

        //new users should be able to update their profiles
        val newName = "Mr. User1"
        val profile1 = route(FakeRequest(POST, "/user/profile")
          .withFormUrlEncodedBody(
            "user_name" -> newName
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + user1AccessToken))).get
        user1Name = newName
        status(profile1) must equalTo(OK)

      }
    }

    "reject users to access admin services" in {
      running(FakeApplication(additionalConfiguration = testConfig)) {
        val register = route(FakeRequest(POST, "/user/register")
        .withFormUrlEncodedBody(
          "user_name"  -> "hypothetical",
          "user_email" -> "email@srdc.com.tr",
          "password"   -> "passwd"
        )
        .withHeaders(AUTHORIZATION -> ("Bearer " + user1AccessToken))).get
        status(register) must equalTo(UNAUTHORIZED)

        val profile = route(FakeRequest(POST, "/vendor/profile")
          .withFormUrlEncodedBody(
            "vendor_fname" -> "hacked"
          )
          .withHeaders(AUTHORIZATION -> ("Bearer " + user1AccessToken))).get
        status(profile) must equalTo(UNAUTHORIZED)

        val sutRegister = route(FakeRequest(POST, "/suts/register")
          .withHeaders(AUTHORIZATION -> ("Bearer " + user1AccessToken))).get
        status(sutRegister) must equalTo(UNAUTHORIZED)

        val sutProfile = route(FakeRequest(POST, "/suts/1/profile")
          .withHeaders(AUTHORIZATION -> ("Bearer " + user1AccessToken))).get
        status(sutProfile) must equalTo(UNAUTHORIZED)
      }
    }

    "drop test database" in new WithApplication{
      PersistenceLayer.dropDatabaseIfExists(testDbName)

      val redisClient = Redis.getClient()
      redisClient.flushdb
      Redis.releaseClient(redisClient)
    }

  }

}
