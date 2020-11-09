package actors.events

import models.Enums.TriggerEventType

class ConformanceStatementSucceededEvent(override val communityId: Long, val systemId: Long, val actorId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.ConformanceStatementSucceeded) {
}
