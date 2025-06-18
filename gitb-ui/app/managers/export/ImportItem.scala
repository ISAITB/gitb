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

import models.Enums.ImportItemChoice._
import models.Enums.ImportItemMatch._
import models.Enums.{ImportItemChoice, ImportItemType}
import models.Enums.ImportItemType._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ImportItem(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String]) {
  require(_itemType == ImportItemType.Settings || _targetKey.isDefined || _sourceKey.isDefined, "Either the target or the source must be defined for an import item")
  var itemName: Option[String] = _itemName
  var itemType: ImportItemType = _itemType
  var itemMatch: ImportItemMatch = _itemMatch
  var itemChoice: Option[ImportItemChoice] = None
  var targetKey: Option[String] = _targetKey
  var sourceKey: Option[String] = _sourceKey
  var parentItem: Option[ImportItem] = None
  val childrenItems: ListBuffer[ImportItem] = new ListBuffer[ImportItem]()

  def this(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String], _parentItem: Option[ImportItem]) = {
    this(_itemName, _itemType, _itemMatch, _targetKey, _sourceKey)
    if (_parentItem.isDefined) {
      parentItem = _parentItem
      parentItem.get.childrenItems += this
    }
  }

  def this(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String], _itemChoice: ImportItemChoice) = {
    this(_itemName, _itemType, _itemMatch, _targetKey, _sourceKey)
    itemChoice = Some(_itemChoice)
  }

  def toSourceMap(): mutable.Map[ImportItemType.Value, mutable.Map[String, ImportItem]] = {
    val map = mutable.Map[ImportItemType.Value, mutable.Map[String, ImportItem]]()
    addToMap(this, map, (item: ImportItem) => {item.sourceKey})
    map
  }

  def toTargetMap(): mutable.Map[ImportItemType.Value, mutable.Map[String, ImportItem]] = {
    val map = mutable.Map[ImportItemType.Value, mutable.Map[String, ImportItem]]()
    addToMap(this, map, (item: ImportItem) => {item.targetKey})
    map
  }

  private def addToMap(item: ImportItem, map: mutable.Map[ImportItemType.Value, mutable.Map[String, ImportItem]], keyFn: ImportItem => Option[String]): Unit = {
    val keyValue = keyFn.apply(item)
    if (keyValue.isDefined) {
      var itemTypeMap = map.get(item.itemType)
      if (itemTypeMap.isEmpty) {
        itemTypeMap = Some(mutable.Map[String, ImportItem]())
        map += (item.itemType -> itemTypeMap.get)
      }
      itemTypeMap.get += (keyValue.get -> item)
    }
    item.childrenItems.foreach { child =>
      addToMap(child, map, keyFn)
    }
  }

  def markAllChildrenAsSkipped(): Unit = {
    childrenItems.foreach { child =>
      child.itemChoice = Some(ImportItemChoice.Skip)
      child.markAllChildrenAsSkipped()
    }
  }

}
