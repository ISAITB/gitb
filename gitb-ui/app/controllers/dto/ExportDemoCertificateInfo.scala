package controllers.dto

import models.CommunityReportSettings

import java.nio.file.Path

case class ExportDemoCertificateInfo(reportSettings: CommunityReportSettings, stylesheetPath: Option[Path], jsSettings: String, reportPath: Path, paramMap: Option[Map[String, Seq[String]]])
