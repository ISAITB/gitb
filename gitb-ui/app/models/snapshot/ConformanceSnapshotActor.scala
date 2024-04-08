package models.snapshot

case class ConformanceSnapshotActor(
  id: Long,
  actorId: String,
  name: String,
  description: Option[String],
  visible: Boolean,
  apiKey: String,
  snapshotId: Long
)