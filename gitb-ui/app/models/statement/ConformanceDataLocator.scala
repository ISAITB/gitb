package models.statement

import models.ConformanceStatementItem
import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ConformanceDataLocator(val data: ConformanceData) {

  private var itemsPerType: Option[mutable.HashMap[ConformanceStatementItemType, ListBuffer[ConformanceStatementItem]]] = None

  private def parseItems(items: Seq[ConformanceStatementItem]): Unit = {
    items.foreach { item =>
      if (!itemsPerType.get.contains(item.itemType)) {
        itemsPerType.get += (item.itemType -> new ListBuffer[ConformanceStatementItem])
      }
      itemsPerType.get(item.itemType) += item
      if (item.items.isDefined) {
        parseItems(item.items.get)
      }
    }
  }

  private def getItems(): mutable.HashMap[ConformanceStatementItemType, ListBuffer[ConformanceStatementItem]]  = {
    if (itemsPerType.isEmpty) {
      itemsPerType = Some(new mutable.HashMap[ConformanceStatementItemType, ListBuffer[ConformanceStatementItem]])
      parseItems(data.conformanceItemTree)
    }
    itemsPerType.get
  }

  def itemTypeNameByIndex(indexes: Iterable[Int], itemType: ConformanceStatementItemType): Map[Int, String] = {
    val map = new mutable.HashMap[Int, String]()
    indexes.foreach { index =>
      val result = getItems().get(itemType).flatMap(_.lift(index))
      if (result.isDefined) {
        map += (index -> result.get.name)
      }
    }
    map.toMap
  }

}
