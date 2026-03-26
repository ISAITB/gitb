/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package persistence.cache

import config.Configurations
import models.Token
import persistence.cache.Keys._

object TokenCache {

  private val sessionMaxAgeMillis = Configurations.AUTHENTICATION_SESSION_MAX_TOTAL_TIME * 1000

  def saveOAuthTokens(userId:Long, tokens:Token): Unit = {
    val accessTokenKey =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + tokens.accessToken
    val redisClient = Redis.getClient()
    try {
      val currentTime = System.currentTimeMillis()
      redisClient.setex(accessTokenKey, Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME, s"$userId:$currentTime")
    } finally {
      Redis.releaseClient(redisClient)
    }
  }

  def deleteOAthToken(accessToken:String): Unit = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken
    val redisClient = Redis.getClient()
    try {
      redisClient.del(act_key)
    } finally {
      Redis.releaseClient(redisClient)
    }
  }

  def checkAccessToken(accessToken:String): Option[Long] = {
    val act_key =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + accessToken
    val redisClient = Redis.getClient()
    try {
      val userData: Option[String] = redisClient.get(act_key)
      if (userData.isEmpty) {
        None
      } else {
        val userDataParts = userData.get.split(':')
        if (userDataParts.length > 1) {
          val sessionCreationTimeStamp = userDataParts(1).toLong
          if (sessionMaxAgeMillis > 0 && (System.currentTimeMillis() - sessionCreationTimeStamp > sessionMaxAgeMillis)) {
            redisClient.del(act_key)
            None
          } else {
            // Reset the token expiry.
            redisClient.expire(act_key, Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME)
            // Return user ID.
            Some(userDataParts(0).toLong)
          }
        } else {
          None
        }
      }
    } finally {
      Redis.releaseClient(redisClient)
    }
  }

}
