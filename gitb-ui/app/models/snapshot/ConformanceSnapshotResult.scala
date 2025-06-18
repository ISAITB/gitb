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

package models.snapshot

import java.sql.Timestamp

/**
 * Created by simatosc.
 */
case class ConformanceSnapshotResult(
  id: Long,
  snapshotId: Long,
  organisationId: Long,
  systemId: Long,
  domainId: Long,
  specGroupId: Option[Long],
  specId: Long,
  actorId: Long,
  testSuiteId: Long,
  testCaseId: Long,
  testCaseGroupId: Option[Long],
  testSession: Option[String],
  result: String,
  outputMessage: Option[String],
  updateTime: Option[Timestamp]
)
