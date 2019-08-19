package persistence

import config.Configurations
import javax.inject.{Inject, Singleton}
import managers.BaseManager
import models.{Enums, Token, Users}
import org.apache.commons.lang3.RandomStringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.cache.TokenCache
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

@Singleton
class AuthenticationManager @Inject()(dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("AuthManager")

  def checkEmailAvailability(email:String, organisationId: Option[Long]): Boolean = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      if (organisationId.isDefined) {
        // We should not have a member user with the same email address within the organisation
        val q = PersistenceSchema.users
          .filter(_.ssoEmail === email)
          .filter(_.organization === organisationId.get)
          .filter(_.role === Enums.UserRole.VendorUser.id.toShort)
        exec(q.result.headOption).isEmpty
      } else {
        // TODO ECAS correct for test bed and community admins
        // The email is a username and needs to be unique across the entire test bed.
        val q = PersistenceSchema.users.filter(_.ssoEmail === email)
        exec(q.result.headOption).isEmpty
      }
    } else {
      // The email is a username and needs to be unique across the entire test bed.
      val q = PersistenceSchema.users.filter(_.email === email)
      exec(q.result.headOption).isEmpty
    }
  }

  def checkUserByEmail(email:String, passwd:String): Option[Users] = {
    val user = exec(PersistenceSchema.users.filter(_.email === email).result.headOption)
    if (user.isDefined && BCrypt.checkpw(passwd, user.get.password)) user else None
  }

  def generateTokens(userId:Long): Token = {
    //1) Create access and refresh tokens
    val access_token  = RandomStringUtils.randomAlphanumeric(Configurations.TOKEN_LENGTH)
    val tokens = Token(access_token)

    //2) Persist new access & refresh token information
    TokenCache.saveOAuthTokens(userId, tokens)
    tokens
  }

}
