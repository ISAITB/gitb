package models

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
                                  sessionIdsToAssign: Option[Map[Long, String]],
                                  forceSequentialExecution: Boolean) {

  def newWithoutTestCaseIds(testCaseIds: Set[Long]): TestSessionLaunchData = {
    TestSessionLaunchData(
      communityId, organisationId, systemId, actorId,
      testCases.filterNot(testCaseIds.contains),
      statementParameters, domainParameters, organisationParameters, systemParameters,
      testCaseToInputMap.map(_.removedAll(testCaseIds)),
      sessionIdsToAssign.map(_.removedAll(testCaseIds)),
      forceSequentialExecution
    )
  }

}
