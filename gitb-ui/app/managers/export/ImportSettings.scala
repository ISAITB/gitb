package managers.export

import java.nio.file.Path

class ImportSettings {

  var encryptionKey: Option[String] = None
  var dataFilePath: Option[Path] = None
}
