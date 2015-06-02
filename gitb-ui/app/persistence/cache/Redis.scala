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
