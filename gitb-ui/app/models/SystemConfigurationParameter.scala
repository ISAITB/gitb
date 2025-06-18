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

class SystemConfigurationParameter(_parameter: Parameters, _configured: Boolean, _config: Option[Configs]) extends WithPrerequisite {
  var parameter: Parameters = _parameter
  var config: Option[Configs] = _config
  var configured: Boolean = _configured

  override def prerequisiteKey(): Option[String] = parameter.dependsOn
  override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
  override def currentKey(): String = parameter.name
  override def currentValue(): Option[String] = if (config.isDefined) Some(config.get.value) else None
}
