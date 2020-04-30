package managers.export

import models.Enums.ImportItemChoice._
import models.Enums.ImportItemMatch._
import models.Enums.{ImportItemChoice, ImportItemType}
import models.Enums.ImportItemType._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ImportItem(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String]) {
  require(_targetKey.isDefined || _sourceKey.isDefined, "Either the target or the source must be defined for an import item")
  var itemName: Option[String] = _itemName
  var itemType: ImportItemType = _itemType
  var itemMatch: ImportItemMatch = _itemMatch
  var itemChoice: Option[ImportItemChoice] = None
  var targetKey: Option[String] = _targetKey
  var sourceKey: Option[String] = _sourceKey
  var parentItem: Option[ImportItem] = None
  val childrenItems: ListBuffer[ImportItem] = new ListBuffer[ImportItem]()

  def this(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String], _parentItem: ImportItem) {
    this(_itemName, _itemType, _itemMatch, _targetKey, _sourceKey)
    if (_parentItem != null) {
      parentItem = Some(_parentItem)
      parentItem.get.childrenItems += this
    }
  }

  def this(_itemName: Option[String], _itemType: ImportItemType, _itemMatch: ImportItemMatch, _targetKey: Option[String], _sourceKey: Option[String], _itemChoice: ImportItemChoice) {
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
