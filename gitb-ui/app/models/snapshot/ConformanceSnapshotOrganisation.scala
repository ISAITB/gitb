package models.snapshot

case class ConformanceSnapshotOrganisation(
  id: Long,
  shortname: String,
  fullname: String,
  apiKey: Option[String],
  snapshotId: Long
)
