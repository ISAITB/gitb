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
import com.gitb.core.{AnyContent, Configuration, ValueEmbeddingEnumeration}

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

}

case class TypedActorConfiguration(actor: String, endpoint: String, config: List[TypedConfiguration])
