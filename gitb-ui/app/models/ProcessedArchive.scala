package models

import java.sql.Timestamp

case class ProcessedArchive(id: Long, hash: String, processTime: Timestamp)
