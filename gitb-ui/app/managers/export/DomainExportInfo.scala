package managers.export

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class DomainExportInfo(
                             latestSequenceId: Int,
                             exportedActorMap: mutable.Map[Long, com.gitb.xml.export.Actor],
                             exportedEndpointParameterMap: mutable.Map[Long, com.gitb.xml.export.EndpointParameter],
                             exportedDomain: Option[com.gitb.xml.export.Domain],
                             actorEndpointMap: mutable.Map[Long, ListBuffer[models.Endpoints]],
                             endpointParameterMap: mutable.Map[Long, ListBuffer[models.Parameters]],
                             exportedDomainParameterMap: mutable.Map[Long, com.gitb.xml.export.DomainParameter],
                             exportedSpecificationGroupMap: mutable.Map[Long, com.gitb.xml.export.SpecificationGroup],
                             exportedSpecificationMap: mutable.Map[Long, com.gitb.xml.export.Specification]
                           ) {}