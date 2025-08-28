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

package models

import models.Enums.ConformanceStatementItemType.ConformanceStatementItemType
import models.statement.ConformanceStatementResults

object ConformanceStatementItem {

  def apply(id: Long,
            name: String,
            description: Option[String],
            reportMetadata: Option[String],
            itemType: ConformanceStatementItemType,
            items: Option[Iterable[ConformanceStatementItem]],
            displayOrder: Short,
            results: Option[ConformanceStatementResults] = None,
            actorToShow: Boolean = true): ConformanceStatementItem = {
    new ConformanceStatementItem(id, name, description, reportMetadata, itemType, items, displayOrder, results, actorToShow)
  }

}

class ConformanceStatementItem(val id: Long,
                               val name: String,
                               val description: Option[String],
                               val reportMetadata: Option[String],
                               val itemType: ConformanceStatementItemType,
                               var items: Option[Iterable[ConformanceStatementItem]],
                               val displayOrder: Short,
                               val results: Option[ConformanceStatementResults] = None,
                               val actorToShow: Boolean = true) {

  def withChildren(children: Iterable[ConformanceStatementItem]): ConformanceStatementItem = {
    if (children.isEmpty) {
      items = None
    } else {
      items = Some(children)
    }
    this
  }
}
