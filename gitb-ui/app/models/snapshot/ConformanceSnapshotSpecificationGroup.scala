package models.snapshot

case class ConformanceSnapshotSpecificationGroup(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  displayOrder: Short,
  snapshotId: Long
)
