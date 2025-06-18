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

package managers.export

import models.Enums.ImportItemType.ImportItemType
import models.TestCases

import scala.collection.mutable

case class ImportContext(importSettings: ImportSettings, importItemMaps: ImportItemMaps, existingIds: ExistingIds, importTargets: ImportTargets,
                        processedIdMap: mutable.Map[ImportItemType, mutable.Map[String, String]], // [Item type] to map of [XML ID] to [DB TARGET KEY]
                        savedSpecificationActors: mutable.Map[Long, mutable.Map[String, Long]], // [Specification ID] to map of [actorIdentifier] to [actor ID]
                        sharedTestSuiteInfo: mutable.Map[Long, (Option[List[TestCases]], Map[String, (Long, Boolean)])], // [Test suite ID] to [List of test cases] and [Map of test case identifier to DB ID and new/updated flag]
                        onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]
)
