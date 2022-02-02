package actors.events.sessions

import com.gitb.core.{ActorConfiguration, AnyContent}

case class TestSessionLaunchData(
   communityId: Long,
   organisationId: Long,
   systemId: Long,
   actorId: Long,
   testCases: List[Long],
   statementParameters: List[ActorConfiguration],
   domainParameters: Option[ActorConfiguration],
   organisationParameters: ActorConfiguration,
   systemParameters: ActorConfiguration,
   testCaseToInputMap: Option[Map[Long, List[AnyContent]]],
   forceSequentialExecution: Boolean)