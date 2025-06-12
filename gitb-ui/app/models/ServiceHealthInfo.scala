package models

import models.Enums.ServiceHealthStatusType.ServiceHealthStatusType

case class ServiceHealthInfo(status: ServiceHealthStatusType, summary: String, details: String)
