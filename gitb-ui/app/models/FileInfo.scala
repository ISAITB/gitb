package models

import java.io.File

class FileInfo (var key: String, var name: String, var contentType: Option[String], var file: File) {}
