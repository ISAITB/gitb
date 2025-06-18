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
