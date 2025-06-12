package managers.export

import models.Parameters

case class DomainImportInfo(
                             targetActorEndpointMap: Map[Long, Map[String, models.Endpoints]],
                             targetEndpointParameterMap: Map[Long, Map[String, models.Parameters]],
                             targetActorToSpecificationMap: Map[Long, models.Specifications],
                             targetEndpointParameterIdMap: Map[Long, Parameters],
                             actorXmlIdToImportItemMap: Map[String, ImportItem]
                           )



