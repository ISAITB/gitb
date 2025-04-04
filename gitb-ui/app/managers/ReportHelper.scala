package managers

import com.gitb.reports.ReportSpecs
import org.apache.commons.lang3.StringUtils
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
          val resourceName = URLDecoder.decode(StringUtils.removeStart(resourceUri, "resources/"), StandardCharsets.UTF_8)
          // We cannot work with async tasks at this point - do a synchronous lookup of the file
          val file = Await.result(communityResourceManager.getCommunityResourceFileByNameAndCommunity(communityId.get, resourceName), Duration(30, SECONDS))
          if (file.nonEmpty) {
            resolvedPath = file.get.toURI.toString
          }
        } else if (resourceUri.startsWith("systemResources/")) {
          // System resource
          val resourceName = URLDecoder.decode(StringUtils.removeStart(resourceUri, "systemResources/"), StandardCharsets.UTF_8)
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
