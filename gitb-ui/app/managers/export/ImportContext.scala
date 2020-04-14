package managers.export

import models.Enums.ImportItemType.ImportItemType

import scala.collection.mutable

case class ImportContext(importSettings: ImportSettings, importItemMaps: ImportItemMaps, existingIds: ExistingIds, importTargets: ImportTargets,
                        processedIdMap: mutable.Map[ImportItemType, mutable.Map[String, String]], // [Item type] to map of [XML ID] to [DB TARGET KEY]
                        savedSpecificationActors: mutable.Map[Long, mutable.Map[String, Long]] // [Specification ID] to map of [actorIdentifier] to [actor ID]
)
