package persistence.cache

import config.Configurations
import exceptions.{ErrorCodes, InvalidTokenException}
import models.Token
import org.slf4j.LoggerFactory
import persistence.cache.Keys._

object TokenCache {
  def logger = LoggerFactory.getLogger("TokenCache")

  val ACC_TOKEN_EXPIRE_IN_SECONDS = Configurations.TOKEN_LIFETIME_IN_SECONDS
  val REFRESH_TOKEN_EXPIRE_IN_SECONDS = Configurations.TOKEN_LIFETIME_IN_SECONDS * 30

  def saveOAuthTokens(userId:Long, tokens:Token) = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + tokens.access_token
    val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + tokens.refresh_token

    val redisClient = Redis.getClient()
    redisClient.setex(act_key,  ACC_TOKEN_EXPIRE_IN_SECONDS,     userId)
    redisClient.setex(reft_key, REFRESH_TOKEN_EXPIRE_IN_SECONDS, userId)
    Redis.releaseClient(redisClient)
  }

  def checkAccessToken(accessToken:String): Long = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken

    val redisClient = Redis.getClient()
    val userId:Option[String] = redisClient.get(act_key)
    Redis.releaseClient(redisClient)

    if(!userId.isDefined){
      throw InvalidTokenException(ErrorCodes.INVALID_ACCESS_TOKEN, "Invalid access token")
    }
    userId.get.toLong
  }

  def checkRefreshToken(refreshToken:String): Long = {
    val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + refreshToken

    val redisClient = Redis.getClient()
    val userId:Option[String] = redisClient.get(reft_key)
    Redis.releaseClient(redisClient)

    if(!userId.isDefined){
      throw InvalidTokenException(ErrorCodes.INVALID_REFRESH_TOKEN, "Invalid refresh token")
    }
    userId.get.toLong
  }

  def deleteRefreshToken(refreshToken:String) = {
    val reft_key = REFRESH_TOKEN_HASH_KEY + HASH_SEPERATOR + refreshToken

    val redisClient = Redis.getClient()
    redisClient.del(reft_key)
    Redis.releaseClient(redisClient)
  }
}
