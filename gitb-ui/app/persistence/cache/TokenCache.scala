package persistence.cache

import persistence.cache.Keys._
import models.Token
import config.Configurations
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import exceptions.{InvalidTokenException, ErrorCodes, InvalidRequestException}
import org.slf4j.LoggerFactory

object TokenCache {
  def logger = LoggerFactory.getLogger("TokenCache")

  val ACC_TOKEN_EXPIRE_IN_SECONDS = Configurations.TOKEN_LIFETIME_IN_SECONDS
  val REFRESH_TOKEN_EXPIRE_IN_SECONDS = Configurations.TOKEN_LIFETIME_IN_SECONDS * 30

  def saveOAuthTokens(userId:Long, tokens:Token):Future[Unit]={
    Future{
      val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + tokens.access_token
      val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + tokens.refresh_token

      val redisClient = Redis.getClient()
      redisClient.setex(act_key,  ACC_TOKEN_EXPIRE_IN_SECONDS,     userId)
      redisClient.setex(reft_key, REFRESH_TOKEN_EXPIRE_IN_SECONDS, userId)
      Redis.releaseClient(redisClient)
    }
  }

  def checkAccessToken(accessToken:String):Future[Long] = {
    Future{
      val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken

      val redisClient = Redis.getClient()
      val userId:Option[String] = redisClient.get(act_key)
      Redis.releaseClient(redisClient)

      if(!userId.isDefined){
        throw InvalidTokenException(ErrorCodes.INVALID_ACCESS_TOKEN, "Invalid access token")
      }
      userId.get.toLong
    }
  }

  def checkRefreshToken(refreshToken:String):Future[Long] = {
    Future{
      val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + refreshToken

      val redisClient = Redis.getClient()
      val userId:Option[String] = redisClient.get(reft_key)
      Redis.releaseClient(redisClient)

      if(!userId.isDefined){
        throw InvalidTokenException(ErrorCodes.INVALID_REFRESH_TOKEN, "Invalid refresh token")
      }
      userId.get.toLong
    }
  }

  def deleteRefreshToken(refreshToken:String):Future[Unit] = {
    Future{
      val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + refreshToken

      val redisClient = Redis.getClient()
      redisClient.del(reft_key)
      Redis.releaseClient(redisClient)
    }
  }
}
