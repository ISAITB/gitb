package persistence

import config.Configurations
import managers.BaseManager
import models.{Token, Users}
import org.apache.commons.lang.RandomStringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.cache.TokenCache
import persistence.db._

object AuthManager extends BaseManager {

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
    val refresh_token = RandomStringUtils.randomAlphanumeric(Configurations.TOKEN_LENGTH)
    val tokens = Token(access_token, refresh_token)

    //2) Persist new access & refresh token information
    TokenCache.saveOAuthTokens(userId, tokens)
    tokens
  }

  def refreshTokens(refreshToken:String): Token = {
    //1) Check if refresh token exists
    val userId = TokenCache.checkRefreshToken(refreshToken)

    //2) If so, generate new tokens
    val tokens = AuthManager.generateTokens(userId)

    //3) Delete the old ones and return the new
    TokenCache.deleteRefreshToken(refreshToken)
    tokens
  }

}
