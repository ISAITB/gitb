package models.snapshot

import models.ConformanceOverviewCertificateMessage

case class ConformanceSnapshotOverviewCertificateMessage(id: Long, message: String, messageType: Short, domainId: Option[Long], groupId: Option[Long], specificationId: Option[Long], actorId: Option[Long], snapshotId: Long) {

  def toConformanceOverviewCertificateMessage(): ConformanceOverviewCertificateMessage = {
    ConformanceOverviewCertificateMessage(id, messageType, message, domainId, groupId, specificationId, actorId, 0L)
  }

}
