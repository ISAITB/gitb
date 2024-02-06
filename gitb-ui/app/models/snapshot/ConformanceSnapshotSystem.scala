package models.snapshot

case class ConformanceSnapshotSystem(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  apiKey: Option[String],
  badgeKey: String,
  snapshotId: Long
)