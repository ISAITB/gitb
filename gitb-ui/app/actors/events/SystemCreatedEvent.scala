package actors.events

import models.Enums.TriggerEventType

class SystemCreatedEvent(override val communityId: Long, val systemId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.SystemCreated) {
}
