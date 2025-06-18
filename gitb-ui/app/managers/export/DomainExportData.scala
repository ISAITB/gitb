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

import models._

case class DomainExportData(domain: Option[Domain],
                            sharedTestSuites: Option[Seq[TestSuites]],
                            specificationGroups: Option[List[SpecificationGroups]],
                            specifications: Option[Seq[Specifications]],
                            specificationActorMap: Option[Map[Long, List[Actors]]],
                            actorEndpointMap: Option[Map[Long, List[Endpoints]]],
                            endpointParameterMap: Option[Map[Long, List[Parameters]]],
                            testSuiteActorMap: Option[Map[Long, List[Long]]],
                            testSuiteTestCaseGroupMap: Option[Map[Long, List[TestCaseGroup]]],
                            testSuiteTestCaseMap: Option[Map[Long, List[TestCases]]],
                            specificationTestSuiteMap: Option[Map[Long, List[TestSuites]]],
                            domainParameters: Option[List[DomainParameter]]
                           )
