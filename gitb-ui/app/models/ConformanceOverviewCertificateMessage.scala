package models

case class ConformanceOverviewCertificateMessage(id: Long, messageType: Short, message: String, domain: Option[Long], group: Option[Long], specification: Option[Long], actor: Option[Long], community: Long)
