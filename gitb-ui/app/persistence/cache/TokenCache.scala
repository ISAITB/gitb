package persistence.cache

import config.Configurations
import exceptions.{ErrorCodes, InvalidTokenException}
import models.Token
import org.slf4j.LoggerFactory
import persistence.cache.Keys._

object TokenCache {
  def logger = LoggerFactory.getLogger("TokenCache")

  val sessionMaxAgeMillis = Configurations.AUTHENTICATION_SESSION_MAX_TOTAL_TIME * 1000

  def saveOAuthTokens(userId:Long, tokens:Token) = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + tokens.access_token
    val redisClient = Redis.getClient()
    val currentTime = System.currentTimeMillis()
    redisClient.setex(act_key,  Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME, userId+":"+currentTime)
    Redis.releaseClient(redisClient)
  }

  def deleteOAthToken(accessToken:String) = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken
    val redisClient = Redis.getClient()
    redisClient.del(act_key)
    Redis.releaseClient(redisClient)
  }

  def checkAccessToken(accessToken:String): Long = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken
    val redisClient = Redis.getClient()
    try {
      val userData:Option[String] = redisClient.get(act_key)
      if(userData.isEmpty){
        throw InvalidTokenException(ErrorCodes.INVALID_ACCESS_TOKEN, "Invalid access token")
      } else {
        val userDataParts = userData.get.split(':')
        if (userDataParts.length > 1) {
          val sessionCreationTimeStamp = userDataParts(1).toLong
          if (sessionMaxAgeMillis > 0 && (System.currentTimeMillis() - sessionCreationTimeStamp > sessionMaxAgeMillis)) {
            redisClient.del(act_key)
            throw InvalidTokenException(ErrorCodes.INVALID_ACCESS_TOKEN, "Expired access token")
          }
        }
        // Reset the token expiry.
        redisClient.setex(act_key, Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME, userData.get)
        // Return user ID.
        userDataParts(0).toLong
      }
    } finally {
      Redis.releaseClient(redisClient)
    }
  }

}
