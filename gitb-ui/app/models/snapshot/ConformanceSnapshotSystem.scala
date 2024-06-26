package models.snapshot

case class ConformanceSnapshotSystem(
  id: Long,
  shortname: String,
  fullname: String,
  version: Option[String],
  description: Option[String],
  apiKey: String,
  badgeKey: String,
  snapshotId: Long
)