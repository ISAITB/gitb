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

import com.gitb.core.ActorConfiguration

import java.util

case class SessionConfigurationData(statementParameters: Option[List[TypedActorConfiguration]],
                                    domainParameters: Option[TypedActorConfiguration],
                                    organisationParameters: Option[TypedActorConfiguration],
                                    systemParameters: Option[TypedActorConfiguration]) {

  def apply(aggregatedConfiguration: util.List[ActorConfiguration]): Unit = {
    import scala.jdk.CollectionConverters._
    if (statementParameters.nonEmpty) {
      aggregatedConfiguration.addAll(statementParameters.get.map(toActorConfiguration).asJava)
    }
    if (domainParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(domainParameters.get))
    }
    if (organisationParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(organisationParameters.get))
    }
    if (systemParameters.nonEmpty) {
      aggregatedConfiguration.add(toActorConfiguration(systemParameters.get))
    }

  }

  private def toActorConfiguration(typedActorConfiguration: TypedActorConfiguration) = {
    val actorConfiguration = new ActorConfiguration()
    actorConfiguration.setActor(typedActorConfiguration.actor)
    actorConfiguration.setEndpoint(typedActorConfiguration.endpoint)
    import scala.jdk.CollectionConverters._
    actorConfiguration.getConfig.addAll(typedActorConfiguration.config.map(_.data).asJava)
    actorConfiguration
  }

}
