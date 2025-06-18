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
