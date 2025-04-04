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
