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

package models.health

import config.Configurations

object SoftwareVersionCheckSettings {

    def fromEnvironment(): SoftwareVersionCheckSettings = {
        SoftwareVersionCheckSettings(
            Configurations.SOFTWARE_VERSION_CHECK_ENABLED,
            Configurations.SOFTWARE_VERSION_CHECK_INFO_URL,
            Configurations.SOFTWARE_VERSION_CHECK_JWKS_URL
        )
    }

}

case class SoftwareVersionCheckSettings(enabled: Boolean, jws: String, jwks: String) {

    def toEnvironment(): Unit = {
        Configurations.SOFTWARE_VERSION_CHECK_ENABLED = enabled
        if (enabled) {
            Configurations.SOFTWARE_VERSION_CHECK_INFO_URL = jws
            Configurations.SOFTWARE_VERSION_CHECK_JWKS_URL = jwks
        }
    }

}
