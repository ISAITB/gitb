package models.snapshot

case class ConformanceSnapshotTestCase(
  id: Long,
  shortname: String,
  fullname: String,
  description: Option[String],
  testSuiteOrder: Short,
  identifier: String,
  isOptional: Boolean,
  isDisabled: Boolean,
  tags: Option[String],
  specReference: Option[String],
  specDescription: Option[String],
  specLink: Option[String],
  snapshotId: Long
)
