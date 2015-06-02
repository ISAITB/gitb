package persistence.cache

object Keys {
  val HASH_SEPERATOR = ":"

  //REDIS HASH extensions for OAUTH tokens
  val ACCESS_TOKEN_HASH_KEY =  "auth"
  val REFRESH_TOKEN_HASH_KEY = "rauth"


}
