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
                  faviconPath: String
                ) {

  def withImagePaths(headerLogoPath: String, footerLogoPath: String, faviconPath: String): Theme = {
    Theme(this.id, this.key, this.description, this.active, this.custom, this.separatorTitleColor, this.modalTitleColor, this.tableTitleColor, this.cardTitleColor, this.pageTitleColor, this.headingColor, this.tabLinkColor,
      this.footerTextColor, this.headerBackgroundColor, this.headerBorderColor, this.headerSeparatorColor, headerLogoPath, this.footerBackgroundColor, this.footerBorderColor, footerLogoPath,
      this.footerLogoDisplay, faviconPath)
  }

  def withId(id: Long): Theme = {
    Theme(id, this.key, this.description, this.active, this.custom, this.separatorTitleColor, this.modalTitleColor, this.tableTitleColor, this.cardTitleColor, this.pageTitleColor, this.headingColor, this.tabLinkColor,
      this.footerTextColor, this.headerBackgroundColor, this.headerBorderColor, this.headerSeparatorColor, this.headerLogoPath, this.footerBackgroundColor, this.footerBorderColor, this.footerLogoPath,
      this.footerLogoDisplay, this.faviconPath)
  }

}
