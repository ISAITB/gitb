package actors.events

import models.Enums.TriggerEventType

class TestSessionFailedEvent(override val communityId: Long, val sessionId: String) extends TriggerEvent(communityId = communityId, TriggerEventType.TestSessionFailed) {
}
