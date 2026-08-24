/*
 * Copyright (C) 2026 European Union
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

case class TestFlags(id: Long, name: String, description: Option[String], colour: String, publicName: Option[String], publicColour: Option[String], adminOnly: Boolean, displayOrder: Short, community: Long) {

  /** The name shown to organisation users - falls back to the internal name when no public override is set. */
  def effectiveName: String = publicName.getOrElse(name)

  /** The colour shown to organisation users - falls back to the internal colour when no public override is set. */
  def effectiveColour: String = publicColour.getOrElse(colour)

}
