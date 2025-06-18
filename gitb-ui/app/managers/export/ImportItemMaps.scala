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

import scala.collection.mutable

object ImportItemMaps {

  def empty(): ImportItemMaps = {
    ImportItemMaps(new mutable.HashMap[ImportItemType, mutable.Map[String, ImportItem]], new mutable.HashMap[ImportItemType, mutable.Map[String, ImportItem]])
  }

}

/*
Import items to facilitate lookup of actions:
targetMap: items for which there are definitely relevant data in the DB (updates or deletes)
sourceMap: items for which there are definitely data in the export archive (additions or updates)
 */
case class ImportItemMaps(sourceMap: mutable.Map[ImportItemType, mutable.Map[String, ImportItem]], targetMap: mutable.Map[ImportItemType, mutable.Map[String, ImportItem]]) {

  def merge(other: ImportItemMaps): ImportItemMaps = {
    this.sourceMap.addAll(other.sourceMap)
    this.targetMap.addAll(other.targetMap)
    this
  }

}
