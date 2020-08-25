package actors.events

import models.Enums.TriggerEventType

class OrganisationCreatedEvent(override val communityId: Long, val organisationId: Long) extends TriggerEvent(communityId = communityId, TriggerEventType.OrganisationCreated) {
}
