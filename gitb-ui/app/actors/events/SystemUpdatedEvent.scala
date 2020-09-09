package actors.events

import models.Enums.TriggerEventType

class SystemUpdatedEvent(override val communityId: Long, val systemId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.SystemUpdated) {
}
