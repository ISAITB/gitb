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

import models.prerequisites.WithPrerequisite

case class OrganisationParameters(id: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, inExports: Boolean, inSelfRegistration: Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], community: Long)

class OrganisationParametersWithValue(_parameter: OrganisationParameters, _value: Option[OrganisationParameterValues]) extends WithPrerequisite {
  var parameter: OrganisationParameters = _parameter
  var value: Option[OrganisationParameterValues] = _value

  override def prerequisiteKey(): Option[String] = parameter.dependsOn
  override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
  override def currentKey(): String = parameter.testKey
  override def currentValue(): Option[String] = if (value.isDefined) Some(value.get.value) else None
}
