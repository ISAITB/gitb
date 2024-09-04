package actors.events.sessions

case class TerminateSessionsEvent(organisationId: Long, testSessions: Set[String])
