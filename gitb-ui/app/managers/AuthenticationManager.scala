package persistence

import config.Configurations
import javax.inject.{Inject, Singleton}
import managers.BaseManager
import models.{Token, Users}
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

  def checkEmailAvailability(email:String): Boolean = {
    val q = PersistenceSchema.users.filter(_.email === email)
    !exec(q.result.headOption).isDefined
  }

  def checkUserByEmail(email:String, passwd:String): Option[Users] = {
    val query = exec(PersistenceSchema.users.filter(_.email === email).result.headOption)
    if (query.isDefined && BCrypt.checkpw(passwd, query.get.password)) query else None
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
