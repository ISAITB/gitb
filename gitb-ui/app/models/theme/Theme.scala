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

package models.theme

case class Theme(
                  id: Long,
                  key: String,
                  description: Option[String],
                  active: Boolean,
                  custom: Boolean,
                  separatorTitleColor: String,
                  modalTitleColor: String,
                  tableTitleColor: String,
                  cardTitleColor: String,
                  pageTitleColor: String,
                  headingColor: String,
                  tabLinkColor: String,
                  footerTextColor: String,
                  headerBackgroundColor: String,
                  headerBorderColor: String,
                  headerSeparatorColor: String,
                  headerLogoPath: String,
                  footerBackgroundColor: String,
                  footerBorderColor: String,
                  footerLogoPath: String,
                  footerLogoDisplay: String,
                  faviconPath: String,
                  primaryButtonColor: String,
                  primaryButtonLabelColor: String,
                  primaryButtonHoverColor: String,
                  primaryButtonActiveColor: String,
                  secondaryButtonColor: String,
                  secondaryButtonLabelColor: String,
                  secondaryButtonHoverColor: String,
                  secondaryButtonActiveColor: String
                ) {

  def withImagePaths(headerLogoPath: String, footerLogoPath: String, faviconPath: String): Theme = {
    this.copy(headerLogoPath = headerLogoPath, footerLogoPath = footerLogoPath, faviconPath = faviconPath)
  }

  def withId(id: Long): Theme = {
    this.copy(id = id)
  }

}
