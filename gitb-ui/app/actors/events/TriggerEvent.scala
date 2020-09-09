package actors.events

import models.Enums.TriggerEventType.TriggerEventType

abstract class TriggerEvent(val communityId: Long, val eventType: TriggerEventType) {}