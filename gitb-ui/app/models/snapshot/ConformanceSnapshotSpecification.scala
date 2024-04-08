package models.snapshot

case class ConformanceSnapshotSpecification(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  apiKey: String,
  displayOrder: Short,
  snapshotId: Long
)
