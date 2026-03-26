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

package managers.ratelimit

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import config.Configurations
import io.github.bucket4j.{Bandwidth, Bucket}
import models.{Constants, RestApiLimits}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}
import utils.RepositoryUtils

import java.nio.file.Files
import java.time.Duration
import javax.inject.{Inject, Singleton}
import scala.util.Using

@Singleton
class RateLimitManager @Inject()(repositoryUtils: RepositoryUtils) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[RateLimitManager])

  private val globalCache = createCache(5_000)
  private val endpointCache = createCache(10_000)

  @volatile private var endpointLimits = Map.empty[String, Int]
  @volatile private var settings = RestApiLimits.defaultSettings()

  private def createCache(size: Int): Cache[String, Bucket] = {
    Caffeine.newBuilder()
      .maximumSize(size)
      .expireAfterAccess(Duration.ofDays(1))
      .build()
  }

  private def createBucket(limit: Int) = {
    val bandwidth = Bandwidth.builder()
      .capacity(limit)
      .refillGreedy(limit, Duration.ofMinutes(1))
      .build()
    Bucket.builder().addLimit(bandwidth).build()
  }

  def reset(newSettings: RestApiLimits): Unit = {
    // Invalidate current caches.
    globalCache.invalidateAll()
    endpointCache.invalidateAll()
    if (newSettings.enabled) {
      // Load the endpoints from the documentation.
      val documentationEndpoints = loadEndpointsFromDocumentation(Some(newSettings.defaultEndpointLimit))
      // Merge with the provided endpoints - ensure that provided endpoint entries are valid.
      val newEndpointLimits = newSettings.endpointLimits.map { e =>
        s"${e.path}|${e.method.toUpperCase}" -> e.limit
      }.toMap
      val mergedEndpointLimits = documentationEndpoints ++ newEndpointLimits.filter {
        case (key, _) => documentationEndpoints.contains(key)
      }
      // Apply new configuration.
      settings = newSettings
      endpointLimits = mergedEndpointLimits
    }
    logger.info("Rate limit configuration updated - limiting is {}", if (newSettings.enabled) "enabled" else "disabled")
  }

  def tryConsume(apiKey: String, path: String, method: String): Option[Long] = {
    if (Configurations.AUTOMATION_API_ENABLED && settings.enabled) {
      val globalProbe = globalCache.get(apiKey, _ => createBucket(settings.globalLimit)).tryConsumeAndReturnRemaining(1)
      if (globalProbe.isConsumed) {
        val endpointKey = s"$path|${method.toUpperCase}"
        val endpointProbe = endpointCache.get(s"$apiKey|$endpointKey", _ => createBucket(endpointLimits.getOrElse(endpointKey, settings.defaultEndpointLimit))).tryConsumeAndReturnRemaining(1)
        if (endpointProbe.isConsumed) {
          None
        } else {
          Some(endpointProbe.getNanosToWaitForRefill / 1_000_000_000L)
        }
      } else {
        Some(globalProbe.getNanosToWaitForRefill / 1_000_000_000L)
      }
    } else {
      None
    }
  }

  def currentSettings(): RestApiLimits = {
    settings
  }

  private def loadEndpointsFromDocumentation(defaultLimit: Option[Int] = None): Map[String, Int] = {
    Using(Files.newInputStream(repositoryUtils.getRestApiDocsDocumentation().toPath)) { stream =>
      val json = Json.parse(stream)
      val paths = (json \ "paths").as[JsObject]
      paths.fields.flatMap { case (path, pathItem) =>
        pathItem.as[JsObject].fields
          .filter { case (method, _) => Constants.HttpMethods.contains(method.toLowerCase) }
          .map    { case (method, _) => s"$path|${method.toUpperCase}" -> defaultLimit.getOrElse(settings.defaultEndpointLimit) }
      }.toMap
    }.get
  }

}
