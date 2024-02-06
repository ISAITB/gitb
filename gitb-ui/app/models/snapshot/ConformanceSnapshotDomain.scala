package models.snapshot

case class ConformanceSnapshotDomain(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  snapshotId: Long
)
