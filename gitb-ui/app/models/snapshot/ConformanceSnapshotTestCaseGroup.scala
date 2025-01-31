package models.snapshot

case class ConformanceSnapshotTestCaseGroup(
  id: Long,
  identifier: String,
  name: Option[String],
  description: Option[String],
  snapshotId: Long
)
