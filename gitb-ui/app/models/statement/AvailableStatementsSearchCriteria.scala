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

case class AvailableStatementsSearchCriteria(filterText: Option[String], selected: Boolean, unselected: Boolean, selectedIds: Set[Long]) {

  private val filterTextToApply = filterText.map(_.trim.toLowerCase)

  def isApplicable(): Boolean = {
    filterText.isDefined || !selected || !unselected
  }

  def checkItem(item: ConformanceStatementItem, onlyCheckStatus: Boolean): Boolean = {
    var matches = if (!onlyCheckStatus && filterTextToApply.isDefined) {
      filterTextToApply.exists(item.name.toLowerCase.contains)
    } else {
      true
    }
    if (matches && (item.items.isEmpty || item.items.exists(_.isEmpty))) {
      matches = (selected && selectedIds.contains(item.id)) || (unselected && !selectedIds.contains(item.id))
    }
    matches
  }

}
