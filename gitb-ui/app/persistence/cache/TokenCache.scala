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

  private val DEMO_SESSION_MARKER = "demo"

  /**
   * Record an access token. The stored value is "userId:creationTime" and, for sessions started through the demo
   * option of the welcome page, "userId:creationTime:demo". The marker is only ever set server-side.
   */
  def saveOAuthTokens(userId:Long, tokens:Token, demoSession: Boolean = false): Unit = {
    val accessTokenKey =  ACCESS_TOKEN_HASH_KEY  + HASH_SEPERATOR + tokens.accessToken
    val redisClient = Redis.getClient()
    try {
      val currentTime = System.currentTimeMillis()
      val value = if (demoSession) s"$userId:$currentTime:$DEMO_SESSION_MARKER" else s"$userId:$currentTime"
      redisClient.setex(accessTokenKey, Configurations.AUTHENTICATION_SESSION_MAX_IDLE_TIME, value)
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

  /**
   * Check whether the given access token is still valid without extending its idle expiry. This is meant for
   * background checks (e.g. server-side event channels) that are not the result of user activity.
   */
  def isAccessTokenValid(accessToken: String): Boolean = {
    val act_key = ACCESS_TOKEN_HASH_KEY + HASH_SEPERATOR + accessToken
    val redisClient = Redis.getClient()
    try {
      redisClient.get(act_key).exists { userData =>
        val userDataParts = userData.split(':')
        userDataParts.length > 1 && !(sessionMaxAgeMillis > 0 && (System.currentTimeMillis() - userDataParts(1).toLong > sessionMaxAgeMillis))
      }
    } finally {
      Redis.releaseClient(redisClient)
    }
  }

  def checkAccessToken(accessToken:String): Option[Long] = {
    checkAccessTokenInfo(accessToken).map(_._1)
  }

  /**
   * Check the given access token (extending its idle expiry) and return the user ID and whether the session was
   * started through the demo option of the welcome page.
   */
  def checkAccessTokenInfo(accessToken:String): Option[(Long, Boolean)] = {
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
            // Return user ID and demo session flag.
            Some((userDataParts(0).toLong, userDataParts.length > 2 && userDataParts(2) == DEMO_SESSION_MARKER))
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
