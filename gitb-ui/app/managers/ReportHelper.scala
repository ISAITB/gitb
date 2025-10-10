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

package managers

import com.gitb.reports.ReportSpecs
import org.apache.commons.lang3.Strings
import org.slf4j.{Logger, LoggerFactory}
import utils.RepositoryUtils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.inject.{Inject, Singleton}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

@Singleton
class ReportHelper @Inject()(repositoryUtils: RepositoryUtils, communityResourceManager: CommunityResourceManager) {

  private def LOG: Logger = LoggerFactory.getLogger(classOf[ReportHelper])

  def createReportSpecs(communityId: Option[Long] = None): ReportSpecs = {
    var specs = ReportSpecs.build()
      .withTempFolder(Path.of(repositoryUtils.getTempReportFolder().toPath.toString, "html"))
    specs = specs.withResourceResolver((resourceUri: String) => {
      var resolvedPath: String = null
      if (resourceUri != null) {
        if (communityId.isDefined && resourceUri.startsWith("resources/")) {
          // Community-specific resource
          val resourceName = URLDecoder.decode(Strings.CS.removeStart(resourceUri, "resources/"), StandardCharsets.UTF_8)
          // We cannot work with async tasks at this point - do a synchronous lookup of the file
          val file = Await.result(communityResourceManager.getCommunityResourceFileByNameAndCommunity(communityId.get, resourceName), Duration(30, SECONDS))
          if (file.nonEmpty) {
            resolvedPath = file.get.toURI.toString
          }
        } else if (resourceUri.startsWith("systemResources/")) {
          // System resource
          val resourceName = URLDecoder.decode(Strings.CS.removeStart(resourceUri, "systemResources/"), StandardCharsets.UTF_8)
          // We cannot work with async tasks at this point - do a synchronous lookup of the file
          val file = Await.result(communityResourceManager.getSystemResourceFileByName(resourceName), Duration(30, SECONDS))
          if (file.nonEmpty) {
            resolvedPath = file.get.toURI.toString
          }
        } else if (resourceUri.startsWith("badgereportpreview/")) {
          // Conformance badge
          // URI format is as such: badgereportpreview/status/systemId/specificationId/actorId/snapshotId
          val uriParts = resourceUri.split('/')
          if (uriParts.length >= 5) {
            val status = uriParts(1)
            val specificationId = uriParts(3).toLong
            val actorId = uriParts(4).toLong
            val snapshotId = if (uriParts.length == 6) {
              Some(uriParts(5).toLong)
            } else {
              None
            }
            val badge = repositoryUtils.getConformanceBadge(specificationId, Some(actorId), snapshotId, status, exactMatch = false, forReport = true)
            resolvedPath = badge.map(_.toURI.toString).orNull
          } else {
            LOG.warn("Unexpected badge report preview URI format [{}]", resourceUri)
          }
        }
      }
      resolvedPath
    })
    specs
  }

}
