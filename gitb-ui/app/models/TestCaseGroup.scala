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

case class TestCaseGroup(id: Long, identifier: String, name: Option[String], description: Option[String], testSuite: Long) {

  def withIds(groupId: Long, testSuiteId: Long): TestCaseGroup = {
    TestCaseGroup(groupId, this.identifier, this.name, this.description, testSuiteId)
  }

  override def equals(obj: Any): Boolean = {
    this.id.equals(obj.asInstanceOf[TestCaseGroup].id)
  }

  override def hashCode(): Int = {
    this.id.hashCode()
  }

}
