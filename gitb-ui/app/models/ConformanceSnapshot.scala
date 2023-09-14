package models

import java.sql.Timestamp

case class ConformanceSnapshot(id: Long, label: String, snapshotTime: Timestamp, community: Long)
