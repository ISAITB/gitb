package persistence

import managers.BaseManager
import org.mindrot.jbcrypt.BCrypt
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import persistence.db._
import models.{Users, Token}
import org.apache.commons.lang.RandomStringUtils
import persistence.cache.TokenCache
import org.slf4j.LoggerFactory
import config.Configurations

object AuthManager extends BaseManager {
  def logger = LoggerFactory.getLogger("AuthManager")

  def checkEmailAvailability(email:String):Future[Boolean] = {
    Future{
      DB.withSession { implicit session:Session =>
        val q = PersistenceSchema.users.filter(_.email === email)
        !q.firstOption.isDefined
      }
    }
  }

  def checkUserByEmail(email:String, passwd:String):Future[Option[Users]] = {
    Future{
      DB.withSession { implicit session:Session =>
        val query = PersistenceSchema.users.filter(_.email === email).firstOption
        if (query.isDefined && BCrypt.checkpw(passwd, query.get.password)) query else None
      }
    }
  }

  def generateTokens(userId:Long):Future[Token] = {
    //1) Create access and refresh tokens
    val access_token  = RandomStringUtils.randomAlphanumeric(Configurations.TOKEN_LENGTH)
    val refresh_token = RandomStringUtils.randomAlphanumeric(Configurations.TOKEN_LENGTH)
    val tokens = Token(access_token, refresh_token)

    //2) Persist new access & refresh token information
    TokenCache.saveOAuthTokens(userId, tokens) map { unit => //return tokens when they are saved
      tokens
    }
  }

  def refreshTokens(refreshToken:String):Future[Token] = {
    //1) Check if refresh token exists
    TokenCache.checkRefreshToken(refreshToken) flatMap { userId =>

      //2) If so, generate new tokens
      AuthManager.generateTokens(userId) map { tokens =>

        //3) Delete the old ones and return the new
        TokenCache.deleteRefreshToken(refreshToken)
        tokens
      }
    }
  }

}
