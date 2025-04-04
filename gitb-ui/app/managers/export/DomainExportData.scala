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
