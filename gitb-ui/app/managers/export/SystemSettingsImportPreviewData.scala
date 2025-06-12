package managers.`export`

import models.Users

case class SystemSettingsImportPreviewData(resourceMap: Map[String, List[Long]],
                                           themeMap: Map[String, List[Long]],
                                           systemAdministrators: Map[String, Users],
                                           defaultLandingPages: Map[String, List[Long]],
                                           defaultLegalNotices: Map[String, List[Long]],
                                           defaultErrorTemplates: Map[String, List[Long]],
                                           systemSettings: Set[String]
                                          ) {

}
