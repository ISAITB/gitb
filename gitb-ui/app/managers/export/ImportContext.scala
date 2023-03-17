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
