package actors.events

import models.Enums.TriggerEventType

class TestSessionStartedEvent(override val communityId: Long, val sessionId: String) extends TriggerEvent(communityId = communityId, TriggerEventType.TestSessionStarted) {
}
