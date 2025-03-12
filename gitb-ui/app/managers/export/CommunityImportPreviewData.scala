package managers.`export`

import models.Enums.LabelType.LabelType
import models._

case class CommunityImportPreviewData(administratorMap: Map[String, Users],
                                      organisationPropertyMap: Map[String, OrganisationParameters],
                                      organisationPropertyIdMap: Map[Long, OrganisationParameters],
                                      systemPropertyMap: Map[String, SystemParameters],
                                      systemPropertyIdMap: Map[Long, SystemParameters],
                                      customLabelMap: Map[LabelType, models.CommunityLabels],
                                      landingPageMap: Map[String, List[Long]],
                                      legalNoticeMap: Map[String, List[Long]],
                                      errorTemplateMap: Map[String, List[Long]],
                                      triggerMap: Map[String, List[Long]],
                                      resourceMap: Map[String, List[Long]],
                                      organisationMap: Map[String, List[Organizations]],
                                      organisationUserMap: Map[Long, Map[String, Users]],
                                      organisationPropertiesValueMap: Map[Long, Map[String, OrganisationParameterValues]],
                                      systemMap: Map[Long, Map[String, List[Systems]]],
                                      systemPropertyValueMap: Map[Long, Map[String, SystemParameterValues]],
                                      statementMap: Map[Long, Map[Long, (Specifications, Actors)]],
                                      statementConfigurationMap: Map[String, Map[String, Configs]]
                                     )
