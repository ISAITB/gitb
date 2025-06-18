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

class TestSuiteUploadItemResult(var itemName: String, var itemType: String, var actionType: String, var specification: Long) {
}

object TestSuiteUploadItemResult {

  val ITEM_TYPE_ACTOR = "actor"
  val ITEM_TYPE_ENDPOINT = "endpoint"
  val ITEM_TYPE_PARAMETER = "parameter"
  val ITEM_TYPE_TEST_CASE = "testCase"
  val ITEM_TYPE_TEST_SUITE = "testSuite"

  val ACTION_TYPE_ADD = "add"
  val ACTION_TYPE_UPDATE = "update"
  val ACTION_TYPE_REMOVE = "remove"
  val ACTION_TYPE_UNCHANGED = "unchanged"

}