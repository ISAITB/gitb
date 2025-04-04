package managers.`export`

object SandboxImportResult {

  def complete(): SandboxImportResult = {
    SandboxImportResult(processingComplete = true, None)
  }

  def incomplete(): SandboxImportResult = {
    SandboxImportResult(processingComplete = false, None)
  }

  def incomplete(message: String): SandboxImportResult = {
    SandboxImportResult(processingComplete = false, Some(message))
  }

}

case class SandboxImportResult(processingComplete: Boolean, errorMessage: Option[String])
