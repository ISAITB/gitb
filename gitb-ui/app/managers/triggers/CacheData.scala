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
