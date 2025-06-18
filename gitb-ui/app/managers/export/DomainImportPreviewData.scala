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

case class DomainImportPreviewData(specificationGroupMap: Map[String, models.SpecificationGroups],
                                   specificationMap: Map[String, models.Specifications],
                                   specificationIdMap: Map[Long, models.Specifications],
                                   domainTestSuiteMap: Map[String, models.TestSuites],
                                   specificationTestSuiteMap: Map[Long, Map[String, models.TestSuites]],
                                   specificationActorMap: Map[Long, Map[String, models.Actors]],
                                   actorEndpointMap: Map[Long, Map[String, models.Endpoints]],
                                   endpointParameterMap: Map[Long, Map[String, models.Parameters]],
                                   domainParametersMap: Map[String, models.DomainParameter]
                                  )
