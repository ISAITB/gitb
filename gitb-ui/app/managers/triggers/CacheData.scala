package managers.triggers

import scala.concurrent.{ExecutionContext, Future}

object CacheCata {

  def fromCache[T](cache: Map[String, Any], cacheKey: String, fnLoad: () => Future[T])(implicit ec: ExecutionContext): Future[CacheData[T]] = {
    if (cache.contains(cacheKey)) {
      Future.successful {
        CacheData(cache, cache(cacheKey).asInstanceOf[T])
      }
    } else {
      fnLoad.apply().map { data =>
        val newCache = cache + (cacheKey -> data)
        CacheData(newCache, data)
      }
    }
  }

  def fromCacheWithCache[T](cache: Map[String, Any], cacheKey: String, fnLoad: () => Future[CacheData[T]]): Future[CacheData[T]] = {
    if (cache.contains(cacheKey)) {
      Future.successful {
        CacheData(cache, cache(cacheKey).asInstanceOf[T])
      }
    } else {
      fnLoad.apply()
    }
  }

}

case class CacheData[T](cache: Map[String, Any], data: T)
