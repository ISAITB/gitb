package models

import org.apache.commons.io.FileUtils

import java.nio.file.Path

case class SessionReportPath(report: Option[Path], sessionFolderInfo: SessionFolderInfo) {

  def cleanup(): Unit = {
    if (sessionFolderInfo.archived) {
      FileUtils.deleteQuietly(sessionFolderInfo.path.toFile)
    }
  }

}
