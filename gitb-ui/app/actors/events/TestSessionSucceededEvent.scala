package actors.events

import models.Enums.TriggerEventType

class TestSessionSucceededEvent(override val communityId: Long, val systemId: Long, val actorId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.TestSessionSucceeded) {
}
