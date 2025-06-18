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
    map += (ImportItemType.SystemResource -> mutable.Set[String]())
    map += (ImportItemType.Theme -> mutable.Set[String]())
    map += (ImportItemType.DefaultLandingPage -> mutable.Set[String]())
    map += (ImportItemType.DefaultLegalNotice -> mutable.Set[String]())
    map += (ImportItemType.DefaultErrorTemplate -> mutable.Set[String]())
    map += (ImportItemType.SystemAdministrator -> mutable.Set[String]())
    map += (ImportItemType.SystemConfiguration -> mutable.Set[String]())
    ExistingIds(map.toMap)
  }
}

case class ExistingIds (map: Map[ImportItemType, mutable.Set[String]])