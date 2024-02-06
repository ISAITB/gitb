package models.snapshot

case class ConformanceSnapshotTestSuite(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  identifier: String,
  specReference: Option[String],
  specDescription: Option[String],
  specLink: Option[String],
  snapshotId: Long
)
