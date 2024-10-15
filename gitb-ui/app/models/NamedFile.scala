package models

import java.io.File

case class NamedFile(file: File, name: String, identifier: Option[String] = None)
