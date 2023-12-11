package models.theme

import models.NamedFile

case class ThemeFiles(headerLogo: Option[NamedFile], footerLogo: Option[NamedFile], faviconFile: Option[NamedFile])
