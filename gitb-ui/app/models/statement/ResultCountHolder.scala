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

import com.gitb.tr.TestResultType

trait ResultCountHolder extends ResultHolder {

  def completedCount(): Long
  def failedCount(): Long
  def otherCount(): Long

  override def resultStatus(): String = {
    if (failedCount() > 0) {
      TestResultType.FAILURE.toString
    } else if (otherCount() > 0) {
      TestResultType.UNDEFINED.toString
    } else if (completedCount() > 0) {
      TestResultType.SUCCESS.toString
    } else {
      TestResultType.UNDEFINED.toString
    }

  }

}
