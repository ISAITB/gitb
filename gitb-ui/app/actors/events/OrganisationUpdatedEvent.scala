package actors.events

import models.Enums.TriggerEventType

class OrganisationUpdatedEvent(override val communityId: Long, val organisationId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.OrganisationUpdated) {
}
