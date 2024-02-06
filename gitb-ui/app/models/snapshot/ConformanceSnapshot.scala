package models.snapshot

import java.sql.Timestamp

case class ConformanceSnapshot(id: Long, label: String, publicLabel: Option[String], snapshotTime: Timestamp, apiKey: String, isPublic: Boolean, community: Long)
