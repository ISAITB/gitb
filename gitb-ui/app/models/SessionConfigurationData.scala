/*
 * Copyright (C) 2026 European Union
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

import com.gitb.core.ActorConfiguration

import java.util

case class SessionConfigurationData(statementParameters: Option[List[TypedActorConfiguration]],
                                    domainParameters: Option[TypedActorConfiguration],
                                    organisationParameters: Option[TypedActorConfiguration],
                                    systemParameters: Option[TypedActorConfiguration],
                                    testServiceParameters: Option[List[TypedActorConfiguration]],
                                    predefinedVariables: Option[TypedActorConfiguration],
                                    settings: Option[TypedActorConfiguration]) {

  def apply(aggregatedConfiguration: util.List[ActorConfiguration]): Unit = {
    import scala.jdk.CollectionConverters._
    if (statementParameters.nonEmpty) {
      aggregatedConfiguration.addAll(statementParameters.get.map(_.toActorConfiguration()).asJava)
    }
    if (domainParameters.nonEmpty) {
      aggregatedConfiguration.add(domainParameters.get.toActorConfiguration())
    }
    if (organisationParameters.nonEmpty) {
      aggregatedConfiguration.add(organisationParameters.get.toActorConfiguration())
    }
    if (systemParameters.nonEmpty) {
      aggregatedConfiguration.add(systemParameters.get.toActorConfiguration())
    }
    if (testServiceParameters.nonEmpty) {
      aggregatedConfiguration.addAll(testServiceParameters.get.map(_.toActorConfiguration()).asJava)
    }
    if (predefinedVariables.nonEmpty) {
      aggregatedConfiguration.add(predefinedVariables.get.toActorConfiguration())
    }
    if (settings.nonEmpty) {
      aggregatedConfiguration.add(settings.get.toActorConfiguration())
    }
  }

}
