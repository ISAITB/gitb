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

package models

import scala.collection.mutable.ListBuffer

object DomainSpecification {

  def forGroup(group: SpecificationGroups): DomainSpecification = {
    new DomainSpecification(
      group.id, group.shortname, group.fullname, group.description,
      false, true, group.domain, group.displayOrder, None, Some(ListBuffer())
    )
  }

  def forSpecification(specification: Specifications): DomainSpecification = {
    new DomainSpecification(
      specification.id, specification.shortname, specification.fullname, specification.description,
      specification.hidden, false, specification.domain, specification.displayOrder, specification.group, None
    )
  }

}

class DomainSpecification(val id: Long,
                          val sname: String,
                          val fname: String,
                          val description: Option[String],
                          var hidden: Boolean,
                          val group: Boolean,
                          val domain: Long,
                          val displayOrder: Short,
                          val groupId: Option[Long],
                          var options: Option[ListBuffer[DomainSpecification]]
                         ) {
}
