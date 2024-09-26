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
