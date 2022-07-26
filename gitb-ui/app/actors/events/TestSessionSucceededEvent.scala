package actors.events

import models.Enums.TriggerEventType

class TestSessionSucceededEvent(override val communityId: Long, val sessionId: String) extends TriggerEvent(communityId = communityId, TriggerEventType.TestSessionSucceeded) {
}
