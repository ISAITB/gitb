package models.snapshot

case class ConformanceSnapshotActor(
  id: Long,
  actorId: String,
  name: String,
  description: Option[String],
  apiKey: String,
  snapshotId: Long
)