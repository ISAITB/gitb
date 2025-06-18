/*
 * Copyright (C) 2025 European Union
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

import config.Configurations._
import com.redis._

object Redis {

  def getClient():RedisClient = {
    val client = new RedisClient(REDIS_HOST, REDIS_PORT)
    client
  }

  def releaseClient(client:RedisClient) = {
    client.disconnect
  }

  /**
   * Example implementation of Redis connection pooling
   */
  def lpush(msgs: List[String]) = {
    val clients = new RedisClientPool(REDIS_HOST, REDIS_PORT)
    clients.withClient {
      client => {
        msgs.foreach(client.lpush("list-l", _))
        client.llen("list-l")
      }
    }
  }

}
