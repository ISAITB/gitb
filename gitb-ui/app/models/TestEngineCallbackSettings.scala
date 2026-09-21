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

import config.Configurations

object TestEngineCallbackSettings {

    def fromEnvironment(): TestEngineCallbackSettings = {
        TestEngineCallbackSettings(
            Configurations.TEST_SERVICE_CALLBACKS_ENABLED,
            Configurations.TEST_SERVICE_CALLBACKS_SOAP_ENABLED,
            Configurations.TEST_SERVICE_CALLBACKS_REST_ENABLED,
            Configurations.TEST_SERVICE_CALLBACKS_API_KEYS_ENABLED
        )
    }

}

case class TestEngineCallbackSettings(enabled: Boolean, soapEnabled: Boolean, restEnabled: Boolean, apiKeysEnabled: Boolean) {

    def toEnvironment(): Unit = {
        Configurations.TEST_SERVICE_CALLBACKS_ENABLED = enabled
        Configurations.TEST_SERVICE_CALLBACKS_SOAP_ENABLED = enabled && soapEnabled
        Configurations.TEST_SERVICE_CALLBACKS_REST_ENABLED = enabled && restEnabled
        Configurations.TEST_SERVICE_CALLBACKS_API_KEYS_ENABLED = enabled && apiKeysEnabled
    }

}
