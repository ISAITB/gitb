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
