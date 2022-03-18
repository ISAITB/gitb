package actors.events.sessions

case class TerminateAllSessionsEvent(communityId: Option[Long], organisationId: Option[Long], systemId: Option[Long])
