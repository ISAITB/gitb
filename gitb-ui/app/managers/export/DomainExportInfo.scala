package managers.export

import scala.collection.mutable

case class DomainExportInfo(latestSequenceId: Int, exportedActorMap: mutable.Map[Long, com.gitb.xml.export.Actor], exportedEndpointParameterMap: mutable.Map[Long, com.gitb.xml.export.EndpointParameter], exportedDomain: com.gitb.xml.export.Domain) {}