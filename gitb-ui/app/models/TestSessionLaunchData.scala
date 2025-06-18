/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package models

import com.gitb.core.{ActorConfiguration, AnyContent}

case class TestSessionLaunchData(
                                  communityId: Long,
                                  organisationId: Long,
                                  systemId: Long,
                                  actorId: Long,
                                  testCases: List[Long],
                                  statementParameters: List[TypedActorConfiguration],
                                  domainParameters: Option[TypedActorConfiguration],
                                  organisationParameters: TypedActorConfiguration,
                                  systemParameters: TypedActorConfiguration,
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
