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

import com.gitb.PropertyConstants
import com.gitb.core.{ActorConfiguration, AnyContent, Configuration, ValueEmbeddingEnumeration}

object TypedActorConfiguration {

  def fromAnyContent(values: List[AnyContent]): TypedActorConfiguration = {
    val configs = values
      .filter(x => x.getEmbeddingMethod == ValueEmbeddingEnumeration.STRING)
      .map { value =>
        val config = new Configuration()
        config.setName(value.getName)
        config.setValue(value.getValue)
        TypedConfiguration(config, "SIMPLE")
      }
    TypedActorConfiguration(PropertyConstants.ACTOR_CONFIG_VARIABLES, PropertyConstants.ACTOR_CONFIG_VARIABLES, configs)
  }

  def fromSettings(): TypedActorConfiguration = {
    val settings = TestEngineCallbackSettings.fromEnvironment()
    TypedActorConfiguration(PropertyConstants.ACTOR_CONFIG_SETTINGS, PropertyConstants.ACTOR_CONFIG_SETTINGS, List(
      TypedConfiguration(createBooleanConfig(PropertyConstants.TEST_SERVICE_CALLBACKS_REST_ENABLED, settings.restEnabled), "SIMPLE"),
      TypedConfiguration(createBooleanConfig(PropertyConstants.TEST_SERVICE_CALLBACKS_SOAP_ENABLED, settings.soapEnabled), "SIMPLE"),
      TypedConfiguration(createBooleanConfig(PropertyConstants.TEST_SERVICE_CALLBACKS_API_KEYS_ENABLED, settings.apiKeysEnabled), "SIMPLE")
    ))
  }

  private def createBooleanConfig(name: String, value: Boolean): Configuration = {
    val config = new Configuration()
    config.setName(name)
    config.setValue(value.toString)
    config
  }

}

case class TypedActorConfiguration(actor: String, endpoint: String, config: List[TypedConfiguration]) {

  def toActorConfiguration(): ActorConfiguration = {
    val actorConfiguration = new ActorConfiguration()
    actorConfiguration.setActor(actor)
    actorConfiguration.setEndpoint(endpoint)
    import scala.jdk.CollectionConverters._
    actorConfiguration.getConfig.addAll(config.map(_.data).asJava)
    actorConfiguration
  }

}
