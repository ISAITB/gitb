package models

case class CommunityReportSettings(reportType: Short, signPdfs: Boolean, customPdfs: Boolean, customPdfsWithCustomXml: Boolean, customPdfService: Option[String], community: Long)