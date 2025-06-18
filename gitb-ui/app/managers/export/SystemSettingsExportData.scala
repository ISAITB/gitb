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

package managers.`export`

import models.theme.Theme
import models.{CommunityResources, ErrorTemplates, LandingPages, LegalNotices, SystemConfigurationsWithEnvironment, Users}

case class SystemSettingsExportData(systemResources: Option[List[CommunityResources]],
                                    themes: Option[Seq[Theme]],
                                    landingPages: Option[List[LandingPages]],
                                    legalNotices: Option[List[LegalNotices]],
                                    errorTemplates: Option[List[ErrorTemplates]],
                                    systemAdministrators: Option[Seq[Users]],
                                    systemConfigurations: Option[List[SystemConfigurationsWithEnvironment]]
                                   )
