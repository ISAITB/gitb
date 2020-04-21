package managers.export

import models.Parameters

import scala.collection.mutable

case class DomainImportInfo(
                             targetActorEndpointMap: mutable.Map[Long, mutable.Map[String, models.Endpoints]],
                             targetEndpointParameterMap: mutable.Map[Long, mutable.Map[String, models.Parameters]],
                             targetActorToSpecificationMap: mutable.Map[Long, models.Specifications],
                             targetEndpointParameterIdMap: mutable.Map[Long, Parameters],
                             actorXmlIdToImportItemMap: mutable.Map[String, ImportItem]
                           ) {}



