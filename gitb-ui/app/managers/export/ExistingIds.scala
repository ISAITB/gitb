package managers.export

import models.Enums.ImportItemType
import models.Enums.ImportItemType.ImportItemType

import scala.collection.mutable

object ExistingIds {
  def init(): ExistingIds = {
    val map = mutable.Map[ImportItemType, mutable.Set[String]]()
    map += (ImportItemType.Domain -> mutable.Set[String]())
    map += (ImportItemType.DomainParameter -> mutable.Set[String]())
    map += (ImportItemType.Specification -> mutable.Set[String]())
    map += (ImportItemType.SpecificationGroup -> mutable.Set[String]())
    map += (ImportItemType.Actor -> mutable.Set[String]())
    map += (ImportItemType.TestSuite -> mutable.Set[String]())
    map += (ImportItemType.Endpoint -> mutable.Set[String]())
    map += (ImportItemType.EndpointParameter -> mutable.Set[String]())
    map += (ImportItemType.Community -> mutable.Set[String]())
    map += (ImportItemType.CustomLabel -> mutable.Set[String]())
    map += (ImportItemType.OrganisationProperty -> mutable.Set[String]())
    map += (ImportItemType.SystemProperty -> mutable.Set[String]())
    map += (ImportItemType.LandingPage -> mutable.Set[String]())
    map += (ImportItemType.LegalNotice -> mutable.Set[String]())
    map += (ImportItemType.ErrorTemplate -> mutable.Set[String]())
    map += (ImportItemType.Trigger -> mutable.Set[String]())
    map += (ImportItemType.CommunityResource -> mutable.Set[String]())
    map += (ImportItemType.Administrator -> mutable.Set[String]())
    map += (ImportItemType.Organisation -> mutable.Set[String]())
    map += (ImportItemType.OrganisationUser -> mutable.Set[String]())
    map += (ImportItemType.OrganisationPropertyValue -> mutable.Set[String]())
    map += (ImportItemType.System -> mutable.Set[String]())
    map += (ImportItemType.SystemPropertyValue -> mutable.Set[String]())
    map += (ImportItemType.Statement -> mutable.Set[String]())
    map += (ImportItemType.StatementConfiguration -> mutable.Set[String]())
    map += (ImportItemType.Theme -> mutable.Set[String]())
    ExistingIds(map.toMap)
  }
}

case class ExistingIds (map: Map[ImportItemType, mutable.Set[String]])