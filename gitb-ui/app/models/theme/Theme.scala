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
