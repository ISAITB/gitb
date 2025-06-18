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

class SystemConfigurationParameterMinimal(var parameterId: Long, var endpointName: String, var parameterName: String, var parameterKey: String, var parameterValue: Option[String], var dependsOn: Option[String], var dependsOnValue: Option[String], var parameterKind: String, var parameterUse: String, var valueContentType: Option[String], var notForTests: Boolean) extends WithPrerequisite {

  override def prerequisiteKey(): Option[String] = dependsOn
  override def prerequisiteValue(): Option[String] = dependsOnValue
  override def currentKey(): String = parameterName
  override def currentValue(): Option[String] = if (parameterValue.isDefined) Some(parameterValue.get) else None
}
