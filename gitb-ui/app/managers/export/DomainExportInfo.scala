package managers.export

case class DomainExportInfo(
                             latestSequenceId: Int,
                             exportedActorMap: Map[Long, com.gitb.xml.export.Actor],
                             exportedEndpointParameterMap: Map[Long, com.gitb.xml.export.EndpointParameter],
                             exportedDomain: Option[com.gitb.xml.export.Domain],
                             actorEndpointMap: Map[Long, List[models.Endpoints]],
                             endpointParameterMap: Map[Long, List[models.Parameters]],
                             exportedDomainParameterMap: Map[Long, com.gitb.xml.export.DomainParameter],
                             exportedSpecificationGroupMap: Map[Long, com.gitb.xml.export.SpecificationGroup],
                             exportedSpecificationMap: Map[Long, com.gitb.xml.export.Specification]
                           )