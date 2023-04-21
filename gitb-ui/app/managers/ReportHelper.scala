package managers

import com.gitb.reports.ReportSpecs
import utils.RepositoryUtils

import java.nio.file.Path
import javax.inject.{Inject, Singleton}

@Singleton
class ReportHelper @Inject()(repositoryUtils: RepositoryUtils, communityResourceManager: CommunityResourceManager) {

  def createReportSpecs(communityId: Option[Long] = None): ReportSpecs = {
    var specs = ReportSpecs.build()
      .withTempFolder(Path.of(repositoryUtils.getTempReportFolder().toPath.toString, "html"))
    if (communityId.isDefined) {
      specs = specs.withResourceResolver((resourceName: String) => {
        val file = communityResourceManager.getCommunityResourceFileByNameAndCommunity(communityId.get, resourceName)
        if (file.nonEmpty) {
          file.get.toURI.toString
        } else {
          null
        }
      })
    }
    specs
  }

}
